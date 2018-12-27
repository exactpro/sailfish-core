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

package com.exactpro.sf.embedded.statistics;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.exactpro.sf.embedded.statistics.configuration.DbmsType;
import com.exactpro.sf.embedded.statistics.configuration.StatisticsServiceSettings;
import com.exactpro.sf.embedded.statistics.storage.OlderSchemaException;
import com.exactpro.sf.embedded.storage.HibernateStorageSettings;

public class StatisticsMigration {

    private final static String CHECK_MODE = "check";
    private final static String MIGRATE_MODE = "migrate";
    private final static int NEED_MIGRATION_CODE = 7;

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.err.println(String.format("Wrong arguments count. Should be %d but was %d", 2, args.length));
                System.exit(1);
            }

            String mode = args[0];
            String configurationFile = args[1];

            if (!mode.equals(CHECK_MODE) && !mode.equals(MIGRATE_MODE)) {
                throw new IllegalArgumentException("Unknown mode: " + mode);
            }

            Properties properties = new Properties();
            try (InputStream in = new FileInputStream(configurationFile)) {
                properties.load(in);
            }

            Map<String, String> settingsMap = new HashMap<>();
            for (Map.Entry<Object, Object> prop : properties.entrySet()) {
                settingsMap.put(prop.getKey().toString(), prop.getValue().toString());
            }
            StatisticsServiceSettings settings = new StatisticsServiceSettings();
            settings.fillFromMap(settingsMap);

            HibernateStorageSettings hibSettings = settings.getStorageSettings();
            DbmsType dbmsType = DbmsType.getTypeByName(hibSettings.getDbms());
            dbmsType.setDbmsSettings(hibSettings);

            StatisticsFlywayWrapper statisticsFlywayWrapper = new StatisticsFlywayWrapper();
            try {
                statisticsFlywayWrapper.init(settings.getStorageSettings());
            } catch (OlderSchemaException ex) {
                System.out.println(ex.getMessage());
            }

            switch (mode) {
                case CHECK_MODE:
                    if (statisticsFlywayWrapper.isMigrationRequired()) {
                        System.exit(NEED_MIGRATION_CODE);
                    }
                    break;
                case MIGRATE_MODE:
                    if (!statisticsFlywayWrapper.isMigrationRequired()) {
                        throw new IllegalStateException("Migrate isn't required");
                    }
                    statisticsFlywayWrapper.migrate();
                    break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }
}
