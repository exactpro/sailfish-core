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

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author dmitry.guriev
 *
 */
public class Table
{
	private static final String EOL = System.getProperty("line.separator");

	public static final int COLUMN_COUNT = 5;

	private static final Row headerRow = new Row(0, "Tag", "Expected", "Actual", "Result", "Comment");
	public static final Row DELIMITER_ROW = new Row(0, null, null, null, null, null);
	private Row header;
    private final List<Row> rows;
    private int offset;
	private String title;

	public Table()
	{
		this.rows = new ArrayList<Row>();
		setHeader(headerRow);
	}

	public Table (String title)
	{
		this();
		setTitle(title);
	}

	public Table (int offset)
	{
		this();
		setOffset(offset);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setHeader(Row row) {
		this.header = row;
	}

	public void setOffset(int offset) {
		this.offset = offset;
		for (Row row : rows) {
			row.setOffset(offset);
		}
	}

	public int getOffset() {
        return offset;
	}

	public int getMaxColumnLength(int iColumn)
	{
		int length = 0;

		for (Row r : rows) {
			length = Math.max(length, r.getColumnLength(iColumn));
		}
		return length;
	}

	public void add(Table that)
	{
        if(!that.rows.isEmpty()) {
//			rows.add(new Row());
			rows.addAll(that.rows);
		}
	}

	public void add(Row row)
	{
        rows.add(row);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

        int[] colWidth = new int[COLUMN_COUNT];

		int tableWidth = 0;
		for (int i=0; i<colWidth.length; i++) {
			int headerLength = 0;
            if(header != null) {
                headerLength = header.getColumnLength(i);
			}
			colWidth[i] = Math.max(headerLength, getMaxColumnLength(i));
			tableWidth += colWidth[i];
		}

        if(title != null) {
			sb.append(new Row().toString(colWidth)).append(EOL);
			sb.append("| ");
            sb.append(title);
            for(int i = title.length(); i < tableWidth + COLUMN_COUNT - 2; i++) {
				sb.append(" ");
			}
			sb.append("|");
			sb.append(EOL);
		}

        if(header != null) {
			sb.append(new Row().toString(colWidth)).append(EOL);
			sb.append(header.toString(colWidth)).append(EOL);
		}
		sb.append(new Row().toString(colWidth)).append(EOL);
        for(Row row : rows) {
			sb.append(row.toString(colWidth)).append(EOL);
		}

		return sb.toString();
	}

	public List<Row> getRows() {
        return rows;
	}
}
