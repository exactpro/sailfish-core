/******************************************************************************
 * Copyright 2009-2022 Exactpro (Exactpro Systems Limited)
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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
            messageStructure = dictionaryStructure.getMessages().get(name);
        }

        if (settings.getMetaContainer().hasKeyFields()) {
            ComparisonResult result = compareValues(name, actual, expected, false, true, messageStructure, Collections.singletonList(settings.getMetaContainer()), settings);

            if (ComparisonUtil.getResultCount(result, StatusType.FAILED) > 0) {
                return null;
            }
        }

        ComparisonResult result = compareValues(name, actual, expected, false, false, messageStructure, Collections.singletonList(settings.getMetaContainer()), settings);

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

    protected static ComparisonResult compareValues(String name, Object actual, Object expected, boolean unchecked, boolean keyFieldsOnly, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        Exception exception = null;

        try {
            if(checkTypes(actual, expected, Object.class, structure)) {
                return compareObjects(name, actual, expected, unchecked, structure, metaContainers, settings);
            }

            if(checkTypes(actual, expected, List.class, structure)) {
                return compareLists(name, actual, expected, unchecked, keyFieldsOnly, structure, metaContainers, settings);
            }

            if(checkTypes(actual, expected, IMessage.class, structure)) {
                return compareMessages(name, actual, expected, unchecked, keyFieldsOnly, structure, metaContainers, settings);
            }
        } catch(Exception e) {
            exception = e;
        }

        if (exception == null) {
            exception = new Exception(String.format("Value type mismatch - actual: %s, expected: %s", getObjectType(actual), getObjectType(expected)));
        }

        return new ComparisonResult(name)
                .setExpected(getObjectType(expected))
                .setActual(getObjectType(actual))
                .setException(exception).setStatus(StatusType.FAILED);
    }

    @NotNull
    private static String getObjectType(Object value) {
        return getObjectType(value, true);
    }

    @NotNull
    private static String getObjectType(Object value, boolean addCollectionContent) {
        if (value == null) {
            return "null";
        }

        if (value instanceof IFilter) {
            return "Filter";
        }

        if (value instanceof IMessage) {
            return "Message";
        }

        if (value instanceof List<?>) {
            if (!addCollectionContent) {
                return "Collection";
            }
            List<?> list = (List<?>)value;
            return list.isEmpty()
                    ? "Empty collection"
                    : getCollectionType(list);
        }

        return value.getClass().getSimpleName();
    }

    @NotNull
    private static String getCollectionType(List<?> list) {
        Object value = list.get(0);
        if (value instanceof IFilter) {
            // cannot add the type of the collection elements for filters
            return "Collection";
        }
        return "Collection of " + getObjectType(value, false) + "s";
    }

    private static ComparisonResult compareObjects(String name, Object actual, Object expected, boolean unchecked, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        ComparisonResult result = initialComparison(name, actual, expected, unchecked, structure, metaContainers, settings);

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

    private static ComparisonResult compareLists(String name, Object actual, Object expected, boolean unchecked, boolean keyFieldsOnly, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        ComparisonResult result = initialComparison(name, actual, expected, unchecked, structure, metaContainers, settings);

        if(result.getStatus() != null && result.getEmbeddedListFilter() == null) {
            return result;
        }

        List<?> actualList = (List<?>)actual;
        List<?> expectedList = getExpectedList(expected, result.getEmbeddedListFilter());

        int maxSize = Math.max(actualList.size(), expectedList.size());
        boolean complex = structure != null && structure.isComplex() || !actualList.isEmpty() && !isObject(actualList.get(0));
        boolean checkOrder = complex && settings.isCheckGroupsOrder() || !complex && settings.isCheckSimpleCollectionsOrder();

        if(checkOrder) {
            for(int i = 0; i < maxSize; i++) {
                Object actualElement = Iterables.get(actualList, i, null);
                Object expectedElement = Iterables.get(expectedList, i, null);

                if(actualElement == null && expectedElement == null) {
                    continue;
                }

                List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, i, keyFieldsOnly);

                if (complex && keyFieldsOnly && !subMetaContainers.get(0).hasKeyFields()) {
                    continue;
                }

                String subName = Integer.toString(i);

                result.addResult(compareValues(subName, actualElement, expectedElement, unchecked, keyFieldsOnly, structure, subMetaContainers, settings));
            }
        } else {
            int[][] countMatrix = new int[maxSize][maxSize];
            ComparisonResult[][] resultMatrix = new ComparisonResult[maxSize][maxSize];
            int actualSize = actualList.size();
            int expectedSize = expectedList.size();
            List<List<MetaContainer>> metaContainersCache = new ArrayList<>(maxSize);

            for (int i = 0; i < maxSize; i++) {
                metaContainersCache.add(getMetaContainers(metaContainers, i, keyFieldsOnly));
            }

            for(int actualIndex = 0; actualIndex < actualSize; actualIndex++) {
                for(int expectedIndex = 0; expectedIndex < expectedSize; expectedIndex++) {
                    Object actualElement = actualList.get(actualIndex);
                    Object expectedElement = expectedList.get(expectedIndex);

                    if(actualElement == null && expectedElement == null) {
                        continue;
                    }

                    List<MetaContainer> subMetaContainers = metaContainersCache.get(expectedIndex);

                    if (keyFieldsOnly && !subMetaContainers.get(0).hasKeyFields()) {
                        continue;
                    }

                    ComparisonResult subResult = compareValues(DUMMY, actualElement, expectedElement, unchecked, keyFieldsOnly, structure, subMetaContainers, settings);
                    ComparisonResult calculationResult = subResult;

                    if (settings.getPostValidation() != null &&
                            actualElement instanceof IMessage &&
                            expectedElement instanceof IMessage) {
                        calculationResult = new ComparisonResult(calculationResult);
                        settings.getPostValidation().doValidate((IMessage)actualElement, (IMessage)expectedElement, settings, calculationResult);
                    }

                    resultMatrix[actualIndex][expectedIndex] = subResult;
                    countMatrix[actualIndex][expectedIndex] = ComparisonUtil.getResultCount(calculationResult, StatusType.PASSED) * 1_000_000
                            + ComparisonUtil.getResultCount(calculationResult, StatusType.CONDITIONALLY_PASSED);
                }
            }

            boolean[] usedActual = new boolean[maxSize];
            boolean[] usedExpected = new boolean[maxSize];
            ComparisonResult[] sortedResults = new ComparisonResult[maxSize];

            for(int i = 0; i < maxSize; i++) {
                int maxCount = -1;
                int maxActualIndex = -1;
                int maxExpectedIndex = -1;

                if (keyFieldsOnly && !metaContainersCache.get(i).get(0).hasKeyFields()) {
                    usedExpected[i] = true;
                    continue;
                }

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
                    List<MetaContainer> subMetaContainers = metaContainersCache.get(maxExpectedIndex);
                    subResult = compareValues(DUMMY, null, expectedList.get(maxExpectedIndex), unchecked, keyFieldsOnly, structure, subMetaContainers, settings);
                } else if(maxExpectedIndex >= expectedSize) {
                    List<MetaContainer> subMetaContainers = metaContainersCache.get(maxExpectedIndex);
                    subResult = compareValues(DUMMY, actualList.get(maxActualIndex), null, unchecked, keyFieldsOnly, structure, subMetaContainers, settings);
                } else {
                    subResult = resultMatrix[maxActualIndex][maxExpectedIndex];
                }

                // We need to place the result right to a position that corresponds to the actual element in collection
                // Otherwise, we can use the total counter that will place the result from best match to worse match
                int resultIndex = settings.isKeepResultGroupOrder() ? maxActualIndex : i;
                String subName = Integer.toString(resultIndex);
                sortedResults[resultIndex] = subResult.setName(subName);
            }

            for (ComparisonResult subResult : sortedResults) {
                if (subResult != null) { // because when we compare only key fields some comparison might be skipped
                    result.addResult(subResult);
                }
            }
        }

        return result;
    }

    private static ComparisonResult compareMessages(String name, Object actual, Object expected, boolean unchecked, boolean keyFieldsOnly, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        ComparisonResult result = initialComparison(name, actual, expected, unchecked, structure, metaContainers, settings);

        if(result.getStatus() != null) {
            return result;
        }

        IMessage actualMessage = (IMessage)actual;
        IMessage expectedMessage = (IMessage)expected;
        Collection<String> fieldNames = null;
        Map<String, Boolean> keyFields = keyFieldsOnly ? metaContainers.get(0).getKeyFields() : Collections.emptyMap();

        if (keyFieldsOnly) {
            fieldNames = keyFields.keySet();
        } else if (structure != null) {
            fieldNames = structure.getFields().keySet();
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

            boolean uncheckedField = unchecked || settings.getUncheckedFields().contains(fieldName);
            boolean checkKeyFieldsOnly = keyFieldsOnly && keyFields.get(fieldName);
            IFieldStructure fieldStructure = structure == null ? null : structure.getFields().get(fieldName);
            List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, fieldName, checkKeyFieldsOnly);

            ComparisonResult subResult = compareValues(fieldName, actualValue, expectedValue, uncheckedField, checkKeyFieldsOnly, fieldStructure, subMetaContainers, settings);
            subResult.setKey(isKeyField(fieldName, metaContainers.get(0)));
            result.addResult(subResult);
        }

        return result;
    }

    private static ComparisonResult initialComparison(String name, Object actual, Object expected, boolean unchecked, IFieldStructure structure, List<MetaContainer> metaContainers, ComparatorSettings settings) {
        ComparisonResult result = new ComparisonResult(name);

        result.setActual(getValue(actual));
        result.setExpected(getValue(expected));

        // We can determinate if the field is key only if we have a single MetaContainer
        // Otherwise, the caller must determinate whether the field is key or not
        if (metaContainers.size() == 1) {
            result.setKey(isKeyField(name, metaContainers.get(0)));
        }

        if (settings.getIgnoredFields().contains(name)) {
            //TODO NA status must be set for nested structures
            return addToResult(actual, result, StatusType.NA, structure, metaContainers, settings, true);
        }

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
            if (!unchecked && !settings.getUncheckedFields().contains(name)) {
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

    private static boolean isKeyField(String name, MetaContainer metaContainer) {
        return BooleanUtils.isFalse(metaContainer.getKeyFields().get(name));
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
                List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, i, false);

                result.addResult(addToResult(value, subResult, status, structure, subMetaContainers, settings, actual));
            }
        } else if(object instanceof IMessage) {
            Exception e = checkStructureType(IMessage.class, structure);

            if(e != null) {
                return result.setActual(null).setExpected(null).setException(e).setStatus(StatusType.FAILED);
            }

            IMessage message = (IMessage)object;
            Collection<String> fieldNames = structure != null ? structure.getFields().keySet() : message.getFieldNames();

            for(String fieldName : fieldNames) {
                Object value = message.getField(fieldName);

                if(value == null) {
                    continue;
                }

                IFieldStructure subStructure = structure != null ? structure.getFields().get(fieldName) : null;
                List<MetaContainer> subMetaContainers = getMetaContainers(metaContainers, fieldName, false);
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

    private static List<MetaContainer> getMetaContainers(List<MetaContainer> metaContainers, int index, boolean removeInheritedKeyFields) {
        int size = metaContainers.size();

        if (size == 0) {
            throw new IllegalStateException("There should be at least one meta container per list");
        }

        if (index < size) {
            if (index == 0 && size == 1) {
                return metaContainers;
            }

            return Collections.singletonList(metaContainers.get(index));
        }

        MetaContainer metaContainer = metaContainers.get(0);

        if (removeInheritedKeyFields && metaContainer.hasKeyFields()) {
            metaContainer = metaContainer.clone().setKeyFields(Collections.emptySet());
            return Collections.singletonList(metaContainer);
        }

        if (size == 1) {
            return metaContainers;
        }

        return Collections.singletonList(metaContainer);
    }

    private static List<MetaContainer> getMetaContainers(List<MetaContainer> metaContainers, String name, boolean removeInheritedKeyFields) {
        if (metaContainers.size() != 1) {
            throw new IllegalStateException("Expected one meta container per message, but got " + metaContainers.size());
        }

        MetaContainer metaContainer = metaContainers.get(0);
        List<MetaContainer> subMetaContainers = metaContainer.get(name);

        if (subMetaContainers == null) {
            if (removeInheritedKeyFields && metaContainer.hasKeyFields()) {
                metaContainer = metaContainer.clone().setKeyFields(Collections.emptySet());
                return Collections.singletonList(metaContainer);
            }

            return metaContainers;
        }

        return subMetaContainers;
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
        return expected instanceof IFilter && embeddedListFilter != null ? embeddedListFilter : (List<?>)expected;
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
