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
package com.exactpro.sf.embedded.statistics.configuration;

import com.exactpro.sf.embedded.storage.HibernateStorageSettings;

public enum DbmsType {
	
	MySql("MySql") {
		@Override
		public void setDbmsSettings(HibernateStorageSettings hibSettings) {
			hibSettings.setSubProtocol("mariadb");
			hibSettings.setDriverClass("org.mariadb.jdbc.Driver");
			hibSettings.setDialect("org.hibernate.dialect.MySQLDialect");
		}
	},
	PostgreSQL("PostgreSQL") {
		@Override
		public void setDbmsSettings(HibernateStorageSettings hibSettings) {
			hibSettings.setSubProtocol("postgresql");
			hibSettings.setDriverClass("org.postgresql.Driver");
			hibSettings.setDialect("org.hibernate.dialect.PostgreSQLDialect");
		}
	};
	
	private final String value;
	
	private DbmsType(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public abstract void setDbmsSettings(HibernateStorageSettings hibSettings);

	public static DbmsType getTypeByName(String name) {
		for (DbmsType type : values()) {
			if (type.value.equals(name)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown DBMS: " + name);
	}
}
