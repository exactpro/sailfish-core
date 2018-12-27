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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.StringUtils;

public class JdbcUtils {

	public static void executeTestQuery(String driverClass, String url, String user, String pass, String testQuery) 
			throws ClassNotFoundException, SQLException {

		Connection conn = null;
		Statement stmt = null;

		try {

			Class.forName(driverClass);
			conn = DriverManager.getConnection(url, user, pass);

			stmt = conn.createStatement();

			stmt.execute(testQuery);

		} finally {
			
			if (stmt != null) {
				stmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}
		}
	}
	
	public static String buildConnectionUrl(String protocol, String subProtocol, String host, String port, String database, String query) {
        
        String hostFormat = StringUtils.isEmpty(port) ? "%s" : "%s:%s";
        
        String format;
        
        if(StringUtils.isNotEmpty(query)) {
            
            format = "%s:%s://" + hostFormat + "/%s?%s";
            
        } else {
            
            format = "%s:%s://" + hostFormat + "/%s";
            
        }
        
        if (StringUtils.isNotEmpty(port)) {
            return String.format(format, protocol, subProtocol, host, port, database, query);
        } else {
            return String.format(format, protocol, subProtocol, host, database, query);
        }
    }
}
