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

import java.math.BigDecimal;

import com.exactpro.sf.comparison.conversion.ConversionException;

public abstract class AbstractNumberConverter<T extends Number> extends AbstractConverter<T> {
    @Override
    public T convert(Byte value) {
        return getValue(value);
    }

    @Override
    public T convert(Short value) {
        return getValue(value);
    }

    @Override
    public T convert(Integer value) {
        return getValue(value);
    }

    @Override
    public T convert(Long value) {
        return getValue(value);
    }

    @Override
    public T convert(Float value) {
        return getValue(value);
    }

    @Override
    public T convert(Double value) {
        return getValue(value);
    }

    @Override
    public T convert(BigDecimal value) {
        return getValue(value);
    }

    @Override
    public T convert(String value) {
        try {
            return getValue(value);
        } catch(NumberFormatException e) {
            throw new ConversionException(String.format("Cannot convert from %s to %s - value: %s, reason: %s", String.class.getSimpleName(), getTargetClass().getSimpleName(), value, e.getMessage()), e);
        }
    }

    protected abstract T convertValue(Number value);

    protected abstract T convertValue(String value);

    @SuppressWarnings("unchecked")
    protected T getValue(Number value) {
        if(value == null || value.getClass() == getTargetClass()) {
            return (T)value;
        }

        return checkForDataLoss(value, convertValue(value));
    }

    protected T getValue(String value) {
        return value != null ? checkForDataLoss(value, convertValue(value)) : null;
    }

    @SuppressWarnings("unchecked")
    protected T checkForDataLoss(Object original, Number converted) {
        if(converted instanceof BigDecimal) {
            return (T)converted;
        }

        BigDecimal originalValue = new BigDecimal(original.toString());
        BigDecimal convertedValue = new BigDecimal(converted.toString());

        if(originalValue.compareTo(convertedValue) != 0) {
            throw new ConversionException(String.format("Cannot convert from %s to %s due to data loss - original: %s, converted: %s", original.getClass().getSimpleName(), getTargetClass().getSimpleName(), original, converted));
        }

        return (T)converted;
    }
}
