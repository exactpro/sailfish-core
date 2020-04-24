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
package com.exactpro.sf.aml.generator.matrix;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * This enumeration represent a matrix columns.
 *
 * @author dmitry.guriev
 *
 */
public enum Column {

    Reference(Constants.SYSTEM_PREFIX + "reference", "action reference"),
    ReferenceToFilter(Constants.SYSTEM_PREFIX + "reference_to_filter", "reference to filter message"),
    Template(Constants.SYSTEM_PREFIX + "template", "Template message"),
    ServiceName(Constants.SYSTEM_PREFIX + "service_name", "service name"),
    Action(Constants.SYSTEM_PREFIX + "action", "action name"),
    Execute(Constants.SYSTEM_PREFIX + "execute", "Y or N to control from matrix whether this action to be executed"),
    Timeout(Constants.SYSTEM_PREFIX + "timeout", "wait action timeout"),
    Id(Constants.SYSTEM_PREFIX + "id", "action id"),
    MessageType(Constants.SYSTEM_PREFIX + "message_type", "message type"),
    CheckPoint(Constants.SYSTEM_PREFIX + "check_point", "action checkpoint"),
    Description(Constants.SYSTEM_PREFIX + "description", "some comments about action"),
    Dictionary(Constants.SYSTEM_PREFIX + "dictionary", "dictionary name"),
    AddToReport(Constants.SYSTEM_PREFIX + "add_to_report", "Y or N to control from matrix whether this action to be added to report"),
    MessageCount(Constants.SYSTEM_PREFIX + "messages_count", "Expected message count. Used to check how many messages received after specified checkpoint."),
    DoublePrecision(Constants.SYSTEM_PREFIX + "double_precision", "precision of double and float comparison"),
    StaticType(Constants.SYSTEM_PREFIX + "static_type", "type of static variable"),
    StaticValue(Constants.SYSTEM_PREFIX + "static_value", "value of static variable"),
    ContinueOnFailed(Constants.SYSTEM_PREFIX + "continue_on_failed", "Y or N to from matrix should script continue execution when error occurred or break test"),
    BreakPoint(Constants.SYSTEM_PREFIX + "break_point", "Y or N (default) to set break point before execute current action"),
    SystemPrecision(Constants.SYSTEM_PREFIX + "system_precision", "precision of double and float comparison"),
    FailUnexpected(Constants.SYSTEM_PREFIX + "fail_unexpected", "fail all unexpected fields in message"),
    Outcome(Constants.SYSTEM_PREFIX + "outcome", "group verifications by outcome parameter"),
    FailOnUnexpectedMessage(Constants.SYSTEM_PREFIX + "fail_on_unexpected_message", "TC should fail if unexpected by wait actions message received"),
    CheckGroupsOrder(Constants.SYSTEM_PREFIX + "check_groups_order", "Check order of group when comparing"),
    Condition(Constants.SYSTEM_PREFIX + "condition", "Used for conditional operator"),
    ReorderGroups(Constants.SYSTEM_PREFIX + "reorder_groups", "If Y then repeating groups in received message will be reordered according to message filter"),
    IsStatic(Constants.SYSTEM_PREFIX + "is_static", "If Y then action will be considered static and will be accessible in following test cases"),
    Comment(Constants.SYSTEM_PREFIX + "comment", "If Y then action will be ignored"),
    Tag(Constants.SYSTEM_PREFIX + "tag", "action tag for statistics"),
    Dependencies(Constants.SYSTEM_PREFIX + "dependencies", "list of references on which this action depends"),
    VerificationsOrder(Constants.SYSTEM_PREFIX + "verifications_order", "Field order for sorting verifications in report"),
    KeyFields(Constants.SYSTEM_PREFIX + "key_fields", "a set of fields which are required to match against a filter for a message to be checked completely");

    public String getHelpString() {
        return helpString;
    }

    private final String column;
    private final String helpString;

    Column(String colName, String helpString) {
        this.column = Objects.requireNonNull(StringUtils.stripToNull(colName), "Column name cannot be null or empty").toLowerCase();
        this.helpString = Objects.requireNonNull(StringUtils.stripToNull(helpString), "Help string cannot be null or empty");
    }

    public String getName() {
        return column;
	}

    /**
     * Returns Column by the provided name. If the name contains system prefix (#) case (upper or lower) would be ignored.
     * @param key column name
     * @return Column that matches the provided name
     */
	public static Column value(String key) {
        if ( key != null
                && key.startsWith(Constants.SYSTEM_PREFIX)) {

            key = key.toLowerCase();

            for (Column c : Column.values()) {
                if (key.equals(c.getName())) {
                    return c;
                }
            }
        }

		return null;
	}

	public static String getSystemPrefix() {
	    return Constants.SYSTEM_PREFIX;
	}

	public static String getIgnoredPrefix() {
	    return Constants.IGNORED_PREFIX;
	}

    private static final class Constants {
	    public static final String SYSTEM_PREFIX = "#";
	    private static final String IGNORED_PREFIX = "~";
	}
}
