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
package com.exactpro.sf;

// ThisIsTheMostRidiculouslyLongClassNameIHaveEverWritten

public class TestScriptRunAggregateReportQueryBuilder {

	private String query;
	
	private TestScriptRunAggregateReportQueryBuilder() {
		
	}
	
	public TestScriptRunAggregateReportQueryBuilder type(Type value) {
		query += (query.isEmpty() ? "type=" : "&type=") + value.toString();
		return this;
	}
	
	public TestScriptRunAggregateReportQueryBuilder startDate(String value) {
		query += (query.isEmpty() ? "startDate=" : "&startDate=") + value;
		return this;
	}
	
	public TestScriptRunAggregateReportQueryBuilder endDate(String value) {
		query += (query.isEmpty() ? "endDate=" : "&endDate=") + value;
		return this;
	}
	
	public TestScriptRunAggregateReportQueryBuilder details(boolean value) {
		query += (query.isEmpty() ? "details=" : "&details=") + String.valueOf(value);
		return this;
	}
	
	public TestScriptRunAggregateReportQueryBuilder duration(Duration value) {
		query += (query.isEmpty() ? "duration=" : "&duration=") + value.toString();
		return this;
	}
	
	public String build() {
		return query;
	}
	
	public static enum Type {
		BASE,
		SEND_DATA,
		FAIL_REASON,
		ETM
	}
	
	public static enum Duration {
		TODAY,
		WEEK,
		MONTH,
		CUSTOM
	}
	
}
