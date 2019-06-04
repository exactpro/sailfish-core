/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.embedded.statistics.storage.utils;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class Conditions {

    public static ICondition create(String value) {
        return new SimpleCondition(value);
    }

    public static ICondition or(ICondition... conditions) {
        return new MultipleCondition(Operator.OR, conditions);
    }

    public static ICondition and(ICondition... conditions) {
        return new MultipleCondition(Operator.AND, conditions);
    }

    public static ICondition wrap(ICondition condition) {
        return new WrapCondition(condition);
    }

    private static class MultipleCondition extends SimpleCondition {
        private MultipleCondition(Operator operator, ICondition... conditions) {
            super(condition(operator, conditions));
        }

        private static String condition(Operator operator, ICondition... conditions) {
            requireNonNull(operator, "'Operator' parameter");
            return Stream.of(requireNonNull(conditions, "'Conditions' parameter"))
                    .filter(Objects::nonNull)
                    .map(ICondition::getCondition)
                    .collect(Collectors.joining(" " + operator.getValue() + " "));
        }
    }

    private static class SimpleCondition implements ICondition {
        private final String value;

        private SimpleCondition(String value) {
            if (StringUtils.isBlank(value)) {
                throw new IllegalArgumentException("condition can't be blank");
            }
            this.value = value;
        }

        @Override
        public String getCondition() {
            return value;
        }
    }

    private static class WrapCondition extends SimpleCondition {
        private WrapCondition(ICondition condition) {
            super("(" + condition.getCondition() + ")");
        }
    }

    private enum Operator {
        OR("or"),
        AND("and");

        private final String value;

        Operator(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
