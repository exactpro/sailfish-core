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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import java.time.temporal.Temporal;

public class DateModificator {

    private final long value;
    private final Strategy strategy;
    private final DateComponent dateComponent;

    private DateModificator(Strategy strategy, DateComponent dateComponent, long value) {
        this.strategy = strategy;
        this.value = Math.abs(value);
        this.dateComponent = dateComponent;
    }

    public static List<DateModificator> parse(String modifyPattern) {
        if (StringUtils.isNotBlank(modifyPattern)) {
            List<DateModificator> result = new ArrayList<>();
            String[] array = StringUtils.stripAll(StringUtils.split(modifyPattern, ':'));

            for (String item : array) {
                if (StringUtils.isNoneEmpty(item)) {
                    Strategy strategy = searchStrategy(item);
                    if (strategy != null) {
                        String[] pair = StringUtils.stripAll(StringUtils.split(item, strategy.symbol));
                        if (pair.length == 2) {
                            DateComponent dateComponent = DateComponent.parse(pair[0]);
                            if (dateComponent != null) {
                                try {
                                    long value = Long.parseLong(pair[1]);
                                    result.add(new DateModificator(strategy, dateComponent, value));
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException("Field value should be long: '" + pair[1] + "' in '" + modifyPattern + "'");
                                }
                            } else {
                                throw new RuntimeException("Unknown field specified: '" + pair[0] + "'. Expected " + Arrays.toString(DateComponent.values()) + ".");
                            }
                        } else {
                            throw new RuntimeException("Invalid field format: '" + item + "' in '" + modifyPattern + "'");
                        }
                    } else {
                        throw new RuntimeException("Invalid field format. Action missed: '" + item + "' in '" + modifyPattern + "'. Expected [+-=]");
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static Strategy searchStrategy(String value) {
        for (Strategy strategy : Strategy.values()) {
            if (StringUtils.containsAny(value, strategy.symbol)) {
                return strategy;
            }
        }
        return null;
    }

    public <T extends Temporal> T modify(T source) {
        return this.strategy.modify(source, this.dateComponent, this.value);
    }

    private enum Strategy {
        PLUS('+') {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends Temporal> T modify(T source, DateComponent dateComponent, long value) {
                return (T) source.plus(value, dateComponent.getTemporalField().getBaseUnit());
            }
        },
        MINUS('-') {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends Temporal> T modify(T source, DateComponent dateComponent, long value) {
                return (T) source.minus(value, dateComponent.getTemporalField().getBaseUnit());
            }
        },
        WITH('=') {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends Temporal> T modify(T source, DateComponent dateComponent, long value) {
                return (T) source.with(dateComponent.getTemporalField(), value);
            }
        };

        private final char symbol;

        private Strategy(char symbol) {
            this.symbol = symbol;
        }

        @SuppressWarnings("unused")
        public static Strategy parse(char symbol) {
            for (Strategy strategy : Strategy.values()) {
                if (strategy.symbol == symbol) {
                    return strategy;
                }
            }
            return null;
        }

        public abstract <T extends Temporal> T modify(T source, DateComponent dateComponent, long value);
    }
}
