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

import java.util.Collection;
import java.util.List;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.scriptrunner.impl.BroadcastScriptReport;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;

// TODO: split ScriptReport and listening functionality
public interface IScriptReport
{
	public static final String NO_DESCRIPTION = "No description";

	/*
	 * Start of Matrix execution
	 */
	// FIXME: ScriptContext contains IScriptReport... circle references
	void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName, String userName);

	/*
	 * Adds Matrix-level errors (compile time warnings)
	 *
	 * Please note: this method will can be called from other thread.
	 */
	void addAlerts(Collection<AggregateAlert> aggregatedAlerts);

	/*
	 * End of Matrix execution
	 */
	void closeReport();

	void flush();

    void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type);

	void closeTestCase(StatusDescription status);

    default boolean isTestCaseCreated() {
        throw new UnsupportedOperationException("Only " + BroadcastScriptReport.class.getSimpleName() + " should implement this method");
    }

    void createAction(String name, String serviceName, String action, String msg, String description,
                      Object inputParameters, CheckPoint checkPoint, String tag, int hash, List<String> verificationsOrder);

	/**
	 *
	 * @return
	 * @throws UnsupportedOperationException - for ScriptListeners it havn't matter
	 */
	boolean isActionCreated() throws UnsupportedOperationException;

    void createAction(String name, String serviceName, String action, String msg, String description,
                      List<Object> inputParameters, CheckPoint checkPoint, String tag, int hash, List<String> verificationsOrder);

    void closeAction(StatusDescription status, Object actionResult);

    void openGroup(String name, String description);

    void closeGroup(StatusDescription status);

    /**
     * Create verification block which may be contains message comparison result 
     * @param name - name of verification
     * @param description - name of verification
     * @param status - status  of verification 
     * @param result - optional message comparison result
     */
    void createVerification(String name, String description, StatusDescription status, ComparisonResult result);

	void createMessage(MessageLevel level, String... messages);

	void createMessage(MessageLevel level, Throwable e, String... messages);

    default void createMessage(TextColor color, TextStyle style, String... messages) {

    }

	/**
	 * This method create failure status for running
	 * @param cause
	 */
	void createException(Throwable cause);

    void createTable(ReportTable table);

	void createLogTable(List<String> header, List<LoggerRow> rows);

    default void createParametersTable(String messageName, Object message) {
    }

	void setOutcomes(OutcomeCollector outcomes);

    void createLinkToReport(String linkToReport);

	IReportStats getReportStats();
}
