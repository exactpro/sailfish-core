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
package com.exactpro.sf.bigbutton.execution;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.configuration.StatisticsServiceSettings;
import com.exactpro.sf.testwebgui.restapi.xml.XmlStatisticsDBSettings;

public class RegressionRunnerUtils {

    private final static Logger logger = LoggerFactory.getLogger(RegressionRunnerUtils.class);

    private RegressionRunnerUtils() {

    }

        public static int calcPercent(long actual, long total) {

        if (total == 0 && actual != 0) {
            return 100;
        }

        int result = (int) ((actual * 100.0f) / total);

        if (result > 100) {
            return 100;
        }

        return result;

    }

    public static String createErrorText(Throwable t) {

        StringBuilder sb = new StringBuilder();

        Throwable[] exceptions = ExceptionUtils.getThrowables(t);

        for (Throwable exception : exceptions) {

            sb.append(exception.getMessage()).append(". ");

        }

        return sb.toString();

    }

    public static String getStatisticsDBSettings() {

        StatisticsService service = SFLocalContext.getDefault().getStatisticsService();
        StatisticsServiceSettings settings = service.getSettings();

        XmlStatisticsDBSettings dbSettings = new XmlStatisticsDBSettings();
        dbSettings.setStatisticsServiceEnabled(settings.isServiceEnabled());
        dbSettings.setDbms(settings.getStorageSettings().getDbms());
        dbSettings.setHost(settings.getStorageSettings().getHost());
        dbSettings.setPort(settings.getStorageSettings().getPort());
        dbSettings.setDbName(settings.getStorageSettings().getDbName());
        dbSettings.setConnectionOptionsQuery(settings.getStorageSettings().getConnectionOptionsQuery());
        dbSettings.setUsername(settings.getStorageSettings().getUsername());
        dbSettings.setPassword(settings.getStorageSettings().getPassword());
        final StringBuilder xml = new StringBuilder();
        try {
            JAXBContext.newInstance(XmlStatisticsDBSettings.class).createMarshaller().marshal(dbSettings, new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    xml.append((char) b);
                }
            });
        } catch (JAXBException e) {
            logger.error("{}", e);
        }

        return xml.toString();
    }
}
