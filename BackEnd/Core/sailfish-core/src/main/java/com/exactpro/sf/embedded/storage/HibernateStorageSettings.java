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
package com.exactpro.sf.embedded.storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.storage.IMapableSettings;

public class HibernateStorageSettings implements IMapableSettings, Serializable {
    
    private static final long serialVersionUID = -5012373122771249391L;
    
    protected String protocol = "jdbc";
    
    protected String subProtocol = "postgresql";
    
    protected String host = "localhost";
    
    protected int port = 5432;
    
    protected String dbName = "sfstatistics";
    
    protected String connectionOptionsQuery = "useUnicode=true&characterEncoding=UTF-8&useFastDateParsing=false&socketTimeout=15000";
    
    protected String username = "sailfish";
    
    protected String password;
    
    protected String dialect = "org.hibernate.dialect.PostgreSQLDialect";
    
    protected String driverClass = "org.postgresql.Driver";
    
    protected String dbms = "PostgreSQL";
    
    private final String storagePrefix;
    
    public HibernateStorageSettings(String storagePrefix) {
        this.storagePrefix = storagePrefix;
    }

    public HibernateStorageSettings(HibernateStorageSettings toClone) {
        this.storagePrefix = toClone.storagePrefix;
        this.protocol = toClone.getProtocol();
        this.subProtocol = toClone.getSubProtocol();
        this.host = toClone.getHost();
        this.port = toClone.getPort();
        this.dbName = toClone.getDbName();
        this.connectionOptionsQuery = toClone.getConnectionOptionsQuery();
        this.username = toClone.getUsername();
        this.password = toClone.getPassword();
        this.dialect = toClone.getDialect();
        this.driverClass = toClone.getDriverClass();
        this.dbms = toClone.getDbms();
        
    }

    @Override
    public String settingsName() {
        throw new UnsupportedOperationException(String.format("%s doesn't have own settings name", HibernateStorageSettings.class.getSimpleName()));
    }

    public void fillFromMap(Map<String, String> options) throws Exception {

        for(Entry<String, String> entry : options.entrySet()) {
            
            if(entry.getKey().startsWith(storagePrefix)) {
                
                BeanUtils.setProperty(this, entry.getKey().replace(storagePrefix, ""), entry.getValue());
                
            }
            
        }
        
    }
    
    public Map<String, String> toMap() throws Exception {
        
        @SuppressWarnings("unchecked")
        Map<String, String> description = BeanUtils.describe(this);
        
        Map<String, String> result = new HashMap<String, String>();

        for(Entry<String, String> entry : description.entrySet()) {

            if(!"class".equals(entry.getKey())) {
            
                result.put(storagePrefix + entry.getKey(), entry.getValue());
            
            }
            
        }
        
        return result;
        
    }
    
    public String buildConnectionUrl() {
        StringBuilder builder = new StringBuilder();

        builder.append(protocol);
        builder.append(':');
        builder.append(subProtocol);
        builder.append("://");
        builder.append(host);
        builder.append(':');
        builder.append(port);
        builder.append('/');
        builder.append(dbName);

        if(StringUtils.isNotEmpty(connectionOptionsQuery)) {
            builder.append('?');
            builder.append(connectionOptionsQuery);
        }

        return builder.toString();
    }
    
    
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSubProtocol() {
        return subProtocol;
    }

    public void setSubProtocol(String subProtocol) {
        this.subProtocol = subProtocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getConnectionOptionsQuery() {
        return connectionOptionsQuery;
    }

    public void setConnectionOptionsQuery(String connectionOptionsQuery) {
        this.connectionOptionsQuery = connectionOptionsQuery;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getDbms() {
        return dbms;
    }

    public void setDbms(String dbms) {
        this.dbms = dbms;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("HibernateStorageSettings", protocol)
                .append("subProtocol", subProtocol)
                .append("host", host)
                .append("port", port)
                .append("dbName", dbName)
                .append("connectionOptionsQuery", connectionOptionsQuery)
                .append("username", username)
                .append("password", "***")
                .append("dialect", dialect)
                .append("driverClass", driverClass).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(connectionOptionsQuery)
                .append(dbName)
                .append(dialect)
                .append(driverClass)
                .append(password)
                .append(protocol)
                .append(subProtocol)
                .append(host)
                .append(port)
                .append(username).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof HibernateStorageSettings)) {
            return false;
        }

        HibernateStorageSettings that = (HibernateStorageSettings)o;
        return new EqualsBuilder()
                .append(dbms, that.dbms)
                .append(connectionOptionsQuery, that.connectionOptionsQuery)
                .append(dbName, that.dbName)
                .append(dialect, that.dialect)
                .append(driverClass, that.driverClass)
                .append(password, that.password)
                .append(protocol, that.protocol)
                .append(subProtocol, that.subProtocol)
                .append(host, that.host)
                .append(port, that.port)
                .append(username, that.username).isEquals();
    }
    
}
