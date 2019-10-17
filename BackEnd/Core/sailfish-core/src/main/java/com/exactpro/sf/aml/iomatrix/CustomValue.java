/*
 *  Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.sf.aml.iomatrix;

import java.util.List;
import java.util.Map;

abstract class CustomValue {
    private boolean array;
    private boolean object ;
    private boolean simple;
    private final int line;
    private final int column;

    private List<CustomValue> arrayValue;
    private Map<KeyValue, CustomValue> objectValue;
    private Object simpleValue;

    public CustomValue(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public boolean isObject() {
        return object;
    }

    public boolean isArray() {
        return array;
    }

    public boolean isSimple() {
        return simple;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public void setArrayValue(List<CustomValue> arrayValue) {
        this.arrayValue = arrayValue;
        array = true;
        object = false;
        simple = false;
    }

    public void setObjectValue(Map<KeyValue, CustomValue> objectValue) {
        this.objectValue = objectValue;
        array = false;
        object = true;
        simple = false;
    }

    public void setSimpleValue(Object simpleValue) {
        this.simpleValue = simpleValue;
        array = false;
        object = false;
        simple = true;
    }

    public List<CustomValue> getArrayValue() {
        return arrayValue;
    }

    public Map<KeyValue, CustomValue> getObjectValue() {
        return objectValue;
    }

    public Object getSimpleValue() {
        return simpleValue;
    }
}
