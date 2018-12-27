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
package com.exactpro.sf.testwebgui.messages;

import java.util.StringTokenizer;

public class MessageParser {
	
	public static String parseFields(String message) {
		
		StringBuilder html = new StringBuilder();
		html.append("<table>");
		
		StringTokenizer st = new StringTokenizer(message, ";");
		while (st.hasMoreTokens()) {
			
			String field = st.nextToken().trim();
			StringTokenizer ft = new StringTokenizer(field, "=");
			
			String fieldName = new String();
			String fieldValue = new String();
			
			int num = 0;
			while (ft.hasMoreTokens()) {
				
				num++;
				String value = ft.nextToken().trim();
				
				switch (num) {
				case 1:
					fieldName = value;
					break;
				case 2:
					fieldValue = value;
					break;
				default:
					break;
				}
				
			}
			
			html.append("<tr>");
			html.append("<td width='30%'>");
			html.append(fieldName);
			html.append("</td>");
			html.append("<td width='70%'>");
			html.append(fieldValue);
			html.append("</td>");
			html.append("</tr>");
			
		}
		
		html.append("</table>");
		return html.toString();
	}

}
