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
package com.exactpro.sf.comparison.conversion.impl;

import com.exactpro.sf.comparison.conversion.ConversionException;

public class CharacterConverter extends AbstractConverter<Character> {
    @Override
    public Character convert(Byte value) {
        return getValue(value);
    }

    @Override
    public Character convert(Short value) {
        return getValue(value);
    }

    @Override
    public Character convert(Integer value) {
        return getValue(value);
    }

    private Character getValue(Number value) {
        if(value == null) {
            return null;
        }

        int intValue = value.intValue();

        if(intValue < Character.MIN_VALUE || intValue > Character.MAX_VALUE) {
            throw new ConversionException(String.format("Cannot convert from %s to %s because value is out range - value: %s, range: [%s..%s]", value.getClass().getSimpleName(), getTargetClass().getSimpleName(), value,
                    (int)Character.MIN_VALUE, (int)Character.MAX_VALUE));
        }

        return (char)intValue;
    }

    @Override
    public Character convert(String value) {
        if(value == null) {
            return null;
        }

        if(value.length() != 1) {
            throw new ConversionException(String.format("Cannot convert from %s to %s due to incorrect length - value: %s, length: %s", String.class.getSimpleName(), getTargetClass().getSimpleName(), value, value.length()));
        }

        return value.charAt(0);
    }

    @Override
    public Class<Character> getTargetClass() {
        return Character.class;
    }
}
