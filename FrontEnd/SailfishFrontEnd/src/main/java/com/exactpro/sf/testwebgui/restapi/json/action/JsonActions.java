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
package com.exactpro.sf.testwebgui.restapi.json.action;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonUtilFunction;

public class JsonActions {

    private final Map<SailfishURI, JsonAction> actions;

	private final Map<String, JsonAction> statements;

    private final Map<SailfishURI, Set<JsonUtilFunction>> utils;

    public JsonActions(Map<SailfishURI, JsonAction> actions, Collection<JsonAction> statements,
                       Map<SailfishURI, Set<JsonUtilFunction>> utils) {

        this.actions = actions;

		this.statements = new LinkedHashMap<>();

		for (JsonAction statement : statements) {
			this.statements.put(statement.getName().toUpperCase(), statement);
		}

        this.utils = utils;
	}

	public Map<SailfishURI, JsonAction> getActions() {
		return actions;
	}

	public Map<String, JsonAction> getStatements() {
		return statements;
	}

	
	public Map<SailfishURI, Set<JsonUtilFunction>> getUtils() {
		return utils;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonActions [actions=");
		builder.append(actions);
		builder.append(", statements=");
		builder.append(statements);
		builder.append("]");
		return builder.toString();
	}

}
