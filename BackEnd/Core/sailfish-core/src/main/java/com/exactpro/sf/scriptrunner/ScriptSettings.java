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
package com.exactpro.sf.scriptrunner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.common.util.ICommonSettings;

public class ScriptSettings implements ICommonSettings 
{
	private static final String TESTREPORT_KEY = "TestReport";
	private static final String TESTSCRIPT_KEY = "TestScript";
	private static final String PROPERTIES_KEY = "ScriptProperties";
	private static final String LOGGING_KEY = "Logging";
	
	
	private String scriptName;
	private String testScriptClassName;
	private IScriptReport scriptReport;
	
	private String consoleLoggerLevel;
	private String fileLoggerLevel;
	private String consoleLayout;
	private String fileLayout;
	private boolean addMessagetoReport;
	private String scriptFolder;
	private Map<String, String> properties;
	
	public ScriptSettings() 
	{
		this.properties = new HashMap<String, String>();
	}
	

	public String getTestScriptClassName()
	{
		return this.testScriptClassName;
	}
	
	
	public void setTestScriptClassName(String testScriptClassName)
	{
		this.testScriptClassName = testScriptClassName;
	}
	
	
	public boolean isAddMessagesToReport()
	{
		return this.addMessagetoReport;
	}
	
	
	public void setAddMessagesToReport(boolean value)
	{
		this.addMessagetoReport = value;
	}
	
	
	public IScriptReport getScriptReport()
	{
		return this.scriptReport;
	}
	
	public void setScriptReport(IScriptReport scriptReport)
	{
		this.scriptReport = scriptReport;
	}
	
	
	public String getProperty(String name)
	{
		return this.properties.get(name);		
	}
	
	
	public Set<String> getPropertiesKeys()
	{
		return this.properties.keySet();
	}
	
	
	public String getScriptName()
	{
		return this.scriptName;
	}
	
	
	public String getConsoleLoggerLevel() {
		return consoleLoggerLevel;
	}


	public void setConsoleLoggerLevel(String consoleLoggerLevel) {
		this.consoleLoggerLevel = consoleLoggerLevel;
	}


	public String getFileLoggerLevel() {
		return fileLoggerLevel;
	}


	public void setFileLoggerLevel(String fileLoggerLevel) {
		this.fileLoggerLevel = fileLoggerLevel;
	}


	public String getConsoleLayout() {
		return consoleLayout;
	}


	public void setConsoleLayout(String consoleLayout) {
		this.consoleLayout = consoleLayout;
	}


	public String getFileLayout() {
		return fileLayout;
	}


	public void setFileLayout(String fileLayout) {
		this.fileLayout = fileLayout;
	}
	
	
	public String getScriptFolder() {
		return scriptFolder;
	}


	public void setScriptFolder(String scriptFolder) {
		this.scriptFolder = scriptFolder;
	}


	
	@Override
	public void load(HierarchicalConfiguration config) 
	{
		HierarchicalConfiguration testScriptConfig = config.configurationAt(TESTSCRIPT_KEY);
		
		String scriptNamePar = config.getString("TestScript[@name]");
		
		if ( scriptNamePar == null )
			throw new ScriptRunException("There is no \"name\" attribute at \"TestScript\" element in the script configuration file");
		
		this.scriptName = scriptNamePar;
		
		if ( !(testScriptConfig.configurationsAt(PROPERTIES_KEY).isEmpty()) )
			loadProperties(testScriptConfig.configurationAt(PROPERTIES_KEY));
		
		if ( !(testScriptConfig.configurationsAt(LOGGING_KEY).isEmpty()) )
			loadLoggingProperties(testScriptConfig.configurationAt(LOGGING_KEY));
		
		if ( !(testScriptConfig.configurationsAt(TESTREPORT_KEY).isEmpty()) )
			loadTestReportProperties(testScriptConfig.configurationAt(TESTREPORT_KEY));
	}
	
	
	private void loadProperties(HierarchicalConfiguration config)
	{
		Iterator<?> it = config.getKeys();

		while (it.hasNext())
		{
		    String key = (String)it.next();
		    String value = config.getString(key);
		    
		    properties.put(key, value);
		}
	}
	
	
	private void loadLoggingProperties(HierarchicalConfiguration config)
	{
		this.consoleLayout = config.getString("ConsoleLogger.Pattern", "%p %t %d{dd MMM yyyy HH:mm:ss,SSS} %c - %m%n");
		this.consoleLoggerLevel = config.getString("ConsoleLogger.Level", "ALL");
		
		
		this.fileLayout = config.getString("FileLogger.Pattern", "%p %t %d{dd MMM yyyy HH:mm:ss,SSS} %c - %m%n");
		this.fileLoggerLevel = config.getString("FileLogger.Level", "ALL");
	}
	
	
	private void loadTestReportProperties(HierarchicalConfiguration config)
	{
		this.addMessagetoReport = config.getBoolean("AddMessagesToReport", true);
	}


	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}
	
}
