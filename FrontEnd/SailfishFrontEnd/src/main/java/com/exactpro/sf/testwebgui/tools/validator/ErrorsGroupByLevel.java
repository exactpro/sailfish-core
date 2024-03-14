/******************************************************************************
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
 ******************************************************************************/
package com.exactpro.sf.testwebgui.tools.validator;

import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ErrorsGroupByLevel {
    private final String levelName;
    private final Map<String, Set<DictValidationErrorWrapper>> namedErrors;

    public ErrorsGroupByLevel(DictionaryValidationErrorLevel level, List<DictionaryValidationError> errors) {
        levelName = levelToName(level);
        namedErrors = groupByMessage(errors);
    }

    public String getLevelName() {
        return levelName;
    }

    public List<String> getMessageNames() {
        return new ArrayList<>(namedErrors.keySet());
    }

    public int getErrorsLengthFor(String name) {
        Set<DictValidationErrorWrapper> errors = namedErrors.get(name);
        if(errors == null) {
            throw new IllegalArgumentException("Unknown message's name: " + name);
        }
        return errors.size();
    }

    public List<IndexedItem<DictValidationErrorWrapper>> getErrorsFor(String name) {
        Set<DictValidationErrorWrapper> errorsSet = namedErrors.get(name);
        if(errorsSet == null) {
            throw new IllegalArgumentException("Unknown message's name: " + name);
        }
        AtomicInteger i = new AtomicInteger(0);
        return new ArrayList<>(errorsSet)
                .stream()
                .map(error -> new IndexedItem<>(i.getAndIncrement(), error))
                .collect(Collectors.toList());
    }

    private static String levelToName(DictionaryValidationErrorLevel level) {
        switch (level) {
            case FIELD: return "Fields";
            case MESSAGE: return "Messages";
            case DICTIONARY: return "Dictionaries";
            default:
                throw new IllegalArgumentException("Unhandled type of DictionaryValidationErrorLevel: " + level);
        }
    }

    private static Map<String, Set<DictValidationErrorWrapper>> groupByMessage(List<DictionaryValidationError> errors) {
        Map<String, Set<DictValidationErrorWrapper>> groupsMap = new TreeMap<>();
        for(DictionaryValidationError error: errors) {
            DictValidationErrorWrapper wrapper = new DictValidationErrorWrapper(error);
            groupsMap.computeIfAbsent(wrapper.getMessage(), k -> new TreeSet<>()).add(wrapper);
        }
        return groupsMap;
    }

    public static class IndexedItem<T> {
        private final int index;
        private final T value;

        public IndexedItem(int index, T value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public T getValue() {
            return value;
        }
    }

    public static class DictValidationErrorWrapper implements Comparable<DictValidationErrorWrapper> {
        private static final String TYPE_PREFIX = "ERR_";
        private final DictionaryValidationError validationError;
        private final String message, field, type;

        public DictValidationErrorWrapper(DictionaryValidationError validationError) {
            this.validationError = validationError;
            this.message = ObjectUtils.defaultIfNull(validationError.getMessage(), StringUtils.EMPTY);
            this.field = ObjectUtils.defaultIfNull(validationError.getField(), StringUtils.EMPTY);
            this.type = validate(validationError.getType());
        }

        public String getMessage() {
            return message;
        }

        public String getField() {
            return field;
        }

        public String getError() {
            return validationError.getError();
        }

        public String getType() {
            return type;
        }

        @Override
        public int compareTo(@NotNull DictValidationErrorWrapper o) {
            return Comparator.comparing(DictValidationErrorWrapper::getMessage)
                    .thenComparing(DictValidationErrorWrapper::getField)
                    .thenComparing(DictValidationErrorWrapper::getType)
                    .compare(this, o);
        }

        private static String validate(DictionaryValidationErrorType type) {
            String val = type.name();
            return (val.startsWith(TYPE_PREFIX))? val.substring(TYPE_PREFIX.length()): val;
        }
    }
}
