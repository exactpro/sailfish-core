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
package com.exactpro.sf.configuration.factory;

import com.exactpro.sf.common.impl.messages.HumanMessage;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.services.fix.FixMessageHelper;

public class FIXHumanMessage extends HumanMessage {
    @Override
    protected void appendName(String fieldName, IFieldStructure fieldStructure) {
        super.appendName(fieldName, fieldStructure);
        if (fieldStructure != null) {
            Object tag = fieldStructure.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_TAG);
            if (tag != null) {
                builder.append('(').append(tag).append(')');
            }
        }
    }
}
