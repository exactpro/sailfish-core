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
package com.exactpro.sf.util;

import static com.exactpro.sf.util.BugsListCheckerBuilder.ListValueType.DEFAULT_LIST;
import static com.exactpro.sf.util.BugsListCheckerBuilder.ListValueType.MISSED_LIST_SIZE;
import static com.exactpro.sf.util.BugsListCheckerBuilder.ListValueType.PRESENT_LIST_SIZE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.Convention;

public class BugsListCheckerBuilder extends AbstractBugsChecker {
    public static String ORIGIN_SIZE_MESSAGE = "Origin size";

    private final ListWrapper originList;
    private final Map<Integer, ListWrapper> alternativeSizes = new HashMap<>();
    private Integer actualSize = DEFAULT_LIST.key; //default value, because real actual value may be null

    public BugsListCheckerBuilder(Object originValue) {
        this.originList = wrap(null, originValue);
    }

    public BugsListCheckerBuilder Bug(String subject, Object alternativeValue, String... categories) {
        BugDescription bugDescription = new BugDescription(subject, categories);

        ListWrapper alternativeList = wrap(bugDescription, alternativeValue);

        if (Objects.equals(originList, alternativeList)) {
            throw new EPSCommonException(
                    "Alternative " + alternativeList + " with description " + bugDescription + " are equal to origin " + originList);
        }
        if (this.alternativeSizes.put(alternativeList.size, alternativeList) != null) {
            throw new EPSCommonException("Alternative sizes map already contains " + alternativeList);
        }
        return this;
    }

    @Override
    public BugsListCheckerBuilder BugAny(String subject, String... categories) {
        if (this.originList.size != MISSED_LIST_SIZE.key) {
            throw new EPSCommonException("Expected size '" + this.originList + "' is not empty");
        }
        return Bug(subject, Convention.CONV_PRESENT_OBJECT, categories);
    }

    @Override
    public BugsListCheckerBuilder Actual(Object obj) {
        if (isActualEmpty()) {
            this.actualSize = size(obj);
        }
        return this;
    }

    @Override
    public ExpressionResult validate(Object actualValue) throws KnownBugException {
        int actualSize = this.actualSize != DEFAULT_LIST.key ? this.actualSize : size(actualValue);

        if (this.originList.size == PRESENT_LIST_SIZE.key && actualSize != MISSED_LIST_SIZE.key) {
            return new ExpressionResult(false, this.originList.list, ORIGIN_SIZE_MESSAGE, null, null, getDescriptions());
        }

        if (originList.size != actualSize) {
            ListWrapper listWrapper = alternativeSizes.get(actualSize);
            if (listWrapper != null) {
                return createExpressionResult(this, listWrapper);
            }

            listWrapper = alternativeSizes.get(PRESENT_LIST_SIZE.key);
            if (listWrapper != null && actualSize != MISSED_LIST_SIZE.key) {
                return createExpressionResult(this, listWrapper);
            }

            Throwable exception = new EPSCommonException("Expected " + originList + " value is not equal actual " + formatSize(actualSize) + " value");
            return new ExpressionResult(false, exception.getMessage(), exception, null, getDescriptions());
        }
        return new ExpressionResult(false, this.originList.list, ORIGIN_SIZE_MESSAGE, null, null, getDescriptions());
    }

    @Override
    public String getCondition() {
        StringBuilder stringBuilder = new StringBuilder()
                .append("Expected: ").append(this.originList).append(", Bugs: [");
        for (Integer alternativeSize : alternativeSizes.keySet()) {
            stringBuilder
                    .append("'").append(alternativeSizes.get(alternativeSize).description).append("'")
                    .append(" = ").append(formatSize(alternativeSize))
                    .append(", ");
        }
        stringBuilder.setLength(stringBuilder.length() - 2);
        return stringBuilder.append(']').toString();
    }

    private static String formatSize(int size) {
        StringBuilder resultBuilder = new StringBuilder("list (");
        if (size == PRESENT_LIST_SIZE.key) {
            resultBuilder.append('*');
        } else if (size == MISSED_LIST_SIZE.key) {
            resultBuilder.append('#');
        } else {
            resultBuilder.append(size);

        }
        return resultBuilder.append(")").toString();
    }

    private Set<BugDescription> getDescriptions() {
        return this.alternativeSizes.values().stream()
                .map(ListWrapper::getDescription)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
    }

    private static ExpressionResult createExpressionResult(BugsListCheckerBuilder bugsCheckerBuilder, ListWrapper listWrapper) {
        return new ExpressionResult(false, listWrapper.list, "Actual " + formatSize(bugsCheckerBuilder.actualSize) + " value сomplies with bug(s): '"
                + listWrapper.description + "'", null, Collections.singleton(listWrapper.description), bugsCheckerBuilder.getDescriptions());
    }

    private boolean isActualEmpty() {
        if (this.actualSize != DEFAULT_LIST.key) {
            throw new EPSCommonException("Actual already set '" + formatSize(this.actualSize) + "'");
        }

        return true;
    }

    private static int size(Object value) {
        return ListValueType.valueOf(value).size(value);
    }

    public static ListWrapper wrap(BugDescription description, Object value) {
        return ListValueType.valueOf(value).wrap(description, value);
    }

    enum ListValueType {
        MISSED_LIST_SIZE(-1),
        PRESENT_LIST_SIZE(-2),
        DEFAULT_LIST(-3),
        OTHER(-4) {
            @Override
            public int size(Object value) {
                if (value instanceof List<?>) {
                    return ((List<?>) value).size();
                }
                throw new IllegalArgumentException("Unsupported type '" + value.getClass().getSimpleName() + "' for Bugs list checker");
            }

            @Override
            public ListWrapper wrap(BugDescription description, Object value) {
                if (value instanceof List<?>) {
                    return new ListWrapper(description, (List<?>) value);
                }
                throw new IllegalArgumentException("Unsupportable type '" + value.getClass().getSimpleName() + "' for Bugs list checker");
            }
        };

        private final int key;

        ListValueType(int key) {
            this.key = key;
        }

        public int size(Object value) {
            return key;
        }

        public ListWrapper wrap(BugDescription description, Object value) {
            return new ListWrapper(description, key);
        }

        public static ListValueType valueOf(Object value) {
            if (value == Convention.CONV_MISSED_OBJECT || value == null) {
                return MISSED_LIST_SIZE;
            } else if (value == Convention.CONV_PRESENT_OBJECT) {
                return PRESENT_LIST_SIZE;
            } else if (value instanceof List<?>) {
                return OTHER;
            }
            throw new IllegalArgumentException("Unsupportable type '" + value.getClass().getSimpleName() + "' for Bugs list checker");
        }
    }

    private static class ListWrapper {
        private final BugDescription description;
        private final List<?> list;
        private final int size;

        private ListWrapper(BugDescription description, List<?> list, int size) {
            this.description = description;
            this.list = list;
            this.size = size;
        }

        ListWrapper(BugDescription description, int size) {
            this(description, null, size);
        }

        ListWrapper(BugDescription description, List<?> value) {
            this(description, value, value.size());
        }

        public BugDescription getDescription() {
            return description;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof ListWrapper)) {
                return false;
            }

            ListWrapper that = (ListWrapper) o;
            return new EqualsBuilder()
                    .append(this.size, that.size)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(this.size)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return formatSize(this.size);
        }
    }
}
