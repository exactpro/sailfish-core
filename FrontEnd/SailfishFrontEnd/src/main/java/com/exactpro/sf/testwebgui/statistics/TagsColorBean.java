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
package com.exactpro.sf.testwebgui.statistics;

import java.io.Serializable;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

@ManagedBean(name="tagsColorsBean")
@ApplicationScoped
@SuppressWarnings("serial")
public class TagsColorBean implements Serializable {
	
	private static final String[] colorClasses = 
			new String[] {"turquoise", "preaver", "amethyst", /*"wet-asphalt",*/ "green-sea", 
		"belize-hole", "wisteria", "sun-flower", "carrot", /*"alizarin",*/ "clouds", "orange", "silver"};
	
	public static String getColorClass(String name) {
		
		if(name != null) {
			return colorClasses[Math.abs(name.hashCode()) % colorClasses.length];
		}
		
		return colorClasses[0]; 
		
	}
	
}
