/******************************************************************************
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
package com.exactpro.sf.scriptrunner.impl.jsonreport;

import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Action;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Bug;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.CustomLink;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.CustomMessage;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.CustomTable;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.LogEntry;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Message;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportException;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCase;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Verification;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Arrays;
import java.util.Collection;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "actionNodeType")
@JsonSubTypes({
        @Type(value = Action.class, name = "action"),
        @Type(value = CustomMessage.class, name = "customMessage"),
        @Type(value = Verification.class, name = "verification"),
        @Type(value = Bug.class, name = "bug"),
        @Type(value = CustomLink.class, name = "link"),
        @Type(value = CustomTable.class, name = "table"),
        @Type(value = LogEntry.class, name = "logEntry"),
        @Type(value = Message.class, name = "message"),
        @Type(value = ReportException.class, name = "reportException"),
        @Type(value = CustomTable.class, name = "table"),
        @Type(value = TestCase.class, name = "testCase")
})
public interface IJsonReportNode {
    default void addSubNodes(IJsonReportNode... nodes) {
        addSubNodes(Arrays.asList(nodes));
    }

    default void addSubNodes(Collection<? extends IJsonReportNode> nodes) {
        throw new IllegalArgumentException("not supported.");
    }

    default void addException(Throwable t) {
        throw new IllegalArgumentException("not supported.");
    }
}