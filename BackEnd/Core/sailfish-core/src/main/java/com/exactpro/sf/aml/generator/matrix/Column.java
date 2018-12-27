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

/**
 * This enumeration represent a matrix columns.
 *
 * @author dmitry.guriev
 *
 */
public enum Column {

	Reference(Constants.SYSTEM_PREFIX.concat("reference"), "action reference"),
	ReferenceToFilter(Constants.SYSTEM_PREFIX.concat("reference_to_filter"), "reference to filter message"),
	Template(Constants.SYSTEM_PREFIX.concat("template"), "Template message"),
	ServiceName(Constants.SYSTEM_PREFIX.concat("service_name"), "service name"),
	Action(Constants.SYSTEM_PREFIX.concat("action"), "action name"),
	Execute(Constants.SYSTEM_PREFIX.concat("execute"), "Y or N to control from matrix whether this action to be executed"),
	Timeout(Constants.SYSTEM_PREFIX.concat("timeout"), "wait action timeout"),
	Id(Constants.SYSTEM_PREFIX.concat("id"), "action id"),
	MessageType(Constants.SYSTEM_PREFIX.concat("message_type"), "message type"),
	CheckPoint(Constants.SYSTEM_PREFIX.concat("check_point"), "action checkpoint"),
	Description(Constants.SYSTEM_PREFIX.concat("description"), "some comments about action"),
	Dictionary(Constants.SYSTEM_PREFIX.concat("dictionary"), "dictionary name"),
	AddToReport(Constants.SYSTEM_PREFIX.concat("add_to_report"), "Y or N to control from matrix whether this action to be added to report"),
	MessageCount(Constants.SYSTEM_PREFIX.concat("messages_count"), "Expected message count. Used to check how many messages received after specified checkpoint."),
	DoublePrecision(Constants.SYSTEM_PREFIX.concat("double_precision"), "precision of double and float comparison"),
	StaticType(Constants.SYSTEM_PREFIX.concat("static_type"), "type of static variable"),
	StaticValue(Constants.SYSTEM_PREFIX.concat("static_value"), "value of static variable"),
	ContinueOnFailed(Constants.SYSTEM_PREFIX.concat("continue_on_failed"), "Y or N to from matrix should script continue execution when error occurred or break test"),
	BreakPoint(Constants.SYSTEM_PREFIX.concat("break_point"), "Y or N (default) to set break point before execute current action"),
	SystemPrecision(Constants.SYSTEM_PREFIX.concat("system_precision"), "precision of double and float comparison"),
	FailUnexpected(Constants.SYSTEM_PREFIX.concat("fail_unexpected"), "fail all unexpected fields in message"),
	Outcome(Constants.SYSTEM_PREFIX.concat("outcome"), "group verifications by outcome parameter"),
	FailOnUnexpectedMessage(Constants.SYSTEM_PREFIX.concat("fail_on_unexpected_message"), "TC should fail if unexpected by wait actions message received"),
	CheckGroupsOrder(Constants.SYSTEM_PREFIX.concat("check_groups_order"), "Check order of group when comparing"),
	Condition(Constants.SYSTEM_PREFIX.concat("condition"), "Used for conditional operator"),
	ReorderGroups(Constants.SYSTEM_PREFIX.concat("reorder_groups"), "If Y then repeating groups in received message will be reordered according to message filter"),
	IsStatic(Constants.SYSTEM_PREFIX.concat("is_static"), "If Y then action will be considered static and will be accessible in following test cases"),
	Comment(Constants.SYSTEM_PREFIX.concat("comment"), "If Y then action will be ignored"),
    Tag(Constants.SYSTEM_PREFIX.concat("tag"), "action tag for statistics"),
    Dependencies(Constants.SYSTEM_PREFIX.concat("dependencies"), "list of references on which this action depends"),
    VerificationsOrder(Constants.SYSTEM_PREFIX.concat("verifications_order"), "Field order for sorting verifications in report"),
    _no_columns_required(null, "Used only for annotations.");

	public String getHelpString() {
		return helpString;
	}

	private String column;
	private String helpString;

	private Column(String colName, String helpString) {
		this.column = colName;
		this.helpString = helpString;
	}

	public String getName() {
		return this.column;
	}

	public static Column value(String key) {

		for (Column c : Column.values()) {
			if (c.getName() != null && c.getName().equals(key)) {
				return c;
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

	private final static class Constants {
	    public static final String SYSTEM_PREFIX = "#";
	    private static final String IGNORED_PREFIX = "~";
	}
}
