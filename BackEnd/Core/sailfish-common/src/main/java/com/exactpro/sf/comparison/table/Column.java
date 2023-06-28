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

public class Column {

    private final String cell;
	private int offset;

	public Column(String s) {
		this(s, 0);
	}

	public Column(String s, int offset) {
		this.cell = s;
		this.offset = offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getLength() {
		return toString().length();
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		for (int i=0; i<offset; i++) {
			sb.append(" ");
		}

		if (cell != null) {
            sb.append(cell);
		}
		return sb.toString();
	}

	public String getValue() {
        return cell;
	}

}
