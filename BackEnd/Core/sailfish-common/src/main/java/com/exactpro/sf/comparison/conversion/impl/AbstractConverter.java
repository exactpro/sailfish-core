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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.comparison.conversion.ConversionException;
import com.exactpro.sf.comparison.conversion.IConverter;

public abstract class AbstractConverter<T> implements IConverter<T> {
    @Override
    public T convert(Boolean value) {
        return getValue(value);
    }

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
    public T convert(Character value) {
        return getValue(value);
    }

    @Override
    public T convert(String value) {
        return getValue(value);
    }

    @Override
    public T convert(LocalDate value) {
        return getValue(value);
    }

    @Override
    public T convert(LocalTime value) {
        return getValue(value);
    }

    @Override
    public T convert(LocalDateTime value) {
        return getValue(value);
    }

    @Override
    public T convert(Object value) {
        if(value instanceof Boolean) {
            return convert((Boolean)value);
        } else if(value instanceof Byte) {
            return convert((Byte)value);
        } else if(value instanceof Short) {
            return convert((Short)value);
        } else if(value instanceof Integer) {
            return convert((Integer)value);
        } else if(value instanceof Long) {
            return convert((Long)value);
        } else if(value instanceof Float) {
            return convert((Float)value);
        } else if(value instanceof Double) {
            return convert((Double)value);
        } else if(value instanceof BigDecimal) {
            return convert((BigDecimal)value);
        } else if(value instanceof Character) {
            return convert((Character)value);
        } else if(value instanceof String) {
            return convert((String)value);
        } else if(value instanceof LocalDate) {
            return convert((LocalDate)value);
        } else if(value instanceof LocalTime) {
            return convert((LocalTime)value);
        } else if(value instanceof LocalDateTime) {
            return convert((LocalDateTime)value);
        }

        return getValue(value);
    }

    @SuppressWarnings("unchecked")
    private T getValue(Object value) {
        if(value == null || value.getClass() == getTargetClass()) {
            return (T)value;
        }

        throw new ConversionException(String.format("Cannot convert from %s to %s - value: %s", value.getClass().getSimpleName(), getTargetClass().getSimpleName(), value));
    }
}
