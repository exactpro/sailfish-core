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

import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.testwebgui.restapi.json.JsonAMLError;
import com.exactpro.sf.testwebgui.restapi.json.WithJsonAMLError;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({ "key", "metadata" })
public class JsonMatrixTestCase implements WithJsonAMLError {

	private final Map<String, String> values;

	private final List<JsonMatrixLine> items;
	
	private final List<JsonAMLError> errors;

	public JsonMatrixTestCase(
			Map<String, String> values,
			List<JsonMatrixLine> items) {
		super();
		this.values = values;
		this.items = items == null ? Collections.<JsonMatrixLine>emptyList() : items;
		this.errors = Collections.emptyList();
	}

	@JsonCreator
	public JsonMatrixTestCase(
			@JsonProperty("values") Map<String, String> values,
			@JsonProperty(value="items", required=false) List<JsonMatrixLine> items,
			@JsonProperty(value="errors", required=false) List<JsonAMLError> errors) {
		super();
		this.values = values;
		this.items = items == null ? Collections.<JsonMatrixLine>emptyList() : items;
		this.errors = errors == null ? Collections.<JsonAMLError>emptyList() : errors;
	}

	public Map<String, String> getValues() {
		return values;
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
		builder.append("JsonMatrixTestCase [values=");
		builder.append(values);
		builder.append(", items=");
		builder.append(items);
		builder.append(", errors=");
		builder.append(errors);
		builder.append("]");
		return builder.toString();
	}

	public AMLBlock toAMLBlock(int lineNumber) {
		return new AMLBlock(lineNumber, stringToCell(), convertElements());
	}

    public AMLElement toAMLElement(int lineNumber) {
        return new AMLElement(lineNumber, stringToCell());
    }

	private Map<String, SimpleCell> stringToCell() {
		LinkedHashMap<String, SimpleCell> result = new LinkedHashMap<>();
		for (Entry<String, String> value: values.entrySet()) {
		    if (value.getValue() == null) {
		        // Bug #46785: JSMatrixEditor > Failed to copy block
		        // skip `null` values in TestCase header
		        continue;
		    }
			result.put(value.getKey(), new SimpleCell(value.getValue()));
		}
		return result;
	}
	
	private List<AMLElement> convertElements() {
		List<AMLElement> result = new ArrayList<>();
		for (int i =0; i< items.size(); i++) {
			result.add(items.get(i).toAMLElement(i));
		}
		return result;
	}

}
