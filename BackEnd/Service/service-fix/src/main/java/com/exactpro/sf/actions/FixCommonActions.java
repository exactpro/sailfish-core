/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import static com.exactpro.sf.actions.ActionUtil.unwrapFilters;
import static com.exactpro.sf.services.fix.FixUtil.convertToNumber;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.services.fix.FIXClient;
import com.exactpro.sf.services.fix.FixMessageHelper;

@MatrixActions
@ResourceAliases("FixCommonActions")
public class FixCommonActions extends AbstractCaller {

    public static final String BEGIN = "begin";
    public static final String END = "end";

    @Description("Retrieve messages from begin and to end seq numbers from internal storage.")
    @CommonColumns(@CommonColumn(value = Column.ServiceName, required = true))
    @CustomColumns({
            @CustomColumn(value = BEGIN, required = true),
            @CustomColumn(END)
    })
    @ActionMethod
    public void retrieveMessages(IActionContext actionContext, HashMap<?, ?> hashMap) {
        Object begin = unwrapFilters(hashMap.get(BEGIN));
        Object end = unwrapFilters(hashMap.get(END));

        int beginSeqNum = convertToNumber(BEGIN, begin).intValue();
        int endSeqNum = (end != null)
                ? convertToNumber(END, end).intValue()
                : Integer.MAX_VALUE;

        FIXClient service = FIXMatrixUtil.getClient(actionContext);

        IActionReport report = actionContext.getReport();
        List<String > header = Arrays.asList("Message", "SeqNum","Content");

        ReportTable table = new ReportTable("Retrieved messages", header);

        try {
            List<IMessage> messages = service.retrieve(beginSeqNum, endSeqNum);

            for (IMessage message : messages) {
                Map<String, String> row = new HashMap<>();
                row.put("Message", message.getName());
                row.put("Content", message.toString());

                String seqNum = "-";
                Object headerMsg = message.getField("header");
                if (headerMsg instanceof IMessage) {
                    Object msgSeqNum = ((IMessage) headerMsg).getField(FixMessageHelper.MSG_SEQ_NUM_FIELD);
                    if (msgSeqNum != null) {
                        seqNum = msgSeqNum.toString();
                    }
                }
                row.put("SeqNum", seqNum);
                table.addRow(row);
            }
            report.createTable(StatusType.PASSED, table);
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }
}
