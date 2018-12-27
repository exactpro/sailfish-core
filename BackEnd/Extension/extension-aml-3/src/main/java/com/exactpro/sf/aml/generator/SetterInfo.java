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
package com.exactpro.sf.aml.generator;

class SetterInfo {
    private final String column;
    private final String code;
    private final String value;
    private final long line;
    private final long uid;
    private final String reference;

    public SetterInfo(String column, String code, String value, long line, long uid, String reference) {
        this.column = column;
        this.code = code;
        this.value = value;
        this.line = line;
        this.uid = uid;
        this.reference = reference;
    }

    public String getColumn() {
        return column;
    }

    public String getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public long getLine() {
        return line;
    }

    public long getUID() {
        return uid;
    }

    public String getReference() {
        return reference;
    }
}