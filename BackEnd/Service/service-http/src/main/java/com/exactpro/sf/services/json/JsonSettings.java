/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.json;

import org.apache.commons.configuration2.HierarchicalConfiguration;

import com.exactpro.sf.common.util.ICommonSettings;
import org.apache.commons.configuration2.tree.ImmutableNode;

public class JsonSettings implements ICommonSettings {
    private boolean rejectUnexpectedFields;
    private boolean treatSimpleValuesAsStrings;

    public boolean isRejectUnexpectedFields() {
        return rejectUnexpectedFields;
    }

    public JsonSettings setRejectUnexpectedFields(boolean rejectUnexpectedFields) {
        this.rejectUnexpectedFields = rejectUnexpectedFields;
        return this;
    }

    public boolean isTreatSimpleValuesAsStrings() {
        return treatSimpleValuesAsStrings;
    }

    public JsonSettings setTreatSimpleValuesAsStrings(boolean treatSimpleValuesAsStrings) {
        this.treatSimpleValuesAsStrings = treatSimpleValuesAsStrings;
        return this;
    }

    @Override
    public void load(HierarchicalConfiguration<ImmutableNode> config) {

    }
}
