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
package com.exactpro.sf.comparison;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.google.common.collect.ImmutableSet;

public class ComparatorSettings {
    private IPostValidation postValidation;
    @Deprecated
    private String alternateValue;
    private MetaContainer metaContainer = new MetaContainer();
    private boolean checkGroupsOrder = false;
    @Deprecated
    private Map<String, Boolean> negativeMap = Collections.emptyMap();
    private boolean reorderGroups = false;
    private IDictionaryStructure dictionaryStructure;
    private Set<String> uncheckedFields = Collections.emptySet();

    public IPostValidation getPostValidation() {
        return postValidation;
    }

    public ComparatorSettings setPostValidation(IPostValidation postValidation) {
        this.postValidation = postValidation;
        return this;
    }

    @Deprecated
    public String getAlternateValue() {
        return alternateValue;
    }

    @Deprecated
    public ComparatorSettings setAlternateValue(String alternateValue) {
        this.alternateValue = alternateValue;
        return this;
    }

    public MetaContainer getMetaContainer() {
        return metaContainer;
    }

    public ComparatorSettings setMetaContainer(MetaContainer metaContainer) {
        if(metaContainer != null) {
            this.metaContainer = metaContainer;
        }

        return this;
    }

    public boolean isCheckGroupsOrder() {
        return checkGroupsOrder;
    }

    public ComparatorSettings setCheckGroupsOrder(boolean checkGroupsOrder) {
        this.checkGroupsOrder = checkGroupsOrder;
        return this;
    }

    @Deprecated
    public Map<String, Boolean> getNegativeMap() {
        return negativeMap;
    }

    @Deprecated
    public ComparatorSettings setNegativeMap(Map<String, Boolean> negativeMap) {
        if(negativeMap != null) {
            this.negativeMap = Collections.unmodifiableMap(negativeMap);
        }

        return this;
    }

    public boolean isReorderGroups() {
        return reorderGroups;
    }

    public ComparatorSettings setReorderGroups(boolean reorderGroups) {
        this.reorderGroups = reorderGroups;
        return this;
    }

    public IDictionaryStructure getDictionaryStructure() {
        return dictionaryStructure;
    }

    public ComparatorSettings setDictionaryStructure(IDictionaryStructure dictionaryStructure) {
        this.dictionaryStructure = Objects.requireNonNull(dictionaryStructure, "dictionaryStructure cannot be null");
        return this;
    }

    public Set<String> getUncheckedFields() {
        return uncheckedFields;
    }

    public ComparatorSettings setUncheckedFields(Set<String> uncheckedFields) {
        if(uncheckedFields != null) {
            this.uncheckedFields = ImmutableSet.copyOf(uncheckedFields);
        }

        return this;
    }
}
