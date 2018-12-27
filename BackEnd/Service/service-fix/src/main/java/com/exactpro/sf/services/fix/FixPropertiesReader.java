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
package com.exactpro.sf.services.fix;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.quickfixj.CharsetSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.services.IServiceContext;

public class FixPropertiesReader {
    public static final Logger logger = LoggerFactory.getLogger(FixPropertiesReader.class);

    public static final String PROPERTIES_FILE_NAME = "fixService.properties";

    public static final AtomicBoolean CONFIGURE = new AtomicBoolean(false);

    private FixPropertiesReader() {
    }

    public static void loadAndSetCharset(IServiceContext serviceContext) {
        if (!CONFIGURE.getAndSet(true)) {
            Properties prop = new Properties();
            InputStream propertiesIS = null;
            try {
                File propertiesFile = serviceContext.getWorkspaceDispatcher().getFile(FolderType.CFG, PROPERTIES_FILE_NAME);
                if (propertiesFile.exists()) {
                    propertiesIS = new FileInputStream(propertiesFile);
                    prop.load(propertiesIS);
                    String charset = prop.getProperty("charset");
                    CharsetSupport.setCharset(charset);
                    logger.info("Charset {} has been set", charset);
                } else {
                    logger.info("File {} had not been found, default charset used", PROPERTIES_FILE_NAME);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(propertiesIS);
            }
        }
    }
}
