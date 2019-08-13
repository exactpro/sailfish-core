/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.scriptrunner;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.scriptrunner.impl.BroadcastScriptReport;
import com.exactpro.sf.scriptrunner.impl.htmlreport.HtmlReport;
import com.exactpro.sf.scriptrunner.impl.jsonreport.JsonReport;
import com.exactpro.sf.util.AbstractTest;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

public class TestIsActionCreated  extends AbstractTest {

    private final IMessageFactory msgFactory = DefaultMessageFactory.getFactory();

    /***
     * Check that nested opened actions reported as alredy created
     */
    @Test
    public void test() {

        HtmlReport htmlReport = new HtmlReport("", workspaceDispatcher, serviceContext.getDictionaryManager(),
                EnvironmentSettings.RelevantMessagesSortingMode.ARRIVAL_TIME);
        JsonReport jsonReport = new JsonReport("", workspaceDispatcher, Mockito.mock(TestScriptDescription.class));
        ZipReport zipReport = new ZipReport("", workspaceDispatcher, Mockito.mock(TestScriptDescription.class), EnvironmentSettings.ReportOutputFormat.ZIP);


        BroadcastScriptReport broadcastScriptReport = new BroadcastScriptReport(Arrays.asList(htmlReport, jsonReport, zipReport));

        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(false));

        broadcastScriptReport.createReport(Mockito.mock(ScriptContext.class), "name", "description", 0, "default", "user");
        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(false));

        broadcastScriptReport.createTestCase("reference", "description", 0, 0, "tcid", 0,
                AMLBlockType.TestCase, Collections.emptySet());
        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(false));

        broadcastScriptReport.createAction("0", "service", "root", "TEST", "description",
                msgFactory.createMessage("Test", "Test"), null, "tag", 0, Collections.emptyList(), "outcome");
        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(true));

        broadcastScriptReport.createAction("0", "service", "nested", "TEST", "description",
                msgFactory.createMessage("Test", "Test"), null, "tag", 0, Collections.emptyList(), "outcome");
        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(true));

        broadcastScriptReport.closeAction(new StatusDescription(StatusType.PASSED,"description"), "Result");

        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(true));

        broadcastScriptReport.closeAction(new StatusDescription(StatusType.PASSED,"description"), "Result");
        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(false));

        broadcastScriptReport.closeTestCase(new StatusDescription(StatusType.PASSED, "description"));
        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(false));

        broadcastScriptReport.closeReport();
        Assert.assertThat("Action created but, report reports that not", broadcastScriptReport.isActionCreated(), CoreMatchers.is(false));
    }

}
