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
package com.exactpro.sf.comparison;

import static java.lang.Math.pow;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.StructureType;
import com.exactpro.sf.scriptrunner.StatusType;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class MessageComparator {
    public static double COMPARISON_PRECISION = pow(10, -10);
    private static final String DUMMY = "DUMMY";

    public static ComparisonResult compare(IMessage actual, IMessage expected, ComparatorSettings settings) {
        return compare(actual, expected, settings, true);
    }

    @SuppressWarnings("deprecation")
    public static ComparisonResult compare(IMessage actual, IMessage expected, ComparatorSettings settings, boolean checkNames) {
        String name = actual.getName();
        String namespace = actual.getNamespace();

        if(checkNames && (!name.equals(expected.getName()) || !namespace.equals(expected.getNamespace()))) {
            return null;
        }

        IDictionaryStructure dictionaryStructure = settings.getDictionaryStructure();
        IFieldStructure messageStructure = null;

        if(dictionaryStructure != null) {
            messageStructure = dictionaryStructure.getMessageStructure(name);
        }

        ComparisonResult result = compareValues(name, actual, expected, false, messageStructure, Collections.singletonList(settings.getMetaContainer()), settings);

        if(result != null) {
            Map<String, Boolean> negativeMap = settings.getNegativeMap();

            if(!negativeMap.isEmpty()) {
                invertResults(result, negativeMap);
            }

            IPostValidation validation = settings.getPostValidation();

            if(validation != null) {
                validation.doValidate(actual, expected, settings, result);
            }
        }

        return result.setMetaData(actual.getMetaData());
    }

    protected static ComparisonResult compareValues(String name, Object actual, Object expected, boolean uncheck, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        Throwable t = null;

        try {
            if(checkTypes(actual, expected, Object.class, structure)) {
                return compareObjects(name, actual, expected, uncheck, structure, metaContainers, settings);
            }

            if(checkTypes(actual, expected, List.class, structure)) {
                return compareLists(name, actual, expected, uncheck, structure, metaContainers, settings);
            }

            if(checkTypes(actual, expected, IMessage.class, structure)) {
                return compareMessages(name, actual, expected, uncheck, structure, metaContainers, settings);
            }
        } catch(Exception e) {
            t = e;
        }

        if(t == null) {
            t = new Exception(String.format("Value type mismatch - actual: %s, expected: %s", actual.getClass().getName(), expected.getClass().getName()));
        }

        return new ComparisonResult(name).setException(t).setStatus(StatusType.FAILED);
    }

    private static ComparisonResult compareObjects(String name, Object actual, Object expected, boolean uncheck, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        ComparisonResult result = initialComparison(name, actual, expected, uncheck, structure, metaContainers, settings);

        if(result.getStatus() != null) {
            return result;
        }

        MetaContainer metaContainer = metaContainers.get(0);
        Double doublePrecision = defaultIfNull(metaContainer.getDoublePrecision(name), COMPARISON_PRECISION);
        Double systemPrecision = metaContainer.getSystemPrecision(name);

        result.setSystemPrecision(systemPrecision);

        if(actual instanceof Float && expected instanceof Float) {
            float diff = Math.abs((float)actual - (float)expected);
            return result.setDoublePrecision(doublePrecision).setStatus(diff > doublePrecision ? StatusType.FAILED : StatusType.PASSED);
        } else if(actual instanceof Double && expected instanceof Double) {
            double diff = Math.abs((double)actual - (double)expected);
            return result.setDoublePrecision(doublePrecision).setStatus(diff > doublePrecision ? StatusType.FAILED : StatusType.PASSED);
        } else if(actual instanceof BigDecimal && expected instanceof BigDecimal) {
            BigDecimal actualValue = (BigDecimal)actual;
            BigDecimal expectedValue = (BigDecimal)expected;
            BigDecimal precision = BigDecimal.valueOf(doublePrecision);
            BigDecimal diff = actualValue.subtract(expectedValue).abs();

            return result.setDoublePrecision(doublePrecision).setStatus(diff.compareTo(precision) > 0 ? StatusType.FAILED : StatusType.PASSED);
        }

        if(systemPrecision != null) {
            BigDecimal precision = BigDecimal.valueOf(systemPrecision);
            BigDecimal actualValue = null;

            if(actual instanceof Float) {
                actualValue = BigDecimal.valueOf((float)actual);
            } else if(actual instanceof Double) {
                actualValue = BigDecimal.valueOf((double)actual);
            } else if(actual instanceof BigDecimal) {
                actualValue = (BigDecimal)actual;
            }

            if(actualValue != null) {
                BigDecimal remainder = actualValue.remainder(precision);

                if(remainder.compareTo(BigDecimal.ZERO) != 0) {
                    return result.setStatus(StatusType.FAILED);
                }
            }
        }

        if(expected instanceof String && actual instanceof String) {
            String expectedValue = (String)expected;

            if(expectedValue.startsWith(AMLLangConst.REGEX_FIELD_START) && expectedValue.endsWith(AMLLangConst.REGEX_FIELD_END)) {
                String regex = StringUtils.substringBetween(expectedValue, AMLLangConst.REGEX_FIELD_START, AMLLangConst.REGEX_FIELD_END);
                boolean matches = Pattern.matches(regex, (String)actual);
                return result.setStatus(matches ? StatusType.PASSED : StatusType.FAILED);
            }
        }

        if(actual instanceof BigDecimal && expected instanceof BigDecimal) {
            BigDecimal actualValue = (BigDecimal)actual;
            BigDecimal expectedValue = (BigDecimal)expected;
            return result.setStatus(expectedValue.compareTo(actualValue) == 0 ? StatusType.PASSED : StatusType.FAILED);
        }

        Class<?> actualClass = actual.getClass();
        Class<?> expectedClass = expected.getClass();

        if(actualClass != expectedClass) {
            Exception e = new Exception(String.format("Type mismatch - actual: %s, expected: %s", actualClass.getName(), expectedClass.getName()));
            return result.setException(e).setStatus(StatusType.FAILED);
        }

        return result.setStatus(expected.equals(actual) ? StatusType.PASSED : StatusType.FAILED);
    }

    private static ComparisonResult compareLists(String name, Object actual, Object expected, boolean uncheck, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        ComparisonResult result = initialComparison(name, actual, expected, uncheck, structure, metaContainers, settings);

        if(result.getStatus() != null && result.getEmbeddedListFilter() == null) {
            return result;
        }

        List<?> actualList = (List<?>)actual;
        List<?> expectedList = getExpectedList(expected, result.getEmbeddedListFilter());

        int maxSize = Math.max(actualList.size(), expectedList.size());
        boolean checkOrder = settings.isCheckGroupsOrder() || (structure != null && !structure.isComplex());

        if(!checkOrder && !expectedList.isEmpty()) {
            checkOrder = isObject(expectedList.get(0));
        }

        if(checkOrder) {
            for(int i = 0; i < maxSize; i++) {
                Object actualElement = Iterables.get(actualList, i, null);
                Object expectedElement = Iterables.get(expectedList, i, null);

                if(actualElement == null && expectedElement == null) {
                    continue;
                }

                List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, i);
                String subName = Integer.toString(i);

                result.addResult(compareValues(subName, actualElement, expectedElement, uncheck, structure, subMetaContainers, settings));
            }
        } else {
            int[][] countMatrix = new int[maxSize][maxSize];
            ComparisonResult[][] resultMatrix = new ComparisonResult[maxSize][maxSize];
            int actualSize = actualList.size();
            int expectedSize = expectedList.size();

            for(int actualIndex = 0; actualIndex < actualSize; actualIndex++) {
                for(int expectedIndex = 0; expectedIndex < expectedSize; expectedIndex++) {
                    Object actualElement = actualList.get(actualIndex);
                    Object expectedElement = expectedList.get(expectedIndex);

                    if(actualElement == null && expectedElement == null) {
                        continue;
                    }

                    List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, expectedIndex);
                    ComparisonResult subResult = compareValues(DUMMY, actualElement, expectedElement, uncheck, structure, subMetaContainers, settings);
                    ComparisonResult calculationResult = subResult;
                    if (settings.getPostValidation() != null &&
                            actualElement instanceof IMessage &&
                            expectedElement instanceof IMessage) {
                        calculationResult = new ComparisonResult(calculationResult);
                        settings.getPostValidation().doValidate((IMessage)actualElement, (IMessage)expectedElement, settings, calculationResult);
                    }
                    resultMatrix[actualIndex][expectedIndex] = subResult;
                    countMatrix[actualIndex][expectedIndex] = getCount(calculationResult, StatusType.PASSED) * 1_000_000
                            + getCount(calculationResult, StatusType.CONDITIONALLY_PASSED);
                }
            }

            boolean[] usedActual = new boolean[maxSize];
            boolean[] usedExpected = new boolean[maxSize];

            for(int i = 0; i < maxSize; i++) {
                int maxCount = -1;
                int maxActualIndex = -1;
                int maxExpectedIndex = -1;

                for(int actualIndex = 0; actualIndex < maxSize; actualIndex++) {
                    if(usedActual[actualIndex]) {
                        continue;
                    }

                    for(int expectedIndex = 0; expectedIndex < maxSize; expectedIndex++) {
                        if(usedExpected[expectedIndex]) {
                            continue;
                        }

                        int currentCount = countMatrix[actualIndex][expectedIndex];

                        if(maxCount < currentCount) {
                            maxCount = currentCount;
                            maxActualIndex = actualIndex;
                            maxExpectedIndex = expectedIndex;
                        }
                    }
                }

                usedActual[maxActualIndex] = true;
                usedExpected[maxExpectedIndex] = true;
                ComparisonResult subResult = null;

                if(maxActualIndex >= actualSize) {
                    subResult = compareValues(DUMMY, null, expectedList.get(maxExpectedIndex), uncheck, structure, metaContainers, settings);
                } else if(maxExpectedIndex >= expectedSize) {
                    List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, maxExpectedIndex);
                    subResult = compareValues(DUMMY, actualList.get(maxActualIndex), null, uncheck, structure, subMetaContainers, settings);
                } else {
                    subResult = resultMatrix[maxActualIndex][maxExpectedIndex];
                }

                String subName = Integer.toString(i);
                result.addResult(subResult.setName(subName));
            }
        }

        return result;
    }

    private static ComparisonResult compareMessages(String name, Object actual, Object expected, boolean uncheck, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        ComparisonResult result = initialComparison(name, actual, expected, uncheck, structure, metaContainers, settings);

        if(result.getStatus() != null) {
            return result;
        }

        IMessage actualMessage = (IMessage)actual;
        IMessage expectedMessage = (IMessage)expected;
        Collection<String> fieldNames = null;

        if(structure != null) {
            fieldNames = structure.getFieldNames();
        } else {
            fieldNames = new LinkedHashSet<>();
            fieldNames.addAll(actualMessage.getFieldNames());
            fieldNames.addAll(expectedMessage.getFieldNames());
        }

        for(String fieldName : fieldNames) {
            Object actualValue = actualMessage.getField(fieldName);
            Object expectedValue = expectedMessage.getField(fieldName);

            if(actualValue == null && expectedValue == null) {
                continue;
            }

            boolean fieldUncheck = uncheck || settings.getUncheckedFields().contains(fieldName);

            IFieldStructure fieldStructure = structure != null ? structure.getField(fieldName) : null;
            List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, fieldName);

            result.addResult(compareValues(fieldName, actualValue, expectedValue, fieldUncheck, fieldStructure, subMetaContainers, settings));
        }

        return result;
    }

    private static ComparisonResult initialComparison(String name, Object actual, Object expected, boolean uncheck, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        ComparisonResult result = new ComparisonResult(name);

        result.setActual(getValue(actual));
        result.setExpected(getValue(expected));

        if(expected instanceof IFilter) {
            IFilter filter = (IFilter)expected;

            try {
                ExpressionResult expressionResult = filter.validate(actual);

                if (expressionResult.getEmbeddedListFilter() == null) {
                    addToResult(actual, result, StatusType.NA, structure, metaContainers, settings, true);
                }

                return result.setExpressionResult(expressionResult)
                        .setStatus(expressionResult.getResult() ? StatusType.PASSED : StatusType.FAILED);
            } catch(RuntimeException e) {
                return addToResult(actual, result, StatusType.FAILED, structure, metaContainers, settings, true)
                        .setException(e);
            }
        }

        if(expected == null && actual != null) {
            if(!uncheck && !settings.getUncheckedFields().contains(name)) {
                String failUnexpected = StringUtils.lowerCase(metaContainers.get(0).getFailUnexpected());
                Set<String> allowedValues = Sets.newHashSet(AMLLangConst.ALL);

                if(isObject(actual) && !StringUtils.isNumeric(name)) {
                    allowedValues.add(AMLLangConst.YES);
                }

                if(allowedValues.contains(failUnexpected)) {
                    return addToResult(actual, result, StatusType.NA, structure, metaContainers, settings, true).setStatus(StatusType.FAILED);
                }
            }

            return addToResult(actual, result, StatusType.NA, structure, metaContainers, settings, true);
        }

        if(expected != null && actual == null) {
            // isConventionedValueMissedOrNestedMissed is the hot fix for RM 50554
            StatusType status = Convention.isConventionedValueMissedOrNestedMissed(expected) ? StatusType.PASSED : StatusType.FAILED;
            return addToResult(expected, result, StatusType.NA, structure, metaContainers, settings, false).setStatus(status);
        }

        if(Convention.isConventionedValuePresent(expected)) {
            return addToResult(actual, result, StatusType.NA, structure, metaContainers, settings, true).setStatus(StatusType.PASSED);
        }

        if(Convention.isConventionedValueMissed(expected)) {
            return addToResult(actual, result, StatusType.NA, structure, metaContainers, settings, true).setStatus(StatusType.FAILED);
        }

        return result;
    }

    private static ComparisonResult addToResult(Object object, ComparisonResult result, StatusType status, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings, boolean actual) {
        result.setStatus(status);

        if(actual) {
            result.setActual(getValue(object));
        } else {
            result.setExpected(getValue(object));
        }

        if(object instanceof List<?>) {
            Exception e = checkStructureType(List.class, structure);

            if(e != null) {
                return result.setActual(null).setExpected(null).setException(e).setStatus(StatusType.FAILED);
            }

            List<?> list = (List<?>)object;

            for(int i = 0; i < list.size(); i++) {
                Object value = list.get(i);

                if(value == null) {
                    continue;
                }

                String name = Integer.toString(i);
                ComparisonResult subResult = new ComparisonResult(name);
                List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, i);

                result.addResult(addToResult(value, subResult, status, structure, subMetaContainers, settings, actual));
            }
        } else if(object instanceof IMessage) {
            Exception e = checkStructureType(IMessage.class, structure);

            if(e != null) {
                return result.setActual(null).setExpected(null).setException(e).setStatus(StatusType.FAILED);
            }

            IMessage message = (IMessage)object;
            Collection<String> fieldNames = structure != null ? structure.getFieldNames() : message.getFieldNames();

            for(String fieldName : fieldNames) {
                Object value = message.getField(fieldName);

                if(value == null) {
                    continue;
                }

                IFieldStructure subStructure = structure != null ? structure.getField(fieldName) : null;
                List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, fieldName);
                ComparisonResult subResult = new ComparisonResult(fieldName);

                result.addResult(addToResult(value, subResult, status, subStructure, subMetaContainers, settings, actual));
            }
        } else if(isObject(object)) {
            Exception e = checkStructureType(Object.class, structure);

            if(e != null) {
                return result.setActual(null).setExpected(null).setException(e).setStatus(StatusType.FAILED);
            }

            String name = result.getName();
            MetaContainer metaContainer = metaContainers.get(0);
            Double doublePrecision = metaContainer.getDoublePrecision(name);
            Double systemPrecision = metaContainer.getSystemPrecision(name);

            result.setDoublePrecision(doublePrecision);
            result.setSystemPrecision(systemPrecision);
        }

        return result;
    }

    private static List<MetaContainer> getMetaContainers(List<MetaContainer> metaContainers, int index) {
        if(index < metaContainers.size()) {
            return Collections.singletonList(metaContainers.get(index));
        }

        return metaContainers;
    }

    private static List<MetaContainer> getMetaContainers(List<MetaContainer> metaContainers, String name) {
        List<MetaContainer> subMetaContainers = metaContainers.get(0).get(name);

        if(subMetaContainers != null) {
            return subMetaContainers;
        }

        return metaContainers;
    }

    private static boolean checkTypes(Object actual, Object expected, Class<?> clazz, IFieldStructure structure) throws Exception {
        boolean actualType = clazz == Object.class ? isObject(actual) : clazz.isInstance(actual);
        boolean expectedType = clazz == Object.class ? isObject(expected) : clazz.isInstance(expected);

        if(actualType || expectedType) {
            Exception e = checkStructureType(clazz, structure);

            if(e != null) {
                throw e;
            }
        }

        return (actualType || actual == null) && (expectedType || expected == null || expected instanceof IFilter);
    }

    private static Exception checkStructureType(Class<?> clazz, IFieldStructure structure) {
        if(structure == null) {
            return null;
        }

        if(clazz == Object.class && structure.isComplex()) {
            return new Exception(String.format("Structure type mismatch - actual: %s, expected: %s", StructureType.SIMPLE, structure.getStructureType()));
        }

        if(clazz == List.class && !isCollection(structure)) {
            return new Exception("Structure type mismatch - not a collection");
        }

        if(clazz == IMessage.class && !structure.isComplex()) {
            return new Exception(String.format("Structure type mismatch - actual: %s, expected: %s", StructureType.COMPLEX, structure.getStructureType()));
        }

        return null;
    }

    private static boolean isObject(Object value) {
        return !(value == null || value instanceof List<?> || value instanceof IMessage || value instanceof IFilter);
    }

    private static boolean isCollection(IFieldStructure structure) {
        try {
            return structure.isCollection();
        } catch(UnsupportedOperationException e) {
            return false;
        }
    }

    private static List<?> getExpectedList(Object expected, List<?> embeddedListFilter) {
        if (expected instanceof IFilter && embeddedListFilter != null) {
            return embeddedListFilter;
        }
        return (List<?>)expected;
    }
    
    private static Object getValue(Object wrapper) {
        if(wrapper instanceof List<?>) {
            List<?> list = (List<?>)wrapper;
            return list.size();
        }

        if(wrapper instanceof IMessage) {
            IMessage message = (IMessage)wrapper;
            return message.getFieldCount();
        }

        return wrapper;
    }

    private static int getCount(ComparisonResult result, StatusType statusType) {
        int count = 0;

        for(ComparisonResult subResult : result) {
            if(subResult.getStatus() == statusType) {
                count++;
            }
        }

        return count;
    }

    protected static void invertResults(ComparisonResult result, Map<String, Boolean> negativeMap) {
        Boolean negative = negativeMap.get(result.getName());

        if(BooleanUtils.toBoolean(negative)) {
            StatusType status = result.getStatus();

            if(status == StatusType.FAILED) {
                status = StatusType.PASSED;
            } else if(status == StatusType.PASSED) {
                status = StatusType.FAILED;
            }

            result.setStatus(status);
            result.setExpected("!" + result.getExpected());
        }

        for(ComparisonResult subResult : result) {
            invertResults(subResult, negativeMap);
        }
    }
}
