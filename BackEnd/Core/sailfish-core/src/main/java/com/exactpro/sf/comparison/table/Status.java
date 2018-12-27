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
package com.exactpro.sf.comparison.table;

public enum Status {

	OK("OK"),
	FAIL("FAIL"),
	NOT_TESTED("");

	private String status;

	private Status(String status) {
		this.status = status;
	}
	
	public String getString() {
		return this.status;
	}

	public static String compare(Object actual, Object expected) {
		if (expected == null) {
			return (actual == null) ? OK.getString() : NOT_TESTED.getString();
		}
		if (actual == null) {
			return FAIL.getString();
		}
		return actual.equals(expected) ? OK.getString() : FAIL.getString();
	}

}
