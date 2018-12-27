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
package com.exactpro.sf.testwebgui.restapi.json.dictionary;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class JsonUtilFunction {

	public static class Parameter {
		private final String parameter; // reserverd for future use
		private final String type;

		public Parameter(String parameter, String type) {
			super();
			this.parameter = parameter;
			this.type = type;
		}

		public String getParameter() {
			return parameter;
		}

		public String getType() {
			return type;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Parameter [parameter=");
			builder.append(parameter);
			builder.append(", type=");
			builder.append(type);
			builder.append("]");
			return builder.toString();
		}

    }

    private final String name;

    @JsonInclude(Include.NON_NULL)
    private final String help;

    @JsonInclude(Include.NON_EMPTY)
    private final Collection<Parameter> parameters;

    private final String returnType;

	public JsonUtilFunction(String name, String help, Collection<Parameter> parameters, String returnType) {
		super();
		this.name = name;
		this.help = help;
		this.parameters = parameters;
		this.returnType = returnType;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return help;
	}

	public Collection<Parameter> getParameters() {
		return parameters;
	}

	public String getReturnType() {
		return returnType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonUtilFunction [name=");
		builder.append(name);
		builder.append(", description=");
		builder.append(help);
		builder.append(", parameters=");
		builder.append(parameters);
		builder.append(", returnType=");
		builder.append(returnType);
		builder.append("]");
		return builder.toString();
	}

}
