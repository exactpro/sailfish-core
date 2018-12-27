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
package com.exactpro.sf.actions;

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.fast.FASTAbstractTCPClient;

@MatrixActions
@ResourceAliases({"FastMatrixActions"})
public class FastMatrixActions extends AbstractCaller {
	@CommonColumns({
        @CommonColumn(Column.CheckPoint),
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
	@ActionMethod
    public void FAST_Connect(IActionContext actionContext)
	throws Exception {
		IInitiatorService client = getClient(actionContext);
		if (!(client instanceof FASTAbstractTCPClient)) {
			throw new EPSCommonException("Service can not be connected");
		}

        FASTAbstractTCPClient fClient = (FASTAbstractTCPClient) client;
		fClient.connect();
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void FAST_Disconnect(IActionContext actionContext)
	throws Exception {
		IInitiatorService client = getClient(actionContext);
		if (!(client instanceof FASTAbstractTCPClient)) {
			throw new EPSCommonException("Service can not be disconnected");
		}
        FASTAbstractTCPClient fClient = (FASTAbstractTCPClient) client;
		fClient.disconnect();
	}

    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void FAST_isDisconnected(IActionContext actionContext)
	throws Exception {
		IInitiatorService client = getClient(actionContext);
		if (!(client instanceof FASTAbstractTCPClient)) {
			throw new EPSCommonException("Service can not is disconnected");
		}
        FASTAbstractTCPClient fClient = (FASTAbstractTCPClient) client;
        String actionId = actionContext.getId();
        if (actionId != null && !actionId.equals("")) actionId += " ";
        IActionReport report = actionContext.getReport();
        String verificationName = actionId + String.format("Checking whether the service [%s] is disconnected", client.getName());
        ComparisonResult cr = new ComparisonResult("FAST_isDisconnected");
        StatusType status = null;
        String actual = null;
        if (fClient.getSession().isClosed()) {
            status = StatusType.PASSED;
            actual = "true";
        }
        else {
            status = StatusType.FAILED;
            actual = "false";
        }
        cr.setStatus(status);
        cr.setActual(actual);
        cr.setExpected("true");
        report.createVerification(status, verificationName, "", "", cr);

        if(status == StatusType.FAILED) {
            throw new EPSCommonException(String.format("Service [%s] is connected", client.getName()));
        }
	}


    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void FAST_isConnected(IActionContext actionContext)
	throws Exception {
		IInitiatorService client = getClient(actionContext);
		if (!(client instanceof FASTAbstractTCPClient)) {
			throw new EPSCommonException("Service can not is connected");
		}
        FASTAbstractTCPClient fClient = (FASTAbstractTCPClient) client;
        String actionId = actionContext.getId();
        if (actionId != null && !actionId.equals("")) actionId += " ";
        IActionReport report = actionContext.getReport();
        String verificationName = actionId + String.format("Checking whether the session [%s] is connected", client.getName());
        ComparisonResult cr = new ComparisonResult("FAST_isConnected");
        StatusType status;
        String actual;
        if (fClient.getSession().isClosed()) {
            status = StatusType.FAILED;
            actual = "false";
        } else {
            status = StatusType.PASSED;
            actual = "true";
        }
        cr.setStatus(status);
        cr.setActual(actual);
        cr.setExpected("true");
        report.createVerification(status, verificationName, "", "", cr);

        if(status == StatusType.FAILED) {
            throw new EPSCommonException(String.format("Service [%s] is disconnected", client.getName()));
        }
	}


	private static IInitiatorService getClient(IActionContext actionContext)
	{
		return ActionUtil.getService(actionContext, IInitiatorService.class);
	}

}
