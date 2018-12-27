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
package com.exactpro.sf.actions.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;

/**
 * @author alexey.suknatov
 * 
 */
public enum DateComponent {
    YEAR("Y", ChronoField.YEAR), 
    MONTH("M", ChronoField.MONTH_OF_YEAR),
    DAY("D", ChronoField.DAY_OF_MONTH),
    HOUR("h", ChronoField.HOUR_OF_DAY),
    MINUTE("m", ChronoField.MINUTE_OF_HOUR),
    SECOND("s", ChronoField.SECOND_OF_MINUTE),
    MILLESECOND("ms", ChronoField.MILLI_OF_SECOND),
    MICROSECOND("mc", ChronoField.MICRO_OF_SECOND),
    NANOSECOND("ns", ChronoField.NANO_OF_SECOND);

    private final String datePart;
    private final TemporalField temporalField;

    DateComponent(String datePart, TemporalField temporalField) {
        this.datePart = datePart;
        this.temporalField = temporalField;
    }

    public static DateComponent parse(String datePart) {
        for (DateComponent datePartEnum : DateComponent.values()) {
            if (datePartEnum.datePart.contentEquals(datePart)) {
                return datePartEnum;
            }
        }
        return null;
    }

    public long diff(LocalDateTime minuend, LocalDateTime subtrahend) {
        return this.temporalField.getBaseUnit().between(subtrahend, minuend);
    }

    public int extract(Temporal source) {
        return source.get(this.temporalField);
    }

    public TemporalField getTemporalField() {
        return this.temporalField;
    }

    @Override
    public String toString() {
        return this.datePart;
    }
}
