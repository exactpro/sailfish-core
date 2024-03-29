/*
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.services.fix.converter.dirty;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.services.fix.converter.QFJIMessageConverterSettings;
import com.exactpro.sf.services.fix.converter.dirty.struct.RawMessage;

public class DirtyQFJIMessageConverterSettings extends QFJIMessageConverterSettings {
    /** Verify fields by dictionary dufing convertation from {@link IMessage} to {@link RawMessage}*/
    private boolean verifyFields;

    public DirtyQFJIMessageConverterSettings(IDictionaryStructure dictionary, IMessageFactory factory){
        super(dictionary, factory);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DirtyQFJIMessageConverterSettings setVerifyTags(boolean verifyTags){
        super.setVerifyTags(verifyTags);
        return this;
    }

    public DirtyQFJIMessageConverterSettings setVerifyFields(boolean verifyFields) {
        this.verifyFields = verifyFields;
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DirtyQFJIMessageConverterSettings setIncludeMilliseconds(boolean includeMilliseconds){
        super.setIncludeMilliseconds(includeMilliseconds);
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DirtyQFJIMessageConverterSettings setIncludeMicroseconds(boolean includeMicroseconds){
        super.setIncludeMicroseconds(includeMicroseconds);
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DirtyQFJIMessageConverterSettings setIncludeNanoseconds(boolean includeNanoseconds) {
        super.setIncludeNanoseconds(includeNanoseconds);
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DirtyQFJIMessageConverterSettings setSkipTags(boolean skipTags){
        super.setSkipTags(skipTags);
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DirtyQFJIMessageConverterSettings setOrderingFields(boolean orderingFields){
        super.setOrderingFields(orderingFields);
        return this;
    }

    public boolean isVerifyFields() {
        return verifyFields;
    }
}
