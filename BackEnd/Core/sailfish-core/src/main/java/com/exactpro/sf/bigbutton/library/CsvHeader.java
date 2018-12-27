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
package com.exactpro.sf.bigbutton.library;

public enum CsvHeader {

	Item("item"),
	Name("name"),
	Path("path"),
	Daemon("daemon"),
	Timeout("timeout"),
	Executor("executor"),
	Services("services"),
	Priority("priority"),
	AutoStart("autostart"),
	ContinueIfFailed("continue_if_failed"),
    RunNetDumper("run_netdumper"),
    SkipOptional("skip_optional"),
	Range("range"),
	Tags("tags"),
	Language("language"),
	StaticVariables("static_variables"),
	Group("group"),
	StartMode("start_mode"),
	IgnoreAskForContinue("ignore_ask_for_continue"),
    Ignore("ignore"),
    ExecuteOnPassed("execute_on_passed"),
    ExecuteOnConditionallyPassed("execute_on_conditionally_passed"),
	ExecuteOnFailed("execute_on_failed");

	private final String fieldKey;

	private CsvHeader(String fieldKey) {
		this.fieldKey = fieldKey;
	}

	public String getFieldKey() {
		return fieldKey;
	}

}
