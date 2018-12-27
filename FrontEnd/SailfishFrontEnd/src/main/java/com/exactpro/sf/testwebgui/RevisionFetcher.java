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
package com.exactpro.sf.testwebgui;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.scriptrunner.AbstractScriptRunner;

public class RevisionFetcher {
	private String ttRevision;
	private String cmnRevision;
	private String guiRevision;
	
	private String totalVersion;
	
	public RevisionFetcher(String guiRevision) {
		this.ttRevision = AbstractScriptRunner.class.getPackage().getImplementationVersion();
		this.cmnRevision = IMessage.class.getPackage().getImplementationVersion();		
		//this.guiRevision = SFWebApplication.class.getPackage().getImplementationVersion();
		this.guiRevision = guiRevision;		
		
		if(ttRevision == null)
			ttRevision ="";
		if(cmnRevision == null)
			cmnRevision ="";
		if(guiRevision == null)
			guiRevision ="";
		
		this.totalVersion = StringUtil.maximumString(guiRevision, cmnRevision);
	}
	
	public String getTestToolsRev() {
		return this.ttRevision;
	}
	
	public String getCommonRev() {
		return this.cmnRevision;
	}
	
	public String getGUIRev() {
		return this.guiRevision;
	}
	
	public String getTotalRev() {
		return this.totalVersion;
	}
}
