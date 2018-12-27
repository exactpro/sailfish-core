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

import java.util.List;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.Convention;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;

@MatrixUtils
@ResourceAliases({"MessageUtil"})
public class MessageUtil extends AbstractCaller {

    @Description("Finds a message within the list, matching the specified field.<br/>" +
            "<b>fieldName</b> - the field name for the matching group.<br/>" +
            "<b>fieldValue</b> - the field value for the matching group.<br/>" +
            "<b>return</b> - a message matched by the field name and value.<br/>" +
            "Considering the above said, the final syntax is:<br/>" +
            "#{FindGroup(groups, fieldName, fieldValue)}")
    @UtilityMethod
    public IMessage FindGroup(List<IMessage> groups, String fieldName, Object fieldValue) {
        if (groups != null && !groups.isEmpty()) {
            fieldName = fieldName != null ? fieldName.trim() : null;
            if (fieldName != null && !fieldName.isEmpty()) {
                for (IMessage msg : groups) {
                    if (msg.isFieldSet(fieldName) && msg.getField(fieldName).equals(fieldValue)
                            || !msg.isFieldSet(fieldName) && fieldValue == null) {
                        return msg;
                    }
                }
                throw new EPSCommonException("Group with " + fieldName + " == " + fieldValue + " not found");
            }
            throw new EPSCommonException("Field name is null or empty");
        }
        throw new EPSCommonException("List groups is null or empty");
    }

    @Description("Finds the value of the target field for a message within the list, matching the specified field.<br/>" +
            "<b>fieldName</b> - the field name for the matching group.<br/>" +
            "<b>fieldValue</b> - the field value for the matching group.<br/>" +
            "<b>targetFieldName</b> – the target field name." +
            "<b>return</b> - the target field value.<br/>" +
            "Considering the above said, the final syntax is:<br/>" +
            "#{FindField(groups, fieldName, fieldValue, targetFieldName)}")
    @UtilityMethod
    public Object FindField(List<IMessage> groups, String fieldName, Object fieldValue, String targetFieldName) {
        IMessage msg = FindGroup(groups, fieldName, fieldValue);
        targetFieldName = targetFieldName != null ? targetFieldName.trim() : null;
        if (msg.isFieldSet(targetFieldName)) {
            return msg.getField(targetFieldName);
        } else {
            return null;
        }
    }

    @Description("Returns the special object representing any value.<br/>" +
            "This value can be used instead of '*' and referred to from other filters.<br/>" +
            "Example:<br/>" +
            "#{Any()} returns \"ANY\"")
    @UtilityMethod
    public Object Any() {
        return Convention.CONV_PRESENT_OBJECT;
    }

    @Description("Returns the special object representing an empty value.<br/>" +
            "This value can be used instead of '#' and referred to from other filters.<br/>"+
            "Example:<br/>" +
            "#{Empty()} returns \"EMPTY\"")
    @UtilityMethod
    public Object Empty() {
        return Convention.CONV_MISSED_OBJECT;
    }

    @Description("Returns the regex filter object for specified pattern.<br/>" +
            "Example: #{Regex(\".*\")}")
    @UtilityMethod
    public IFilter Regex(String pattern) {
        return StaticUtil.regexFilter(0, "", pattern);
    }
}
