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
package com.exactpro.sf.testwebgui.servlets;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.FileWatchdog;
import org.apache.log4j.helpers.LogLog;

import com.exactpro.sf.center.impl.PluginLoader;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;

/**
 * PropertyConfigurator is able to stopWatch to clean up its threads
 */
public class CustomPropertyConfigurator extends PropertyConfigurator {

    private static CustomPropertyWatchdog pdog;

    public static void configureAndWatch(IWorkspaceDispatcher workspaceDispatcher) {
        pdog = new CustomPropertyWatchdog(workspaceDispatcher);
        pdog.start();
    }

    public static void stopWatch() {
        pdog.interrupt();
    }

    private static class CustomPropertyWatchdog extends Thread {

        private final IWorkspaceDispatcher workspaceDispatcher;
        /**
         * The delay to observe between every check. By default set @
         * {@link FileWatchdog#DEFAULT_DELAY}.
         */
        protected volatile long delay = 1000;//FileWatchdog.DEFAULT_DELAY;

        private long lastModif = 0;
        private boolean warnedAlready = false;
        private boolean interrupted = false;

        public CustomPropertyWatchdog(IWorkspaceDispatcher workspaceDispatcher) {
            super("CustomPropertyWatchdog");
            setDaemon(true);
            this.workspaceDispatcher = workspaceDispatcher;
            checkAndConfigure();
        }

        @Override
        public void run() {
            while (!interrupted) {
                try {
                    Thread.sleep(delay);
                    checkAndConfigure();
                } catch (InterruptedException e) {
                    interrupted = true;
                    return;
                }
            }
        }
        
        protected void checkAndConfigure() {
            try {
                File file = workspaceDispatcher.getFile(FolderType.CFG, PluginLoader.LOG4J_PROPERTIES_FILE_NAME);
                long l = file.lastModified();
                if (l > lastModif) {
                    lastModif = l;
                    doOnChange(file);
                    warnedAlready = false;
                }
            } catch (SecurityException | WorkspaceSecurityException e) {
                LogLog.warn("Was not allowed to read check file existance, file:[" +
                        PluginLoader.LOG4J_PROPERTIES_FILE_NAME + "].", e);
                interrupted = true;
            } catch (FileNotFoundException e) {
                if (!warnedAlready) {
                    LogLog.debug("[" + PluginLoader.LOG4J_PROPERTIES_FILE_NAME + "] does not exist.", e);
                    warnedAlready = true;
                }
            }
        }

        protected void doOnChange(File file) {
            new PropertyConfigurator().doConfigure(file.getAbsolutePath(),
                    LogManager.getLoggerRepository());

        }
    }
}
