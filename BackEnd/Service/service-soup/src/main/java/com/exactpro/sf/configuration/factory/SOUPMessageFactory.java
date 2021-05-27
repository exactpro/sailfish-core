/******************************************************************************
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
 ******************************************************************************/
package com.exactpro.sf.configuration.factory;

import com.exactpro.sf.common.impl.messages.AbstractMessageFactory;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class SOUPMessageFactory extends AbstractMessageFactory {

    public static final Set<String> UNCHECKED_FIELDS = ImmutableSet.of(
            "UnsequencedDataPacketHeader",
            "SequencedDataPacketHeader"
    );

    @Override
    public String getProtocol() {
        return "SOUP";
    }

    @Override
    public Set<String> getUncheckedFields() {
        return UNCHECKED_FIELDS;
    }
}
