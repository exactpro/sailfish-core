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
package com.exactpro.sf.actions.tools;

import java.util.Objects;
import java.util.function.Function;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.aml.scriptutil.MvelException;
import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;

public class Filters {
    public static IFilter any(long line, String column) {
        return StaticUtil.notNullFilter(line, ObjectUtils.defaultIfNull(column, ""));
    }

    public static IFilter any(long line) {
        return any(line, null);
    }

    public static IFilter any(String column) {
        return any(0, column);
    }

    public static IFilter any() {
        return any(0, null);
    }

    public static IFilter empty(long line, String column) {
        return StaticUtil.nullFilter(line, ObjectUtils.defaultIfNull(column, ""));
    }

    public static IFilter empty(long line) {
        return empty(line, null);
    }

    public static IFilter empty(String column) {
        return empty(0, column);
    }

    public static IFilter empty() {
        return empty(0, null);
    }

    public static IFilter filter(long line, String column, Function<Object, Boolean> function, Function<Object, String> view) {
        return new FunctionFilter<>(line, column, function, view);
    }

    public static IFilter filter(long line, Function<Object, Boolean> function, Function<Object, String> view) {
        return filter(line, null, function, view);
    }

    public static IFilter filter(String column, Function<Object, Boolean> function, Function<Object, String> view) {
        return filter(0, column, function, view);
    }

    public static IFilter filter(Function<Object, Boolean> function, Function<Object, String> view) {
        return filter(0, null, function, view);
    }

    public static IFilter filter(long line, String column, Object simpleValue) {
        return new FunctionFilter<>(line, column, simpleValue);
    }

    public static IFilter filter(long line, Object simpleValue) {
        return filter(line, null, simpleValue);
    }

    public static IFilter filter(String column, Object simpleValue) {
        return filter(0, column, simpleValue);
    }

    public static IFilter filter(Object simpleValue) {
        return filter(0, null, simpleValue);
    }

    private static class FunctionFilter<T> implements IFilter {
        private static final Function<Object, String> SIMPLE_CONDITION_VIEW = value -> "x == " + value;
        private static final Object NOT_PRESENT = new Object();

        private final Function<T, Boolean> function;
        private final Function<Object, String> view;
        private final Object value;
        private final long line;
        private final String column;

        private FunctionFilter(long line, String column, Function<T, Boolean> function, Function<Object, String> view, Object value) {
            this.function = Objects.requireNonNull(function, "Function can't be null");
            this.view = Objects.requireNonNull(view, "View can't be null");
            this.value = value;
            this.line = line;
            this.column = column;
        }

        public FunctionFilter(long line, String column, Function<T, Boolean> function, Function<Object, String> view) {
            this(line, column, function, view, NOT_PRESENT);
        }

        public FunctionFilter(long line, String column, Object value) {
            this(line, column, arg -> Objects.equals(arg, value), SIMPLE_CONDITION_VIEW, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public ExpressionResult validate(Object value) {
            return ExpressionResult.create(this.function.apply((T)value));
        }

        @Override
        public String getCondition() {
            return view == SIMPLE_CONDITION_VIEW ? view.apply(getValue()) : "user condition";
        }

        @Override
        public String getCondition(Object value) {
            return view == SIMPLE_CONDITION_VIEW ? view.apply(getValue()) : view.apply(value);
        }

        @Override
        public Object getValue() throws MvelException {
            if (this.value == NOT_PRESENT) {
                throw new MvelException(line, column, "Cannot get value from " + this.getClass().getSimpleName());
            }
            return this.value;
        }
        
        @Override
        public boolean hasValue() {
            return false;
        }
    }
}
