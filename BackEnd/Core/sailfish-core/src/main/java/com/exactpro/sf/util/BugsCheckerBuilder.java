/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.util;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.exactpro.sf.comparison.NullValueSubstitute;
import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.Convention;
import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * @author sergey.vasiliev
 *
 */
public class BugsCheckerBuilder extends AbstractBugsChecker {
    public static String ORIGIN_VALUE_MESSAGE = "Origin value";

    private static final Set<Object> CONVENTIONS = ImmutableSet.of(Convention.CONV_MISSED_OBJECT, Convention.CONV_PRESENT_OBJECT, Convention.CONV_EXISTENCE_OBJECT);

    private final Object originValue;
    private final SetMultimap<Object, BugDescription> alternativeValues = LinkedHashMultimap.create();
    private Object actualValue = BugsCheckerBuilder.class; //default value, because real actual value may be null
    private final Function<Object, Object> defaultActualMapFunction = value ->
            actualValue == BugsCheckerBuilder.class ? convertValue(value) : actualValue;

    private Function<Object, Object> actualMapFunction = defaultActualMapFunction;

    public BugsCheckerBuilder(Object originValue) {
        this.originValue = convertValue(originValue);
    }

    @Override
    public BugsCheckerBuilder Bug(String subject, Object alternativeValue, String... categories) {
        BugDescription bugDescription = new BugDescription(subject, categories);

        alternativeValue = ObjectUtils.defaultIfNull(alternativeValue, Convention.CONV_MISSED_OBJECT);

        if (Objects.equals(originValue, alternativeValue)) {
            throw new EPSCommonException(
                    "Alternative value " + alternativeValue + " with description " + bugDescription + " are equal to origin value " + originValue);
        }
        try {
            alternativeValues.put(alternativeValue, bugDescription);
        } catch (IllegalArgumentException e) {
            throw new EPSCommonException("Alternative values map already contains " + alternativeValue, e);
        }
        return this;
    }

    @Override
    public BugsCheckerBuilder BugAny(String subject, String... categories) {
        if(originValue != Convention.CONV_MISSED_OBJECT) {
            throw new EPSCommonException("Expected value '" + originValue + "' is not empty");
        }
        return Bug(subject, Convention.CONV_PRESENT_OBJECT, categories);
    }

    public BugsCheckerBuilder ActualFunction(Function<Object, Object> actualMapFunction) {
        if(isActualEmpty()) {
            this.actualMapFunction = Objects.requireNonNull(actualMapFunction, "Actual map function can't be null");
        }
        return this;
    }

    @Override
    public BugsCheckerBuilder Actual(Object obj) {
        if(isActualEmpty()) {
            this.actualValue = convertValue(obj);
        }
        return this;
    }

    @Override
    public ExpressionResult validate(Object actualValue) throws KnownBugException {
        actualValue = actualMapFunction.apply(actualValue);

        if (isConventionsMatched(originValue, actualValue)) {
            return new ExpressionResult(false, ORIGIN_VALUE_MESSAGE, null, null, getDescriptions());
        }

        Function<Object, ExpressionResult> checkFunction = value -> ExpressionResult.create( Objects.equals(originValue, value));

        if(!CONVENTIONS.contains(originValue) && !CONVENTIONS.contains(actualValue)) {
            if(originValue instanceof IFilter) {
                checkFunction = ((IFilter)originValue)::validate;
            } else if(originValue != null && actualValue != null) {
                Object convertedValue = MultiConverter.convert(originValue, actualValue.getClass());
                checkFunction = value -> ExpressionResult.create(areEqual(convertedValue, value));
            }
        }

        if(!checkFunction.apply(actualValue).getResult()) {
            Set<BugDescription> actualDescriptions = new LinkedHashSet<>(); 
            Set<BugDescription> descriptions = alternativeValues.get(actualValue);
            if (descriptions != null) {
                actualDescriptions.addAll(descriptions);
            }

            boolean convertible = false;
            if (actualValue != null) {
                convertible = MultiConverter.SUPPORTED_TYPES.contains(actualValue.getClass());
            }
            
            for(Object alternativeValue : alternativeValues.keySet()) {
                descriptions = alternativeValues.get(alternativeValue);
                
                if(alternativeValue instanceof IFilter) {
                    IFilter filter = (IFilter)alternativeValue;
                    if(filter.validate(actualValue).getResult()) {
                        actualDescriptions.addAll(descriptions);
                    }
                } else if (convertible && MultiConverter.SUPPORTED_TYPES.contains(alternativeValue.getClass())) {
                    Object convertedValue = MultiConverter.convert(alternativeValue, actualValue.getClass());
                    if (areEqual(convertedValue, actualValue)) {
                        actualDescriptions.addAll(descriptions);
                    }
                } else if (CONVENTIONS.contains(alternativeValue)) {
                    if (isConventionsMatched(alternativeValue, actualValue)) {
                        actualDescriptions.addAll(descriptions);
                    }
                }
            }

            if (!actualDescriptions.isEmpty()) {
                return createExpressionResult(this, actualDescriptions);
            }
            Throwable exception = new EPSCommonException("Expected " + formatValue(originValue) + " value is not equal actual " + formatValue(actualValue) + " value");
            return new ExpressionResult(false, exception.getMessage(), exception, null, getDescriptions());
        }
        return new ExpressionResult(false, ORIGIN_VALUE_MESSAGE, null, null, getDescriptions());
    }

