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

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.services.CSHIterator;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fix.FIXClient;
import com.exactpro.sf.services.fix.FixMessageHelper;
import com.exactpro.sf.util.DateTimeUtility;
import quickfix.DateField;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.SendingTime;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.exactpro.sf.actions.ActionUtil.unwrapFilters;
import static com.exactpro.sf.services.fix.FixUtil.convertToNumber;

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
    //----------------NewFixMsgs end-----------------------------------//

    @ActionMethod
    public HashMap<?, ?> SetVariables(IActionContext actionContext, HashMap<?, ?> message) throws Exception {
        return message.isEmpty() ? null : unwrapFilters(message);
    }

    @CustomColumns({
            @CustomColumn(value = "DateField", required = true),
            @CustomColumn(value = "DateString", required = true),
            @CustomColumn(value = "DateStringFormat", required = true),
            @CustomColumn(value = "Delta", required = true)
    })
    @ActionMethod
    public void FIX_DateStringMinusDateField(IActionContext actionContext,
                                             HashMap<?, ?> messageFilter) throws Exception {
        String field1 = "DateField";
        String field2 = "DateString";
        String field2format = "DateStringFormat";
        String field3 = "Delta";

        if (!messageFilter.containsKey(field1)) {
            throw new EPSCommonException(field1 + " is absent");
        }
        if (!messageFilter.containsKey(field2)) {
            throw new EPSCommonException(field2 + " is absent");
        }
        if (!messageFilter.containsKey(field3)) {
            throw new EPSCommonException(field3 + " is absent");
        }
        if (!messageFilter.containsKey(field2format)) {
            throw new EPSCommonException(field2format + " is absent");
        }

        Timestamp value = ((DateField) unwrapFilters(messageFilter.get(field1))).getValue();
        LocalDateTime localDateTime = DateTimeUtility.toLocalDateTime(value);
        BigDecimal value1 = new BigDecimal(FIXUtility.extractMillisecFromMinut(localDateTime));

        SimpleDateFormat format = new SimpleDateFormat(unwrapFilters(messageFilter.get(field2format)));

        localDateTime = DateTimeUtility.toLocalDateTime(format.parse(unwrapFilters(messageFilter.get(field2))));

        BigDecimal value2 = new BigDecimal(FIXUtility.extractMillisecFromMinut(localDateTime));

        BigDecimal delta = new BigDecimal(unwrapFilters(messageFilter.get(field3)).toString());

        delta(actionContext, value2, value1, delta);
    }

    @CustomColumns({
            @CustomColumn(value = "DateField", required = true),
            @CustomColumn(value = "DateString", required = true),
            @CustomColumn(value = "DateStringFormat", required = true),
            @CustomColumn(value = "Delta", required = true)
    })
    @ActionMethod
    public void FIX_DateFieldMinusDateString(IActionContext actionContext,
                                             HashMap<?, ?> messageFilter) throws Exception {
        String field1 = "DateField";
        String field2 = "DateString";
        String field2format = "DateStringFormat";
        String field3 = "Delta";

        if (!messageFilter.containsKey(field1)) {
            throw new EPSCommonException(field1 + " is absent");
        }
        if (!messageFilter.containsKey(field2)) {
            throw new EPSCommonException(field2 + " is absent");
        }
        if (!messageFilter.containsKey(field3)) {
            throw new EPSCommonException(field3 + " is absent");
        }
        if (!messageFilter.containsKey(field2format)) {
            throw new EPSCommonException(field2format + " is absent");
        }

        Timestamp value = ((DateField) unwrapFilters(messageFilter.get(field1))).getValue();
        LocalDateTime localDateTime = DateTimeUtility.toLocalDateTime(value);
        BigDecimal value1 = new BigDecimal(FIXUtility.extractMillisecFromMinut(localDateTime));

        SimpleDateFormat format = new SimpleDateFormat(unwrapFilters(messageFilter.get(field2format)));

        localDateTime = DateTimeUtility.toLocalDateTime(format.parse(unwrapFilters(messageFilter.get(field2))));

        BigDecimal value2 = new BigDecimal(FIXUtility.extractMillisecFromMinut(localDateTime));

        BigDecimal delta = new BigDecimal(unwrapFilters(messageFilter.get(field3)).toString());

        delta(actionContext, value1, value2, delta);
    }

    @CustomColumns({
            @CustomColumn(value = "Delta", required = true),
            @CustomColumn(value = "DeltaMessageMinuend", required = true),
            @CustomColumn(value = "DeltaMessageMinuendFormat", required = true),
            @CustomColumn(value = "DeltaMessageSubstracter", required = true),
            @CustomColumn(value = "DeltaMessageSubstracterFormat", required = true)
    })
    @ActionMethod
    public void FIX_DateDeltaField(IActionContext actionContext,
                                   HashMap<?, ?> messageFilter) throws Exception {
        String field1 = "DeltaMessageMinuend";
        String field1format = "DeltaMessageMinuendFormat";
        String field2 = "DeltaMessageSubstracter";
        String field2format = "DeltaMessageSubstracterFormat";
        String field3 = "Delta";

        if (!messageFilter.containsKey(field1)) {
            throw new EPSCommonException("DeltaMessageMinuend is absent");
        }
        if (!messageFilter.containsKey(field2)) {
            throw new EPSCommonException("DeltaMessageSubtracter is absent");
        }
        if (!messageFilter.containsKey(field3)) {
            throw new EPSCommonException("Delta is absent");
        }
        if (!messageFilter.containsKey(field1format)) {
            throw new EPSCommonException("DeltaMessageMinuendFormat is absent");
        }
        if (!messageFilter.containsKey(field2format)) {
            throw new EPSCommonException(
                    "DeltaMessageSubtracterFormat is absent");
        }

        SimpleDateFormat format1 = new SimpleDateFormat(unwrapFilters(messageFilter.get(field1format)).toString());
        SimpleDateFormat format2 = new SimpleDateFormat(unwrapFilters(messageFilter.get(field2format)).toString());

        long value = format1.parse(unwrapFilters(messageFilter.get(field1)).toString()).getTime();
        BigDecimal value1 = new BigDecimal(value);

        value = format2.parse(unwrapFilters(messageFilter.get(field2)).toString()).getTime();
        BigDecimal value2 = new BigDecimal(value);

        BigDecimal delta = new BigDecimal(unwrapFilters(messageFilter.get(field3)).toString());

        delta(actionContext, value1, value2, delta);
    }

    @CustomColumns({
            @CustomColumn(value = "Delta", required = true),
            @CustomColumn(value = "DeltaMessageMinuend", required = true),
            @CustomColumn(value = "DeltaMessageSubtracter", required = true)
    })
    @ActionMethod
    public void FIX_DeltaField(IActionContext actionContext,
                               HashMap<?, ?> messageFilter) throws Exception {

        String field1 = "DeltaMessageMinuend";
        String field2 = "DeltaMessageSubtracter";
        String field3 = "Delta";

        if (!messageFilter.containsKey(field1)) {
            throw new EPSCommonException();
        }
        if (!messageFilter.containsKey(field2)) {
            throw new EPSCommonException();
        }
        if (!messageFilter.containsKey(field3)) {
            throw new EPSCommonException();
        }

        BigDecimal value1 = new BigDecimal(unwrapFilters(messageFilter.get(field1)).toString());
        BigDecimal value2 = new BigDecimal(unwrapFilters(messageFilter.get(field2)).toString());
        BigDecimal delta = new BigDecimal(unwrapFilters(messageFilter.get(field3)).toString());

        delta(actionContext, value1, value2, delta);
    }

    private static void delta(IActionContext actionContext, BigDecimal value1,
                              BigDecimal value2, BigDecimal delta) throws Exception {
        BigDecimal actual = value1.subtract(value2);
        IActionReport report = actionContext.getReport();

        ComparisonResult result = new ComparisonResult("delta");

        result.setActual(actual.toPlainString());
        result.setName("The difference must be");
        result.setExpected(" less than " + delta.toPlainString());

        if (actual.compareTo(delta) == -1) {
            result.setStatus(StatusType.PASSED);
            report.createVerification(StatusType.PASSED, "Fields comparison", "Compares difference between two BigDecimal values", "An actual difference is lesser than it was expected.", result);
        } else {
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Fields comparison", "Compares difference between two BigDecimal values", "An actual difference is greater than it was expected.", result);
            throw new EPSCommonException(
                    "A difference between fields is greater than allowed");
        }
    }

    @CustomColumns({
            @CustomColumn("RealTags"),
            @CustomColumn("WaitingGroup"),
            @CustomColumn("WaitingTags")
    })
    @Deprecated
    @ActionMethod
    public Message FIX_WaitMessageWithTags(
            IActionContext actionContext, HashMap<?, ?> messageFilter)
            throws Exception {
        List<String> masWaitingTags = new ArrayList<>();
        List<String> masRealTags = new ArrayList<>();

        boolean checkedRight = false;
        boolean addToReport = actionContext.isAddToReport();
        String groupHeader = null;
        String groupDelimiter = null;
        List<String> groupTags = new ArrayList<>();

        if (messageFilter.containsKey("RealTags")) {
            String[] tmpTags = ((String) unwrapFilters(messageFilter.get("RealTags")))
                    .split(";");

            for (String tmpTag : tmpTags) {
                masRealTags.add(tmpTag);
            }
        }

        if (messageFilter.containsKey("WaitingTags")) {
            String[] waitingTags = ((String) unwrapFilters(messageFilter.get("WaitingTags")))
                    .split(";");

            for (String waitingTag : waitingTags) {
                masWaitingTags.add(waitingTag + "|");
            }
        }

        if (messageFilter.containsKey("WaitingGroup")) {
            String[] waitingTags = ((String) unwrapFilters(messageFilter.get("WaitingGroup")))
                    .split(";");

            groupHeader = waitingTags[0];
            groupDelimiter = waitingTags[1];

            for (int i = 2; i < waitingTags.length; i++) {
                groupTags.add(waitingTags[i] + "|");
            }
        }

        IInitiatorService service = FIXMatrixUtil.getClient(actionContext);
        ISession isession = service.getSession();

        long waitTime = actionContext.getTimeout();

        CollectorServiceHandler handler = (CollectorServiceHandler) service
                .getServiceHandler();

        long endTime = System.currentTimeMillis() + waitTime;

        IActionReport report = actionContext.getReport();

        report.createMessage(StatusType.NA, MessageLevel.WARN, "DO NOT USE THIS ACTION DUE TO RESTRICTION. THIS ACTION DEPRECATED AND WILL BE REMOVED SOON.");

        CSHIterator<IMessage> messagesIterator = handler.getIterator(isession, ServiceHandlerRoute.FROM_APP, actionContext.getCheckPoint());

        while (messagesIterator.hasNext(endTime - System.currentTimeMillis())) {
            IMessage message = messagesIterator.next();

            String htmlMsg = new String(message.toString().getBytes(),
                    StandardCharsets.UTF_8);

            StringBuilder tmpMsg = new StringBuilder();
            int countChars = 0;

            for (int j = 0; j < htmlMsg.length(); j++) {
                countChars++;
                if (htmlMsg.charAt(j) == '\001') {
                    tmpMsg.append("|");
                    if (countChars > 140) {
                        countChars = 0;
                        tmpMsg.append("<br>");
                    }

                } else {
                    tmpMsg.append(htmlMsg.charAt(j));
                }
            }

            htmlMsg = tmpMsg.toString();
            ReportTable table = new ReportTable(tmpMsg.toString(), Arrays.asList("EXPECTED", "ACTUAL", "STATUS"));

            // compare message
            // **** WaitingGroupTags ****
            if (!groupTags.isEmpty()) {
                // remove part before start of Group
                htmlMsg = htmlMsg.replaceAll("<br>", "");

                if (htmlMsg.indexOf(groupHeader + "=") <= 0) {
                    continue;
                }

                htmlMsg = htmlMsg.substring(htmlMsg.indexOf(groupHeader
                        + "="), htmlMsg.length());
                htmlMsg = htmlMsg.substring(htmlMsg.indexOf("|") + 1,
                        htmlMsg.length());

                if (htmlMsg.indexOf(groupDelimiter) != 0) {
                    if (addToReport) {
                        try (IActionReport embeddedReport = report.createEmbeddedReport("Wait for message",
                                tmpMsg.toString())) {
                            embeddedReport.createMessage(StatusType.FAILED, MessageLevel.INFO,
                                    "Have not needed group. in:<br> " + tmpMsg);
                            embeddedReport.createVerification(StatusType.FAILED, "Wait for message", tmpMsg.toString(), "");
                        }
                    }
                    continue;
                }
                htmlMsg = htmlMsg.substring(htmlMsg.indexOf("=") + 1,
                        htmlMsg.length());

                // fill array with repeating groups
                List<String> msgGroups = new ArrayList<>();

                String[] groupInTags = htmlMsg.split(groupDelimiter + "=");

                for (String groupInTag : groupInTags) {
                    msgGroups.add(groupDelimiter + "=" + groupInTag);
                }

                // find tags in group
                for (int i = 0; i < msgGroups.size(); i++) {
                    String msg = msgGroups.get(i);
                    int passedCountForGroup = 0;
                    for (int j = 0; j < groupTags.size(); j++) {
                        String firstpart = groupTags.get(j).split("=")[0] + "=";
                        int position = msg.indexOf(firstpart);
                        if (position < 0) {
                            continue;
                        }

                        if (msg.contains(groupTags.get(j))) {
                            passedCountForGroup++;
                            String tempStr = groupTags.get(j).substring(0, groupTags.get(j).length() - 1);
                            String newMsg = msg.replaceFirst(tempStr, "<font color='green'><strong>" + tempStr + "</strong></font>");
                            msgGroups.set(i, newMsg);
                        } else if (msg.contains(firstpart)) {
                            String tempStr = msg.substring(position, msg.indexOf("|", position));
                            String newMsg = msg.replaceFirst(tempStr, "<font color='red'><strong>" + tempStr + "</strong></font>");
                            msgGroups.set(i, newMsg);
                        }
                    }

                    ReportTable verificationTable = new ReportTable("Groups", Arrays.asList("group"));
                    Map<String, String> row = new HashMap<>();

                    row.put("group", msg.toString());
                    verificationTable.addRow(row);

                    if (passedCountForGroup == groupTags.size()) {
                        if (addToReport) {
                            try (IActionReport embeddedReport = report.createEmbeddedReport("GROUP: "
                                    + passedCountForGroup + " passed from "
                                    + groupTags.size(), "#" + i)) {
                                embeddedReport.createTable(StatusType.PASSED, verificationTable);
                                embeddedReport.createVerification(StatusType.PASSED, "GROUP: "
                                        + passedCountForGroup + " passed from "
                                        + groupTags.size(), "#" + i, "");
                            }
                        }
                        checkedRight = true;
                    } else {
                        if (addToReport) {
                            try (IActionReport embeddedReport = report.createEmbeddedReport("GROUP: "
                                    + passedCountForGroup + " passed from "
                                    + groupTags.size(), "#" + i)) {
                                embeddedReport.createTable(StatusType.FAILED, verificationTable);
                                embeddedReport.createVerification(StatusType.FAILED, "GROUP: "
                                        + passedCountForGroup + " passed from "
                                        + groupTags.size(), "#" + i, "");
                            }
                        }
                        checkedRight = false;
                    }
                }
                if (!checkedRight) {
                    throw new Exception("Incorrect message");
                }
            }
            // **** WaitingTags ****
            if (!masWaitingTags.isEmpty()) {
                int passedCount = 0;
                for (int i = 0; i < masWaitingTags.size(); i++) {
                    String pair = masWaitingTags.get(i);
                    String tag = pair.split("=")[0];

                    Map<String, String> row = new HashMap<>();

                    String firstpart = tag + "=";
                    int position = htmlMsg.indexOf(firstpart);

                    if (masRealTags.contains(tag)) {
                        if (htmlMsg.contains(firstpart)) {
                            String tempStr = htmlMsg.substring(
                                    position, htmlMsg.indexOf("|",
                                            position));

                            double tmpOrigValue = Double
                                    .parseDouble(pair
                                            .split("=")[1]);
                            double tmpActValue = Double
                                    .parseDouble(tempStr);
                            row.put("EXPECTED", pair);
                            row.put("ACTUAL", tempStr);
                            if (tmpOrigValue == tmpActValue) {
                                row.put("STATUS", "OK");
                                passedCount++;
                            } else {
                                row.put("STATUS", "FAILED");
                            }
                        }
                    } else {
                        if (htmlMsg.contains(pair)) {
                            passedCount++;
                            String tempStr = pair
                                    .substring(
                                            0,
                                            pair
                                                    .length() - 1);
                            row.put("EXPECTED", pair);
                            row.put("ACTUAL", tempStr);
                            row.put("STATUS", "OK");
                        } else if (htmlMsg.contains(firstpart)) {
                            String tempStr = htmlMsg.substring(
                                    position, htmlMsg.indexOf("|",
                                            position));
                            row.put("EXPECTED", pair);
                            row.put("ACTUAL", tempStr);
                            row.put("STATUS", "FAILED");
                        }
                    }

                    table.addRow(row);
                }
                if (passedCount == masWaitingTags.size()) {
                    if (addToReport) {
                        report.createTable(StatusType.NA, table);
                    }
                    return (Message) message;
                }
                if (addToReport) {
                    report.createTable(StatusType.NA, table);
                }
                continue;
            } else if (checkedRight) {
                return (Message) message;
            }

            // do not wait if timeout reached or when timeout == 0
            if (endTime <= System.currentTimeMillis()) {
                return null; // PartialList
            }
        }

        throw new Exception("Time is over");
    }


    @CommonColumns({
            @CommonColumn(Column.CheckPoint),
            @CommonColumn(Column.DoublePrecision),
            @CommonColumn(value = Column.MessageCount, required = true),
            @CommonColumn(value = Column.ServiceName, required = true),
            @CommonColumn(Column.SystemPrecision),
            @CommonColumn(value = Column.Timeout, required = true)
    })
    @ActionMethod
    public void FIX_CountApplicationTotal(IActionContext actionContext) throws Exception {
        WaitAction.countMessages(actionContext, null, true);
    }

    @CommonColumns({
            @CommonColumn(Column.CheckPoint),
            @CommonColumn(Column.DoublePrecision),
            @CommonColumn(value = Column.MessageCount, required = true),
            @CommonColumn(value = Column.ServiceName, required = true),
            @CommonColumn(Column.SystemPrecision),
            @CommonColumn(value = Column.Timeout, required = true)
    })
    @ActionMethod
    public void FIX_CountAdminTotal(IActionContext actionContext) throws Exception {
        WaitAction.countMessages(actionContext, null, false);
    }


    @CommonColumns(@CommonColumn(value = Column.ServiceName, required = true))
    @ActionMethod
    public void FIX_Disconnect(IActionContext actionContext) {
        FIXClient service = FIXMatrixUtil.getClient(actionContext);
        service.forcedDisconnect();
    }

    @CustomColumns({
            @CustomColumn(value = "EndRef", required = true),
            @CustomColumn(value = "LatencyMax", required = true),
            @CustomColumn(value = "LatencyMin", required = true),
            @CustomColumn(value = "StartRef", required = true)
    })
    @ActionMethod
    public void FIX_CheckLatency(IActionContext actionContext, HashMap<String, String> args) {
        IActionReport report = actionContext.getReport();

        ComparisonResult result = new ComparisonResult("FIX_CheckLatency");

        String startRef = args.get("StartRef");
        String endRef = args.get("EndRef");
        Object o;

        try {
            o = actionContext.getMessage(startRef);
        } catch (EPSCommonException e) {
            result.setName("Latency");
            result.setExpected("");
            result.setActual("Unable to find message StartRef [" + startRef + "]");
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Unable to find message StartRef [" + startRef + "]", result);
            throw new EPSCommonException("Unable to find message StartRef [" + startRef + "]");
        }
        if (!(o instanceof Message)) {
            result.setName("Latency");
            result.setExpected("");
            result.setActual("Message StartRef [" + startRef + "] is not FIX message");
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Message StartRef [" + startRef + "] is not FIX message", result);
            throw new EPSCommonException("Message StartRef [" + startRef + "] is not FIX message");
        }
        Message m1 = (Message) o;

        try {
            o = actionContext.getMessage(endRef);
        } catch (EPSCommonException e) {
            result.setName("Latency");
            result.setExpected("");
            result.setActual("Unable to find message EndRef [" + endRef + "]");
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Unable to find message EndRef [" + endRef + "]", result);
            throw new EPSCommonException("Unable to find message EndRef [" + endRef + "]");
        }

        if (!(o instanceof Message)) {
            result.setName("Latency");
            result.setExpected("");
            result.setActual("Message EndRef [" + endRef + "] is not FIX message");
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Message EndRef [" + endRef + "] is not FIX message", result);
            throw new EPSCommonException("Message EndRef [" + endRef + "] is not FIX message");
        }

        Message m2 = (Message) o;

        try {
            o = m1.getHeader().getUtcTimeStamp(SendingTime.FIELD).getTime();
        } catch (FieldNotFound e) {
            result.setName("Latency");
            result.setExpected(m1.toString());
            result.setActual("Message StartRef [" + startRef + "] did not contain SendingTime (" + SendingTime.FIELD + ") tag");
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Message StartRef [" + startRef + "] did not contain SendingTime (" + SendingTime.FIELD + ") tag", result);
            throw new EPSCommonException("Message StartRef [" + startRef + "] did not contain SendingTime (" + SendingTime.FIELD + ") tag");
        }

        long timestamp1;
        try {
            timestamp1 = (long) o;
        } catch (ClassCastException e) {
            result.setName("Latency");
            result.setExpected(m2.toString());
            result.setActual("Timestamp in message StartRef [" + startRef + "] is not instance of Date class: " + o.getClass());
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Timestamp in message StartRef [" + startRef + "] is not in number format: " + o, result);
            throw new EPSCommonException("Timestamp in message StartRef [" + startRef + "] is not instance of Date class: " + o.getClass());
        }

        try {
            o = m2.getHeader().getUtcTimeStamp(SendingTime.FIELD).getTime();
        } catch (FieldNotFound e) {
            result.setName("Latency");
            result.setExpected(m2.toString());
            result.setActual("Message EndRef [" + endRef + "] did not contain SendingTime (" + SendingTime.FIELD + ") tag");
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Message EndRef [" + endRef + "] did not contain SendingTime (" + SendingTime.FIELD + ") tag", result);
            throw new EPSCommonException("Message EndRef [" + endRef + "] did not contain SendingTime (" + SendingTime.FIELD + ") tag");
        }

        long timestamp2;
        try {
            timestamp2 = (long) o;
        } catch (ClassCastException e) {
            result.setName("Latency");
            result.setExpected(m2.toString());
            result.setActual("Timestamp in message EndRef [" + endRef + "] is not instance of Date class: " + o.getClass());
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Timestamp in message EndRef [" + endRef + "] is not in number format: " + o, result);
            throw new EPSCommonException("Timestamp in message EndRef [" + endRef + "] is not instance of Date class: " + o.getClass());
        }

        long delta = timestamp2 - timestamp1;
        long latencyMin = 0;
        try {
            latencyMin = Long.parseLong(args.get("LatencyMin"));
        } catch (NumberFormatException e) {
            result.setName("Latency");
            result.setExpected("");
            result.setActual("LatencyMin is not in number: " + args.get("LatencyMin"));
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "LatencyMin is not in number: " + args.get("LatencyMin"), result);
            throw new EPSCommonException("LatencyMin is not in number: " + args.get("LatencyMin"));
        }
        long latencyMax = 0;
        try {
            latencyMax = Long.parseLong(args.get("LatencyMax"));
        } catch (NumberFormatException e) {
            result.setName("Latency");
            result.setExpected("");
            result.setActual("LatencyMax is not in number: " + args.get("LatencyMax"));
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "LatencyMax is not in number: " + args.get("LatencyMax"), result);
            throw new EPSCommonException("LatencyMax is not in number: " + args.get("LatencyMax"));
        }
        if (delta < latencyMin) {
            result.setName("Latency");
            result.setExpected("");
            result.setActual("Latency is less LatencyMin: " + delta + " < " + latencyMin);
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Latency is less LatencyMin: " + delta + " < " + latencyMin, result);
            throw new EPSCommonException("Latency is less LatencyMin: " + delta + " < " + latencyMin);
        }
        if (delta > latencyMax) {
            result.setName("Latency");
            result.setExpected("");
            result.setActual("Latency is greater LatencyMax: " + delta + " > " + latencyMax);
            result.setStatus(StatusType.FAILED);
            report.createVerification(StatusType.FAILED, "Latency check", "Check latency in a range", "Latency is greater LatencyMax: " + delta + " > " + latencyMax, result);
            throw new EPSCommonException("Latency is greater LatencyMax: " + delta + " > " + latencyMax);
        }
        result.setName("Latency");
        result.setExpected("");
        result.setActual("Latency is in a range: " + latencyMin + " <= " + delta + " <= " + latencyMax);
        result.setStatus(StatusType.PASSED);
        report.createVerification(StatusType.PASSED, "Latency check", "Check latency in a range", "Latency is in a range: " + latencyMin + " <= " + delta + " <= " + latencyMax, result);
    }

    @CommonColumns(@CommonColumn(Column.Reference))
    @CustomColumns({
            @CustomColumn(value = "endDate", required = true),
            @CustomColumn(value = "startDate", required = true)
    })
    @ActionMethod
    public long DiffBetweenDateFieldsInSec(IActionContext actionContext, HashMap<?, ?> inputData) throws InterruptedException {
        Object start = unwrapFilters(inputData.get("startDate"));
        Object end = unwrapFilters(inputData.get("endDate"));
        Timestamp startDate;
        Timestamp endDate;
        if (start instanceof DateField && end instanceof DateField) {
            startDate = ((DateField) start).getObject();
            endDate = ((DateField) end).getObject();
        } else {
            throw new IllegalArgumentException("Field startDate or endDate have got value not instance of DateField");
        }

        actionContext.getLogger().info("DiffBetweenDateFieldsInSec start:{} end:{}", startDate, endDate);

        return (endDate.getTime() - startDate.getTime()) / 1000;
    }
}