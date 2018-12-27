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
package com.exactpro.sf.testwebgui.restapi.json.editor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonUpdateTransactionSpliceEntry {

	private final List<Integer> path;

	private final int start;

	private final int deleteCount;

	private final boolean shallowReplace;
	
	// JsonMatrixTestCase very similar to JsonMatrixLine
	private final List<JsonMatrixTestCase> data;
	
	
	@JsonCreator
	public JsonUpdateTransactionSpliceEntry(
			@JsonProperty("path") List<Integer> path,
			@JsonProperty("start") int start,
			@JsonProperty("deleteCount") int deleteCount,
			@JsonProperty("shallowReplace") boolean shallowReplace,
			@JsonProperty("data") List<JsonMatrixTestCase> data) {
		super();
		this.path = path;
		this.start = start;
		this.deleteCount = deleteCount;
		this.shallowReplace = shallowReplace;
		this.data = data;
	}

	public List<Integer> getPath() {
		return path;
	}

	public int getStart() {
		return start;
	}

	public int getDeleteCount() {
		return deleteCount;
	}

	public boolean isShallowReplace() {
		return shallowReplace;
	}

	public List<JsonMatrixTestCase> getData() {
		return data;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonUpdateTransactionSpliceEntry [path=");
		builder.append(path);
		builder.append(", start=");
		builder.append(start);
		builder.append(", deleteCount=");
		builder.append(deleteCount);
		builder.append(", shallowReplace=");
		builder.append(shallowReplace);
		builder.append(", data=");
		builder.append(data);
		builder.append("]");
		return builder.toString();
	}

}
