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
package com.exactpro.sf.scriptrunner.reportbuilder;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.impl.JAXBContextHolder;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;

public class XMLReportCreatorImpl implements IXMLReportCreator {

	private static final Logger logger = LoggerFactory.getLogger(XMLReportCreatorImpl.class);
	private static final String REPORT_FILE_NAME = "report.xml";
	private File xmlReportFile = null;

	private final JAXBContext context;
	private final IWorkspaceDispatcher dispatcher;

	public XMLReportCreatorImpl(IWorkspaceDispatcher dispatcher) throws JAXBException {
		this.context = JAXBContextHolder.getJAXBContext();
		this.dispatcher = dispatcher;
	}

	@Override
	public XmlFunctionalReport create(TestScriptDescription descr) throws JAXBException, FileNotFoundException {

		Unmarshaller unmarshaller = context.createUnmarshaller();
		xmlReportFile = dispatcher.getFile(FolderType.REPORT, descr.getWorkFolder(), REPORT_FILE_NAME);
		logger.debug("Unmarshalling [{}]", xmlReportFile);
		XmlFunctionalReport report = (XmlFunctionalReport)unmarshaller.unmarshal(xmlReportFile);
		return report;
	}

	@Override
	public File getReportFileName() {
		return xmlReportFile;
	}


}
