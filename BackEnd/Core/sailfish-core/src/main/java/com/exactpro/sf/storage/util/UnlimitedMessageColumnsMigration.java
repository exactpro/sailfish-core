/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class UnlimitedMessageColumnsMigration {
    private static final Logger logger = LoggerFactory.getLogger(UnlimitedMessageColumnsMigration.class);

    private static final String MYSQL_CHECK_MIGRATION_QUERY = "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE table_schema = ? AND table_name = 'MESSAGES' AND COLUMN_NAME = 'RAW_MSG';";
    private static final String MYSQL_MIGRATION_QUERY = "ALTER TABLE MESSAGES MODIFY RAW_MSG longblob;";
    private static final String POSTGRES_CHECK_MIGRATION_QUERY = "select data_type from information_schema.columns " +
            "where table_name = 'messages' and column_name = 'json_msg';";
    private static final String POSTGRES_MIGRATION_QUERY = "ALTER TABLE messages " +
            "ALTER COLUMN human_msg SET DATA TYPE text, " +
            "ALTER COLUMN json_msg SET DATA TYPE text;";

    private final Session session;
    private final String databaseName;
    private final NativeDbQueries nativeDbQueries;

    public UnlimitedMessageColumnsMigration(Session session, Configuration configuration) throws URISyntaxException {
        this.session = session;
        String dialect = configuration.getProperty(AvailableSettings.DIALECT);
        this.nativeDbQueries = NativeDbQueries.fromDialect(dialect);
        String url = configuration.getProperty(AvailableSettings.URL);
        URI full = new URI(url);
        URI uri = new URI(full.getSchemeSpecificPart());
        this.databaseName = uri.getPath().replace("/", "");
    }

    public void migrate() throws Exception {
        if (nativeDbQueries == NativeDbQueries.NOT_SUPPORTED_DB) {
            logger.warn("Could not migrate database to set unlimited raw/json/human " +
                    "message columns. Reason: migration supported for mysql and postgres only");
            return;
        }
        if (nativeDbQueries.isMigrationNeeded(session, databaseName)) {
            Transaction transaction = session.beginTransaction();
            try(AutoCloseable closeable = transaction::commit) {
                NativeQuery<?> migrateSqlQuery = session.createSQLQuery(nativeDbQueries.getMigrationQuery());
                migrateSqlQuery.executeUpdate();
            } catch (Exception e) {
                transaction.rollback();
                throw e;
            }
        }
    }

    private enum NativeDbQueries {
        MYSQL("mysql", MYSQL_MIGRATION_QUERY) {
            @Override
            public boolean isMigrationNeeded(Session session, String dbName) {
                NativeQuery<?> conditionalSqlQuery = session.createSQLQuery(MYSQL_CHECK_MIGRATION_QUERY);
                conditionalSqlQuery.setParameter(0, dbName);
                List<?> conditionalSqlQueryResult = conditionalSqlQuery.list();
                if (CollectionUtils.isEmpty(conditionalSqlQueryResult)) {
                    return false;
                }
                return !StringUtils.equalsIgnoreCase("longblob", (String)conditionalSqlQueryResult.get(0));
            }
        },
        POSTGRES("postgres", POSTGRES_MIGRATION_QUERY) {
            @Override
            public boolean isMigrationNeeded(Session session, String dbName) {
                NativeQuery<?> conditionalSqlQuery = session.createSQLQuery(POSTGRES_CHECK_MIGRATION_QUERY);
                List<?> conditionalSqlQueryResult = conditionalSqlQuery.list();
                if (CollectionUtils.isEmpty(conditionalSqlQueryResult)) {
                    return false;
                }
                return !StringUtils.equalsIgnoreCase("text", (String)conditionalSqlQueryResult.get(0));
            }
        },
        NOT_SUPPORTED_DB() {
            @Override
            public boolean isMigrationNeeded(Session session, String dbName) {
                return false;
            }
        },;

        private final String type;
        private final String migrationQuery;

        NativeDbQueries() {
            this(null, null);
        }

        NativeDbQueries(String type, String migrationQuery) {
            this.type = type;
            this.migrationQuery = migrationQuery;
        }

        public abstract boolean isMigrationNeeded(Session session, String dbName);

        public static NativeDbQueries fromDialect(String dialect) {
            for (NativeDbQueries value : values()) {
                if (StringUtils.containsIgnoreCase(dialect, value.type)) {
                    return value;
                }
            }
            return NOT_SUPPORTED_DB;
        }

        public String getType() {
            return type;
        }

        public String getMigrationQuery() {
            return migrationQuery;
        }
    }
}
