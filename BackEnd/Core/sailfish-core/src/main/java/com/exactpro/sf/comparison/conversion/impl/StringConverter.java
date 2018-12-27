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
import java.util.Objects;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class StringConverter extends AbstractConverter<String> {
    @Override
    public String convert(Boolean value) {
        return Objects.toString(value, null);
    }

    @Override
    public String convert(Byte value) {
        return Objects.toString(value, null);
    }

    @Override
    public String convert(Short value) {
        return Objects.toString(value, null);
    }

    @Override
    public String convert(Integer value) {
        return Objects.toString(value, null);
    }

    @Override
    public String convert(Long value) {
        return Objects.toString(value, null);
    }

    @Override
    public String convert(Float value) {
        return convertDecimal(value);
    }

    @Override
    public String convert(Double value) {
        return convertDecimal(value);
    }

    private String convertDecimal(Number value) {
        return value != null ? convert(new BigDecimal(value.toString())) : null;
    }

    @Override
    public String convert(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : null;
    }

    @Override
    public String convert(Character value) {
        return Objects.toString(value, null);
    }

    @Override
    public String convert(LocalDate value) {
        return convertTemporal(value, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public String convert(LocalTime value) {
        return convertTemporal(value, DateTimeFormatter.ISO_LOCAL_TIME);
    }

    @Override
    public String convert(LocalDateTime value) {
        return convertTemporal(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String convertTemporal(TemporalAccessor accessor, DateTimeFormatter formatter) {
        return accessor != null ? formatter.withZone(ZoneOffset.UTC).format(accessor) : null;
    }

    @Override
    public Class<String> getTargetClass() {
        return String.class;
    }
}
