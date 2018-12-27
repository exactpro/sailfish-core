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
package com.exactpro.sf.testwebgui.restapi.xml;


import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "testscriptactions")
public class XmlTestSciptrunActionList {
	
	 private List<XmlTestscriptActionResponse> testsciptActions;

	 @XmlElement(name = "testscriptrun")
	 public List<XmlTestscriptActionResponse> getTestScriptRuns() {
		 return testsciptActions;
	 }

	 public void setTestscriptRuns(List<XmlTestscriptActionResponse> testScriptActionList) {
		 this.testsciptActions = testScriptActionList;
	 }

}
