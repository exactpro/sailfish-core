/*******************************************************************************
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

package com.exactpro.sf.storage.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.IMapableSettings;
import com.exactpro.sf.storage.IMappableSettingsSerializer;

public class PropertiesSettingsReaderSerializer implements IMappableSettingsSerializer {
    private final static Logger logger = LoggerFactory.getLogger(PropertiesSettingsReaderSerializer.class);
    private final static String SETTINGS_EXTENSION = ".properties";

    public void readMappableSettings(IMapableSettings settings, IWorkspaceDispatcher wd, FolderType folderType, String... relativePath) throws Exception {
        String fileName = settings.settingsName() + SETTINGS_EXTENSION;

        File settingsFile = wd.getFile(folderType, ArrayUtils.addAll(relativePath, fileName));
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(settingsFile)) {
            properties.load(in);
        }
        Map<String, String> settingsMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            settingsMap.put(entry.getKey().toString(), entry.getValue().toString());
        }
        settings.fillFromMap(settingsMap);
        logger.debug("Loaded {} settings: {}", settings.settingsName(), settings);
    }

    public void writeMappableSettings(IMapableSettings settings, IWorkspaceDispatcher wd, FolderType folderType, String... relativePath) throws Exception {
        String fileName = settings.settingsName() + SETTINGS_EXTENSION;
        File settingsFile = wd.createFile(folderType, true, ArrayUtils.addAll(relativePath, fileName));
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : settings.toMap().entrySet()) {
            if (entry.getValue() != null) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        logger.debug("Save {} settings: {}", settings.settingsName(), settings);
        try (Writer writer = new FileWriter(settingsFile)) {
            properties.store(writer, null);
        }
    }
}
