/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fix.converter;

import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;

import java.util.Objects;

public class QFJIMessageConverterSettings {
    private final IDictionaryStructure dictionary;
    private final IMessageFactory factory;
    private boolean verifyTags;
    private boolean includeMilliseconds;
    private boolean includeMicroseconds;
    private boolean includeNanoseconds;
    private boolean skipTags;
    private boolean orderingFields;

    public QFJIMessageConverterSettings(IDictionaryStructure dictionary, IMessageFactory factory){
        this.dictionary = Objects.requireNonNull(dictionary, "Dictionary can't be null");
        this.factory = Objects.requireNonNull(factory, "Factory can't be null");
    }

    public QFJIMessageConverterSettings setVerifyTags(boolean verifyTags){
        this.verifyTags = verifyTags;
        return this;
    }

    public QFJIMessageConverterSettings setIncludeMilliseconds(boolean includeMilliseconds){
        this.includeMilliseconds = includeMilliseconds;
        return this;
    }

    public QFJIMessageConverterSettings setIncludeMicroseconds(boolean includeMicroseconds){
        this.includeMicroseconds = includeMicroseconds;
        return this;
    }

    public QFJIMessageConverterSettings setIncludeNanoseconds(boolean includeNanoseconds) {
        this.includeNanoseconds = includeNanoseconds;
        return this;
    }

    public QFJIMessageConverterSettings setSkipTags(boolean skipTags){
        this.skipTags = skipTags;
        return this;
    }

    public QFJIMessageConverterSettings setOrderingFields(boolean orderingFields){
        this.orderingFields = orderingFields;
        return this;
    }

    public IDictionaryStructure getDictionary() {
        return dictionary;
    }

    public IMessageFactory getFactory() {
        return factory;
    }

    public boolean isVerifyTags(){
        return verifyTags;
    }

    public boolean isIncludeMicroseconds() {
        return includeMicroseconds;
    }

    public boolean isIncludeMilliseconds() {
        return includeMilliseconds;
    }

    public boolean isIncludeNanoseconds() {
        return includeNanoseconds;
    }

    public boolean isSkipTags() {
        return skipTags;
    }

    public boolean isOrderingFields() {
        return orderingFields;
    }
}
