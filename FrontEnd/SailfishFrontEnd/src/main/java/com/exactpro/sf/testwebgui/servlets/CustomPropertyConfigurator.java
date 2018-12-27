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

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.FileWatchdog;

/**
 * PropertyConfigurator is able to stopWatch to clean up its threads
 */
public class CustomPropertyConfigurator extends PropertyConfigurator {

    private static CustomPropertyWatchdog pdog;

    public static void configureAndWatch(String configFileName) {
        pdog = new CustomPropertyWatchdog(configFileName);
        pdog.setDelay(FileWatchdog.DEFAULT_DELAY);
        pdog.start();
    }

    public static void stopWatch() {
        pdog.interrupt();
    }


    private static class CustomPropertyWatchdog extends FileWatchdog {

        private volatile boolean customInterrupted = false;

        public CustomPropertyWatchdog(String configFileName) {
            super(configFileName);
        }

        @Override
        protected void doOnChange() {
            new PropertyConfigurator().doConfigure(filename,
                    LogManager.getLoggerRepository());

        }

        @Override
        public void run() {
            while(!customInterrupted) {
                try {
                    Thread.sleep(delay);
                } catch(InterruptedException e) {
                    customInterrupted = true;
                    return;
                }
                checkAndConfigure();
            }
        }
    }
}
