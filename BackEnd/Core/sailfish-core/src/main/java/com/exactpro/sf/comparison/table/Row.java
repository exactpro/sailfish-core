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



public class Row {

	private Column[] columns;

	private int offset;

	public Row()
	{
		this(0, null, null, null, null);
	}

	public Row(int offset, String name, String actual, String expected, String result)
	{
		this.offset = offset;
		columns = new Column[Table.COLUMN_COUNT];
		columns[0] = new Column(name, offset);
		columns[1] = new Column(actual);
		columns[2] = new Column(expected);
		columns[3] = new Column(result);
	}

	public int getColumnLength(int iColumn) {
		return columns[iColumn].getLength();
	}

	public boolean isDelimiter()
	{
		int length = 0;
		for (Column c : columns) {
			length += c.getLength();
		}
		return length == this.offset;
	}

	public String toString(int[] length)
	{
		StringBuilder sb = new StringBuilder();
		if (isDelimiter()) {
			sb.append("+");
			for (int len : length)
			{
				for (int i=0; i< len; i++) {
					sb.append("-");
				}
				sb.append("+");
			}
			return sb.toString();
		}

		sb.append("|");
		for (int ic = 0; ic < columns.length; ic++)
		{
			Column c = columns[ic];
			int len = length[ic];
			sb.append(c.toString());
			for (int i=c.getLength(); i<len; i++) {
				sb.append(" ");
			}
			sb.append("|");
		}
		return sb.toString();
	}

	public void setOffset(int offset) {
		this.offset = offset;
		columns[0].setOffset(offset);
	}

	public Column[] getColumns() {
		return this.columns;
	}

	public Column getColumn(int index) {
		return this.columns[index];
	}
}
