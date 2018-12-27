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
package com.exactpro.sf.aml.iomatrix;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimpleCellUtils {

	public static String[] dropStyles(SimpleCell[] line) {
		String[] result = new String[line.length];
		for (int i = 0; i < line.length; i++) {
			result[i] = line[i].getValue();
		}
		return result;
	}

	public static SimpleCell[] addStyles(String[] line) {
		SimpleCell[] result = new SimpleCell[line.length];
		for (int i = 0; i < line.length; i++) {
			result[i] = new SimpleCell(line[i]);
		}
		return result;
	}
	
	public static List<String> dropStyles(List<SimpleCell> line) {
		List<String> result = new ArrayList<>();
		for (SimpleCell cell : line) {
			result.add(cell.getValue());
		}
		return result;
	}
	
	public static List<SimpleCell> addStyles(List<String> line) {
		List<SimpleCell> result = new ArrayList<>();
		for (String value : line) {
			result.add(new SimpleCell(value));
		}
		return result;
	}
	
	public static Map<String, String> dropStyles(Map<String, SimpleCell> line) {
		Map<String, String> result = new LinkedHashMap<>();
		for (Map.Entry<String, SimpleCell> entry : line.entrySet()) {
			result.put(entry.getKey(), entry.getValue().getValue());
		}
		return result;
	}
	
	public static Map<String, SimpleCell> addStyles(Map<String, String> line) {
		Map<String, SimpleCell> result = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : line.entrySet()) {
			result.put(entry.getKey(), new SimpleCell(entry.getValue()));
		}
		return result;
	}
}
