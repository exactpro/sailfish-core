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
package com.exactpro.sf.aml.generator.matrix;

import com.exactpro.sf.configuration.suri.SailfishURI;

public enum JavaStatement {

	BEGIN_LOOP("REPEAT", Column.MessageCount),
	END_LOOP("NEXT"),
	SET_STATIC("SetStatic", Column.Reference, Column.StaticType, Column.StaticValue),
	DEFINE_SERVICE_NAME("DefineServiceName", Column.Reference, Column.ServiceName),
	DEFINE_HEADER("DefineHeader"),
	BEGIN_IF("IF", Column.Condition),
	BEGIN_ELIF("ELIF", Column.Condition),
	BEGIN_ELSE("ELSE"),
	END_IF("ENDIF"),
	INCLUDE_BLOCK("IncludeBlock", Column.Template);

	private String value;
	private Column[] requiredColumns;
	private SailfishURI uri;

	private JavaStatement(String s, Column... requiredColumns) {
		this.value = s;
		this.requiredColumns = requiredColumns;
		this.uri = SailfishURI.unsafeParse(s);
	}

	public String getValue() {
		return this.value;
	}

	public Column[] getRequiredColumns() {
	    return this.requiredColumns;
	}

	public SailfishURI getURI() {
	    return this.uri;
	}

	public static JavaStatement value(String s) {
	    if(s == null) {
	        return null;
	    }

		for (JavaStatement e : JavaStatement.values()) {
			if (e.value.equalsIgnoreCase(s))
				return e;
		}

		return null;
	}

	public static JavaStatement value(SailfishURI uri) {
	    for(JavaStatement e : JavaStatement.values()) {
	        if(e.getURI().equals(uri)) {
	            return e;
	        }
	    }

		return null;
	}

	@Override
	public String toString() {
	    return this.value;
	}
}
