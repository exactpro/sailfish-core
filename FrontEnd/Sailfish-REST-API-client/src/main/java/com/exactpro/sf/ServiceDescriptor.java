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
package com.exactpro.sf;

public class ServiceDescriptor {
	
	private String name;
	private String className;
	private String settingsClassName;
	
	public ServiceDescriptor(String name, String className, String settingsClassName) {
		this.name = name;
		this.className = className;
		this.settingsClassName = settingsClassName;
	}
	
	public String getName() {
		return name;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getSettingsClassName() {
		return settingsClassName;
	}
	
	public String getSettingsTagName() {
		String cName = settingsClassName.substring(settingsClassName.lastIndexOf('.') + 1);
		String lName = cName.toLowerCase();
		
		// Homebrew PascalCase to camelCase conversion routine
		// Please don't blame me
		if (cName.charAt(1) == lName.charAt(1)) {
			// Second letter is in lower case (e.g. AwesomeType)
			// Just lower the first one
			return lName.substring(0, 1) + cName.substring(1);
		}
		else {
			// Second letter is not in lower case (e.g. TCPIPServer)
			// Lower anything before first border upper case letter or digit
			int i;
			for (i = 1; i < cName.length(); i++) {
				if (cName.charAt(i) == cName.toLowerCase().charAt(i)) {
					if (cName.charAt(i) >= '0' && cName.charAt(i) <= '9') {
						i++;
					}
					break;
				}
			}
			return lName.substring(0, i - 1) + cName.substring(i - 1);
		}
	}
	
}
