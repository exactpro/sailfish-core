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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.testwebgui.notifications.events.EventRetriever;
import com.exactpro.sf.testwebgui.notifications.matrices.MatrixUpdateRetriever;
import com.exactpro.sf.testwebgui.notifications.messages.MessagesUpdateRetriever;
import com.exactpro.sf.testwebgui.notifications.scriptrunner.ScriptrunUpdateRetriever;
import com.exactpro.sf.testwebgui.notifications.services.EnvironmentUpdateRetriever;
import com.exactprosystems.webchannels.IUpdateRetriever;

public class SFWebApplication {

	private static final Logger logger = LoggerFactory.getLogger(SFWebApplication.class);

	private static volatile SFWebApplication instance;

	private IUpdateRetriever scriptrunsUpdateRetriever;
	
	private IUpdateRetriever matrixUpdateRetriever;
	
	private IUpdateRetriever environmentUpdateRetriever;

	private IUpdateRetriever messagesUpdateRetriever;
	
	private IUpdateRetriever eventRetriever;

	//private volatile boolean dbError = false;
	
	private volatile boolean fatalError = false;
	
	private volatile String fatalErrorMessage;

	private SFWebApplication() {

	}

	public static SFWebApplication getInstance() {
		if (instance == null) {
			synchronized (SFWebApplication.class) {
				instance = new SFWebApplication();
			}
		}
		return instance;
	}

	public void init(String logFilePath, ISFContext context) {

		logger.debug("init() invoked");

		this.scriptrunsUpdateRetriever = new ScriptrunUpdateRetriever(context);
		this.matrixUpdateRetriever = new MatrixUpdateRetriever();
		this.environmentUpdateRetriever = new EnvironmentUpdateRetriever();
		this.eventRetriever = new EventRetriever();
		this.messagesUpdateRetriever = new MessagesUpdateRetriever();
	}
	
	public boolean isPageNotRestricted(String name) {
		if(ReleaseController.getRestrictions() != null) {        
	        for(String suffix : ReleaseController.getRestrictions()) {
	        	if(name.equals(suffix))
	        		return false;
	        }
		}
		return true;
	}
	
	public String getRelease() {
		return ReleaseController.getRelease();
	}

	public IUpdateRetriever getScriptrunsUpdateRetriever() {
		return scriptrunsUpdateRetriever;
	}

	public IUpdateRetriever getMatrixUpdateRetriever() {
		return matrixUpdateRetriever;
	}

	public IUpdateRetriever getEnvironmentUpdateRetriever() {
		return environmentUpdateRetriever;
	}

	public IUpdateRetriever getEventRetriever() {
		return eventRetriever;
	}

	public IUpdateRetriever getMessagesUpdateRetriever() {
		return messagesUpdateRetriever;
	}

	public boolean isFatalError() {
		return fatalError;
	}

	public void setFatalError(boolean fatalError) {
		this.fatalError = fatalError;
	}

	public String getFatalErrorMessage() {
		return fatalErrorMessage;
	}

	public void setFatalErrorMessage(String fatalErrorMessage) {
		this.fatalErrorMessage = fatalErrorMessage;
	}

}
