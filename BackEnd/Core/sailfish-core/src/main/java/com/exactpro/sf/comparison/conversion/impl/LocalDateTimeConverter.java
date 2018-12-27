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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter extends AbstractDateTimeConverter<LocalDateTime> {
    @Override
    public LocalDateTime convert(LocalDate value) {
        return value != null ? getValue(value) : null;
    }

    @Override
    public LocalDateTime convert(LocalTime value) {
        return value != null ? getValue(value) : null;
    }

    @Override
    protected DateTimeFormatter getFormatter() {
        return DateTimeFormatter.ISO_DATE_TIME;
    }

    @Override
    protected LocalDateTime convertValue(LocalDateTime value) {
        return value;
    }

    @Override
    public Class<LocalDateTime> getTargetClass() {
        return LocalDateTime.class;
    }
}
