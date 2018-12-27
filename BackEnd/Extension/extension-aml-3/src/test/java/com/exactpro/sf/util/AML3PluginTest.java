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
package com.exactpro.sf.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.SFContextSettings;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcherBuilder;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.ScriptRunException;

public class AML3PluginTest {

	private static final Logger logger = LoggerFactory.getLogger(AML3PluginTest.class);

	protected static final String BIN_FOLDER_PATH = "build/test-results";

	protected static final File BASE_DIR = new File((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir"));
	protected static final File SAILFISH_DICTIONARY_PATH = new File(BASE_DIR, "src/main/plugin/cfg/dictionaries/");
	protected static final String WORKSPACE_FOLDER = BIN_FOLDER_PATH + File.separator + "plugin" + File.separator;
	protected static final String LOG_PROPERTIES_PATH = BASE_DIR + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "log4j.properties";

	protected static ISFContext context;

	private static boolean isLoggingAlreadyConfigured = false;
	private static boolean isAlreadyConfigured = false;

	private static final String DEFAULT_CFG_FILENAME = "sf.cfg.xml";

	public static class TestWorkspaceLayout extends DefaultWorkspaceLayout {

        private final String rootFolder;

        public TestWorkspaceLayout(String rootFolder) {
            this.rootFolder = rootFolder;
        }

        @Override
        public String getPath(File root, FolderType folderType) {
            switch (folderType) {
            case ROOT:
                return new StringBuilder(BASE_DIR.getAbsolutePath())
                .append(File.separator).append("src")
                .append(File.separator).append(this.rootFolder)
                .append(File.separator).append("plugin")
                .toString();
            case CFG:
                return new StringBuilder(BASE_DIR.getAbsolutePath())
                    .append(File.separator).append("src")
                    .append(File.separator).append(this.rootFolder)
                    .append(File.separator).append("plugin")
                    .append(File.separator).append("cfg")
                    .toString();
            case MATRIX:
                return BASE_DIR.getAbsolutePath();
            case CSV:
                return WORKSPACE_FOLDER + "csv";
            case LOGS:
                return WORKSPACE_FOLDER + "logs";
            case REPORT:
                return WORKSPACE_FOLDER + "report";
            case TEST_LIBRARY:
    			return WORKSPACE_FOLDER + "test_library";
            default:
                return super.getPath(root, folderType);
            }
        }
    }

	@BeforeClass
	public static void initTestToolsProject() throws Throwable {
		synchronized (logger) {
			try {
				if (!isLoggingAlreadyConfigured) {
					PropertyConfigurator.configure(LOG_PROPERTIES_PATH);
				}
			} finally {
				isLoggingAlreadyConfigured = true;
			}
			try {
				if (!isAlreadyConfigured) {
                    IWorkspaceDispatcher wd = new DefaultWorkspaceDispatcherBuilder()
                            .addWorkspaceLayer(new File("src/main"), new TestWorkspaceLayout("main"))
                        	.addWorkspaceLayer(new File("src/gen"), new TestWorkspaceLayout("gen"))
                        	.addWorkspaceLayer(new File("src/test"), new TestWorkspaceLayout("test"))
    						.addWorkspaceLayer(Files.createTempDirectory("sf-tests").toFile(), DefaultWorkspaceLayout.getInstance())
                        	.build();

					SFContextSettings settings = new SFContextSettings();
                    settings.setConfig(loadDefaultEnvironment(wd));
                    settings.setCompilerClassPath(System.getProperty("java.class.path"));

					context = SFLocalContext.createContext(wd, settings);
				}
			} catch (Throwable e) {
				System.err.println(e);
				logger.error(e.getMessage(), e);
				throw e;
			} finally {
				isAlreadyConfigured = true;
			}
		}
	}

	protected static HierarchicalConfiguration loadDefaultEnvironment(IWorkspaceDispatcher wd) {
		try {
			File file = wd.getFile(FolderType.CFG, DEFAULT_CFG_FILENAME);
			return new XMLConfiguration(file);
		} catch (ConfigurationException e) {
			throw new ScriptRunException("Exception during configuration loading", e);
		} catch (Throwable e) {
			logger.error("Exception during environment initialization", e);
			throw new ScriptRunException("Exception during environment initialization", e);
		}
	}

	protected IDictionaryStructure loadMessageDictionary(InputStream inputStream) throws IOException {
		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
		return loader.load(inputStream);
	}
}
