/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.aml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.GeneratedScript;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.util.AML3PluginTest;

/**
 * @author nikita.smirnov
 *
 */
public abstract class TestAML3Base extends AML3PluginTest {

    private final static Logger logger = LoggerFactory.getLogger(TestAML3Base.class);

    protected abstract AMLSettings createSettings();

    protected AML executeTest(String path) throws AMLException, IOException, InterruptedException
    {
    	// copy to 'report' folder
    	TestWorkspaceLayout layout = new TestWorkspaceLayout("test");
    	File pathFile = new File(path);
    	File srcFile = new File(BASE_DIR, path);
    	File dstFile = new File(layout.getPath(new File("."), FolderType.REPORT), pathFile.getParent());
    	FileUtils.copyFileToDirectory(srcFile, dstFile);

    	// work
        logger.debug("start");
        AMLSettings settings = createSettings();
        settings.setMatrixPath(path);

        AML aml = new AML(settings,
                          context.getWorkspaceDispatcher(),
                          context.getAdapterManager(),
                          context.getEnvironmentManager(),
                          context.getDictionaryManager(),
                          context.getStaticServiceManager(),
                          context.getLanguageManager(),
                          context.getActionManager(),
                          context.getUtilityManager(),
                          context.getCompilerClassPath());

        try {
            ScriptContext scriptContext = new ScriptContext(context, null, null, null, System.getProperty("user.name"), 0, ServiceName.DEFAULT_ENVIRONMENT);
            GeneratedScript script = aml.run(scriptContext, "UTF-8");
            AlertCollector collector = aml.getAlertCollector();

            if(collector.getCount(AlertType.ERROR) > 0) {
                throw new AMLException("Got errors during code generation", collector);
            }

            logger.info("Script created");

            String compilerClassPath = context.getCompilerClassPath();
            File srcFolder = new File(BIN_FOLDER_PATH);
            AML.compileScript(script, srcFolder, null, compilerClassPath);
            logger.info("run finished");
            printAlerts(aml.getAlertCollector());
            return aml;
        } catch (AMLException e) {
            StringBuilder builder = new StringBuilder();
            for (Alert error : e.getAlertCollector().getAlerts(AlertType.ERROR)) {
                builder.append(error).append(System.lineSeparator());
            }
            printAlerts(e.getAlertCollector());
            throw e;
        }
    }

    protected Alert remove(List<Alert> source, Alert Alert) {
        for (int i = 0; i < source.size(); i++) {
            if ((source.get(i).getLine() == Alert.getLine())
                    && (source.get(i).getType() == Alert.getType())
                    && (source.get(i).getReference() == null ? Alert.getReference() == null : source.get(i).getReference().equals(Alert.getReference()))
                    && (source.get(i).getColumn() == null ? Alert.getColumn() == null : source.get(i).getColumn().equals(Alert.getColumn()))
                    && (source.get(i).getMessage() == null ? Alert.getMessage() == null : source.get(i).getMessage().startsWith(Alert.getMessage()))) {
                source.remove(i);
                return null;
            }
        }
        return Alert;
    }

    private void printAlerts(AlertCollector alertCollector) {
        if (alertCollector != null && alertCollector.getCount(AlertType.ERROR) != 0) {
            for (Alert error : alertCollector.getAlerts(AlertType.ERROR)) {
                System.err.println(error);
            }
        }
    }
}
