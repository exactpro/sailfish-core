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
package com.exactpro.sf.storage;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class MessageRow {
    public static ThreadLocal<SimpleDateFormat> TIMESTAMP_FORMAT = new ThreadLocal<SimpleDateFormat> () {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", new Locale("eng"));
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
    };
    
    private String id;
    private String msgName;
    private String msgNamespace;
    private String timestamp;
    private String from;
    private String to;
    private String content;
    private String json;
    private String metaDataID;
    private String rawMessage;
    private String printableMessage;
    private String rejectReason;

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getMsgName() {
        return msgName;
    }

    public void setMsgName(String msgName) {
        this.msgName = msgName;
    }

    public String getMsgNamespace() {
        return msgNamespace;
    }

    public void setMsgNamespace(String msgNamespace) {
        this.msgNamespace = msgNamespace;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getMetaDataID() {
        return metaDataID;
    }

    public void setMetaDataID(String metaDataID) {
        this.metaDataID = metaDataID;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public String getPrintableMessage() {
        return printableMessage;
    }

    public void setPrintableMessage(String printableMessage) {
        this.printableMessage = printableMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
