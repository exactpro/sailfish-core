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

import com.exactpro.sf.embedded.statistics.configuration.DbmsType;
import com.exactpro.sf.embedded.statistics.storage.NewerSchemaException;
import com.exactpro.sf.embedded.statistics.storage.OlderSchemaException;
import com.exactpro.sf.embedded.storage.HibernateStorageSettings;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StatisticsFlywayWrapper {
    private final static String PSQL_SCRIPT_LOCATION = "com/exactpro/sf/statistics/storage/pg/migration";
    private final static String MYSQL_SCRIPT_LOCATION = "com/exactpro/sf/statistics/storage/mysql/migration";

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private Flyway flyway;

    private MigrationInfo currentDbVersionInfo;

    private MigrationInfo[] pendingMigrationsInfo;

    private boolean migrationRequired;

    private boolean sfUpdateRequired;

    public void init(HibernateStorageSettings settings) {
        try {
            readWriteLock.writeLock().lock();
            migrationRequired = false;

            sfUpdateRequired = false;

            flyway = new Flyway();

            flyway.getPlaceholders().put("db_name", settings.getDbName());

            flyway.setDataSource(settings.buildConnectionUrl(), settings.getUsername(), settings.getPassword());

            if (settings.getDbms().equals(DbmsType.PostgreSQL.getValue())) {
                flyway.setLocations(PSQL_SCRIPT_LOCATION);
            }

            if (settings.getDbms().equals(DbmsType.MySql.getValue())) {
                flyway.setLocations(MYSQL_SCRIPT_LOCATION);
            }

            flyway.setBaselineOnMigrate(false);

            // Get info about migrations

            MigrationInfoService info = flyway.info();

            currentDbVersionInfo = info.current();

            pendingMigrationsInfo = info.pending();

            MigrationInfo[] all = info.all();

            // Checks

            if (currentDbVersionInfo == null) {
                migrationRequired = true;

                throw new OlderSchemaException("DB initialization is required");

            }

            if (pendingMigrationsInfo.length != 0) {
                migrationRequired = true;

                throw new OlderSchemaException("Migration to version "
                        + pendingMigrationsInfo[pendingMigrationsInfo.length - 1].getVersion().getVersion()
                        + " is required");

            }

            if (all.length != 0) {
                MigrationInfo lastKnown = all[all.length - 1];

                if (lastKnown.getState().equals(MigrationState.FUTURE_SUCCESS)) {

                    sfUpdateRequired = true;

                    throw new NewerSchemaException("DB schema has newer version " + lastKnown.getVersion().getVersion()
                            + ". Upgrade this Sailfish instance to use it.");
                }
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void migrate() {
        try {
            readWriteLock.writeLock().lock();
            flyway.migrate();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public MigrationInfo getCurrentDbVersionInfo() {
        try {
            readWriteLock.readLock().lock();
            return currentDbVersionInfo;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public MigrationInfo[] getPendingMigrationsInfo() {
        try {
            readWriteLock.readLock().lock();
            return pendingMigrationsInfo;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public boolean isMigrationRequired() {
        try {
            readWriteLock.readLock().lock();
            return migrationRequired;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public boolean isSfUpdateRequired() {
        try {
            readWriteLock.readLock().lock();
            return sfUpdateRequired;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
}