    @Override
    public String getCondition() {
        StringBuilder stringBuilder = new StringBuilder()
                .append("Expected: ").append(formatValue(originValue)).append(", Bugs: [");
        for (Object alternativeValue : alternativeValues.keySet()) {
            stringBuilder
                .append("'").append(alternativeValues.get(alternativeValue)).append("'")
                .append(" = ").append(formatValue(alternativeValue))
                .append(", ");
        }
        stringBuilder.setLength(stringBuilder.length() - 2);
        return stringBuilder.append(']').toString();
    }

    private String formatValue(Object value) {
        StringBuilder resultBuilder = new StringBuilder(String.valueOf(value));
        if (!CONVENTIONS.contains(value)) {
            resultBuilder.append(" (").append(value.getClass().getSimpleName()).append(")");
        }
        return resultBuilder.toString();
    }

    private Set<BugDescription> getDescriptions() {
        return alternativeValues.isEmpty()
                ? EXPECTED_ONLY_BUG_DESCRIPTION_SET
                : Collections.unmodifiableSet(new HashSet<>(alternativeValues.values()));
    }

    private static Object convertValue(Object value) {
        Object result = ObjectUtils.defaultIfNull(value, Convention.CONV_MISSED_OBJECT);
        return result == NullValueSubstitute.INSTANCE ? Convention.CONV_EXISTENCE_OBJECT : result;
    }

    private static ExpressionResult createExpressionResult(BugsCheckerBuilder bugsCheckerBuilder, Set<BugDescription> descriptions) {
        return new ExpressionResult(false, "Actual " + bugsCheckerBuilder.formatValue(bugsCheckerBuilder.actualValue) + " value сomplies with bug(s): '" + descriptions + "'", null, descriptions, bugsCheckerBuilder.getDescriptions());
    }

    private boolean isActualEmpty() {
        if(actualMapFunction != defaultActualMapFunction) {
            throw new EPSCommonException("Actual map function already set");
        } else if(actualValue != BugsCheckerBuilder.class) {
            throw new EPSCommonException("Actual already set '" + actualValue + "'");
        }

        return true;
    }

    private static boolean isConventionsMatched(Object originValue, Object actualValue) {
        if (originValue == Convention.CONV_MISSED_OBJECT) {
            return actualValue == Convention.CONV_MISSED_OBJECT || actualValue == Convention.CONV_EXISTENCE_OBJECT;
        }
        if (originValue == Convention.CONV_PRESENT_OBJECT) {
            return actualValue != Convention.CONV_MISSED_OBJECT && actualValue != Convention.CONV_EXISTENCE_OBJECT;
        }
        if (originValue == Convention.CONV_EXISTENCE_OBJECT) {
            return actualValue != Convention.CONV_MISSED_OBJECT;
        }
        return false;
    }

    @SuppressWarnings("OverlyStrongTypeCast")
    private static boolean areEqual(Object first, Object second) {
        if (first instanceof BigDecimal && second instanceof BigDecimal) {
            return ((BigDecimal)first).compareTo((BigDecimal)second) == 0;
        }

        return Objects.equals(first, second);
    }
}
