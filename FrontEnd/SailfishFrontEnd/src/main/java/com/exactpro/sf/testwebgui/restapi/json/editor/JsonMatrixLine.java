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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.testwebgui.restapi.JsonAMLUtil;
import com.exactpro.sf.testwebgui.restapi.json.JsonAMLError;
import com.exactpro.sf.testwebgui.restapi.json.WithJsonAMLError;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"key"})
public class JsonMatrixLine implements WithJsonAMLError {

	// special optional value. It allows to group actions to sets
	@JsonInclude(value =Include.NON_NULL)
	private int setId;
	private final Map<String, String> values;
	private final Map<String, String> metadata;
	private final List<JsonAMLError> errors;
	private final List<JsonMatrixLine> items;

	@Deprecated // don't use it in your code // only for Jackson
	@JsonCreator
	public JsonMatrixLine(@JsonProperty("values") Map<String, String> values,
			@JsonProperty(value = "items", required = false) List<JsonMatrixLine> items) {
		super();
		this.values = values;
		this.items = items;
		this.metadata = Collections.emptyMap();
		this.errors = Collections.emptyList();
	}

	public JsonMatrixLine(Map<String, String> values, Map<String, String> metadata, List<JsonMatrixLine> items) {
		this(values, metadata, new ArrayList<JsonAMLError>(), items);
	}
	
	public JsonMatrixLine(Map<String, String> values, Map<String, String> metadata, List<JsonAMLError> errors, List<JsonMatrixLine> items) {
		super();
		this.values = values;
		this.metadata = metadata;
		this.errors = errors;
		this.items = items;
	}

	public int getSetId() {
		return setId;
	}

	public void setSetId(int setId) {
		this.setId = setId;
	}

	public Map<String, String> getValues() {
		return values;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}
	
	public List<JsonMatrixLine> getItems() {
		return items;
	}

	@Override
	public List<JsonAMLError> getErrors() {
		return errors;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonMatrixLine [setId=");
		builder.append(setId);
		builder.append(", values=");
		builder.append(values);
		builder.append(", metadata=");
		builder.append(metadata);
		builder.append(", errors=");
		builder.append(errors);
		builder.append("]");
		return builder.toString();
	}
	
	public AMLElement toAMLElement(int lineNumber) {
		AMLElement result;
		JavaStatement action = JavaStatement.value(values.get(Column.Action.getName()));
		if (JsonAMLUtil.isBlockAction(action)) {
			List<AMLElement> internalActions = new ArrayList<AMLElement>();
			if (items != null) {
				for (JsonMatrixLine internalLine: items) {
					internalActions.add(internalLine.toAMLElement(lineNumber));
				}
			}
			result = new AMLBlock(lineNumber, stringToCell(), internalActions);
		} else {
			result = new AMLElement(lineNumber, stringToCell());
		}
		return result;
	}

	private Map<String, SimpleCell> stringToCell() {
		LinkedHashMap<String, SimpleCell> result = new LinkedHashMap<>();
		for (Entry<String, String> value: values.entrySet()) {
			result.put(value.getKey(), new SimpleCell(value.getValue()));
		}
		return result;
	}

}
