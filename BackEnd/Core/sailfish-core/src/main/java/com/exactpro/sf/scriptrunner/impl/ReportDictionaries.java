/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf.scriptrunner.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.services.IServiceSettings;

public class ReportDictionaries extends DefaultScriptReport {

    public static final String DICTIONARIES_FOLDER = "dictionaries";
    private static final Logger logger = LoggerFactory.getLogger(ReportDictionaries.class);

    private final String dictionariesFolder;

    public ReportDictionaries(String reportFolder){
        dictionariesFolder = Paths.get(reportFolder, DICTIONARIES_FOLDER).toString();
    }

    @Override
    public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName, String userName) {

        File dictionariesFolderFile;
        try {
            dictionariesFolderFile = scriptContext.getWorkspaceDispatcher().createFolder(FolderType.REPORT, dictionariesFolder);
        } catch (WorkspaceStructureException e) {
            throw new EPSCommonException("Can`t create dictionaries folder", e);
        }

        Set<SailfishURI> dictionariesNames = new HashSet<>();
        List<String> matrixServicesNames = scriptContext.getServiceList();
        IConnectionManager connectionManager = scriptContext.getEnvironmentManager().getConnectionManager();

        for (String serviceName : matrixServicesNames) {
            IServiceSettings serviceSettings = connectionManager.getServiceSettings(ServiceName.parse(serviceName));
            SailfishURI dictionaryInfo = serviceSettings.getDictionaryName();
            if (dictionaryInfo == null || !dictionariesNames.add(dictionaryInfo)) {
                continue;
            }

            String dictionaryPath = scriptContext.getDictionaryManager().getDictionaryLocations().get(dictionaryInfo);

            if (dictionaryPath == null) {
                logger.warn(String.format("Can`t find dictionary [%s] file", dictionaryInfo.toString()));
                continue;
            }


            try {
                File srcDictionaryFile = scriptContext.getWorkspaceDispatcher().getFile(FolderType.ROOT, dictionaryPath);
                File targetDictionaryDirectory = dictionariesFolderFile;

                if (StringUtils.isNotEmpty(dictionaryInfo.getPluginAlias())) {
                    targetDictionaryDirectory = scriptContext.getWorkspaceDispatcher().createFolder(FolderType.REPORT, dictionariesFolder, dictionaryInfo.getPluginAlias());
                }

                FileUtils.copyFileToDirectory(srcDictionaryFile, targetDictionaryDirectory);

            } catch (FileNotFoundException e) {
                throw new EPSCommonException(String.format("Can`t get dictionary [%s] file", dictionaryInfo.toString()), e);
            } catch (WorkspaceStructureException e) {
                throw new EPSCommonException("Can`t create dictionaries plugin folder", e);
            } catch (IOException e) {
                throw new EPSCommonException("Can`t copy dictionary file to target folder", e);
            }

        }
    }
}
