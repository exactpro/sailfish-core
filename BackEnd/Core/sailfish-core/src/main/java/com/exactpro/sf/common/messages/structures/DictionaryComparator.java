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
package com.exactpro.sf.common.messages.structures;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

public class DictionaryComparator {

    private final boolean typedCheck;
    
    public DictionaryComparator(boolean typedCheck) {
        this.typedCheck = typedCheck;
    }
    
    public DictionaryComparator() {
        this(true);
    }
    
    public static void main(String[] args) {
        try {
            Path one = Paths.get(args[0]).toAbsolutePath();
            Path two = Paths.get(args[1]).toAbsolutePath();

            boolean compareOrder;
            try{
                compareOrder = BooleanUtils.toBoolean(args[2]);
            }catch (IndexOutOfBoundsException e){
                compareOrder = false;
            }

            IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
            DictionaryComparator comparator = new DictionaryComparator();
            try (InputStream oneIn = Files.newInputStream(one);
                    InputStream twoIn = Files.newInputStream(two)) {
                comparator.compare(DictionaryComparator::displayDifference,
                        loader.load(oneIn), loader.load(twoIn), compareOrder, false, true);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayDifference(DistinctionType distinctionType, Object first, Object second, DictionaryPath dictionaryPath) {
        StringBuilder builder = new StringBuilder(dictionaryPath.toString()).append(' ')
                .append('[');
        if (first != null) {
            builder.append(first.getClass().getSimpleName()).append(" : ");
        }
        builder.append(first).append(']')
                .append(' ')
                .append('[');
        if (second != null) {
            builder.append(second.getClass().getSimpleName()).append(" : ");
        }
        builder.append(second).append(']');
        if (distinctionType != null) {
            builder.append(" - ").append(distinctionType);
        }
        System.out.println(builder.toString());
    }

    public void compare(IDiffListener listener, IDictionaryStructure one, IDictionaryStructure two, boolean compareOrder, boolean checkByFirst, boolean deepCheck) {
        DictionaryPath path = null;
        equal(listener, one.getNamespace(), two.getNamespace(), path, DistinctionType.Namespace);
        path = new DictionaryPath(one.getNamespace());
        equal(listener, one.getDescription(), two.getDescription(), path, DistinctionType.Description);

        Iterable<? extends IFieldStructure> iterable;
        if (checkByFirst) {
            iterable = one.getMessageStructures();
        } else {
            iterable = Iterables.concat(one.getMessageStructures(), two.getMessageStructures());
        }
        Set<String> names = getNames(iterable);
        for (String name : names) {
            compare(listener, one.getMessageStructure(name), two.getMessageStructure(name), new DictionaryPath(path).setMessage(name),
                    compareOrder, checkByFirst, deepCheck, EntityCheckType.ALL);

        }

        if (checkByFirst) {
            iterable = one.getFieldStructures();
        } else {
            iterable = Iterables.concat(one.getFieldStructures(), two.getFieldStructures());
        }
        names = getNames(iterable);
        for (String name : names) {
            compare(listener, one.getFieldStructure(name), two.getFieldStructure(name), new DictionaryPath(path).setField(name),
                    compareOrder, checkByFirst, deepCheck, EntityCheckType.ALL);
        }
    }

    public void compare(IDiffListener listener, IFieldStructure one, IFieldStructure two, DictionaryPath path, boolean compareOrder, boolean checkByFirst, boolean deepCheck, EntityCheckType checkType) {
        if (one == null || two == null) {
            callListener(listener, one != null ? "exists" : null,
                    two != null ? "exists" : null, path, DistinctionType.Existing);
            return;
        }

        if (!deepCheck) {
            return;
        }

        equal(listener, one.getDescription(), two.getDescription(), path, DistinctionType.Description);
        equal(listener, safeDefaultValue(one), safeDefaultValue(two), path, DistinctionType.DefaultValue);
        equal(listener, safeJavaType(one), safeJavaType(two), path, DistinctionType.JavaType);
        equal(listener, safeIsCollection(one), safeIsCollection(two), path, DistinctionType.IsCollection);
        equal(listener, safeIsRequired(one), safeIsRequired(two), path, DistinctionType.IsRequired);
        equal(listener, safeIsServiceName(one), safeIsServiceName(two), path, DistinctionType.IsServiceName);
        equal(listener, one.isComplex(), two.isComplex(), path, DistinctionType.IsComplex);
        equal(listener, one.isEnum(), two.isEnum(), path, DistinctionType.IsEnum);

        Set<String> names = new TreeSet<>();
        names.addAll(ObjectUtils.defaultIfNull(one.getAttributeNames(), Collections.emptyList()));
        if (!checkByFirst) {
            names.addAll(ObjectUtils.defaultIfNull(two.getAttributeNames(), Collections.emptyList()));
        }
        for (String name : names) {
            equal(listener, one.getAttributeValueByName(name), two.getAttributeValueByName(name), new DictionaryPath(path).setAttribute(name), DistinctionType.AttributeValue);
        }

        names.clear();
        Map<String, IAttributeStructure> oneValueMap = safeValues(one);
        Map<String, IAttributeStructure> twoValueMap = safeValues(two);
        names.addAll(oneValueMap.keySet());
        if (!checkByFirst) {
            names.addAll(twoValueMap.keySet());
        }
        for (String name : names) {
            equal(listener, safeCastValue(oneValueMap.get(name)), safeCastValue(twoValueMap.get(name)), new DictionaryPath(path).setValue(name), DistinctionType.EnumValue);
        }

        if (checkType != EntityCheckType.FIELD) {
            names.clear();
            names.addAll(safeFieldNames(one));
            if (!checkByFirst) {
                names.addAll(safeFieldNames(two));
            }
            int subFieldIndex;
            for (String name : names) {
                if (compareOrder) {
                    subFieldIndex = safeFieldIndex(one, two, name);
                    equal(listener, safeField(one, subFieldIndex), safeField(two, subFieldIndex),
                            new DictionaryPath(path).setValue(name), DistinctionType.FieldOrder);
                }
                compare(listener, safeField(one, name), safeField(two, name),
                        new DictionaryPath(path).setField(path.field != null ? path.field + " -> " + name : name), compareOrder, checkByFirst,
                        checkType == EntityCheckType.ALL && deepCheck,
                        checkType == EntityCheckType.ALL ? checkType : EntityCheckType.FIELD);
            }
        }
    }

    private void equal(IDiffListener listener, Object one, Object two, DictionaryPath path, DistinctionType distinctionType) {
        if (!Objects.equal(one, two)) {
            callListener(listener, one, two, path, distinctionType);
        }
    }

    private void callListener(IDiffListener listener, Object one, Object two, DictionaryPath path, DistinctionType distinctionType) {
        listener.differnce(distinctionType, one, two, path);
    }

    private Set<String> getNames(Iterable<? extends IFieldStructure> collection) {
        Set<String> names = new TreeSet<>();
        for (IFieldStructure fieldStructure : collection) {
            names.add(fieldStructure.getName());
        }
        return names;
    }

    private Object safeDefaultValue(IFieldStructure fieldStructure) {
        try {
            return tryToCast(fieldStructure, fieldStructure.getDefaultValue());
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private Boolean safeIsCollection(IFieldStructure fieldStructure) {
        try {
            return fieldStructure.isCollection();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private Boolean safeIsRequired(IFieldStructure fieldStructure) {
        try {
            return fieldStructure.isRequired();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private Boolean safeIsServiceName(IFieldStructure fieldStructure) {
        try {
            return fieldStructure.isServiceName();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private JavaType safeJavaType(IFieldStructure fieldStructure) {
        try {
            return fieldStructure.getJavaType();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private Object safeCastValue(IAttributeStructure attributeStructure) {
        if (attributeStructure != null) {
            if (this.typedCheck) {
                return attributeStructure.getCastValue();
            } else {
                return attributeStructure.getValue();
            }
        }
        return null;
    }

    private IFieldStructure safeField(IFieldStructure fieldStructure, String subFiledName) {
        try {
            return fieldStructure.getField(subFiledName);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private String safeField(IFieldStructure fieldStructure, int subFieldIndex){
        try {
            return fieldStructure.getFieldNames().get(subFieldIndex);
        } catch (UnsupportedOperationException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private int safeFieldIndex(IFieldStructure fieldStructureOne, IFieldStructure fieldStructureTwo, String subFiledName){
        try {
            int result = fieldStructureOne.getFieldNames().indexOf(subFiledName);
            if(result == -1){
                result = fieldStructureTwo.getFieldNames().indexOf(subFiledName);
            }
            return result;
        } catch (UnsupportedOperationException e) {
            return -1;
        }
    }

    private Map<String, IAttributeStructure> safeValues(IFieldStructure fieldStructure) {
        try {
            return fieldStructure.getValues();
        } catch (UnsupportedOperationException e) {
            return Collections.emptyMap();
        }
    }

    private List<String> safeFieldNames(IFieldStructure fieldStructure) {
        try {
            return ObjectUtils.defaultIfNull(fieldStructure.getFieldNames(), Collections.<String>emptyList());
        } catch (UnsupportedOperationException e) {
            return Collections.emptyList();
        }
    }

    private Object tryToCast(IFieldStructure fieldStructure, Object value) {
        if (!this.typedCheck && value instanceof String) {
            value = StructureUtils.castValueToJavaType((String)value, fieldStructure.getJavaType());
        }
        return value;
    }

    public static class DictionaryPath {
        private String dictionary;
        private String message;
        private String field;
        private String attribute;
        private String value;

        public DictionaryPath(String dictionary, String message, String field, String attribute, String value) {
            this.dictionary = dictionary;
            this.message = message;
            this.field = field;
            this.attribute = attribute;
            this.value = value;
        }

        public DictionaryPath(DictionaryPath prototype) {
            this(prototype.dictionary, prototype.message, prototype.field, prototype.attribute, prototype.value);
        }

        public DictionaryPath(String dictionary, String message, String field, String attribute) {
            this(dictionary, message, field, attribute, null);
        }

        public DictionaryPath(String dictionary, String message, String field) {
            this(dictionary, message, field, null);
        }

        public DictionaryPath(String dictionary, String message) {
            this(dictionary, message, null);
        }

        public DictionaryPath(String dictionary) {
            this(dictionary, null);
        }

        public DictionaryPath setDictionary(String dictionary) {
            this.dictionary = dictionary;
            return this;
        }

        public DictionaryPath setMessage(String message) {
            this.message = message;
            return this;
        }

        public DictionaryPath setField(String field) {
            this.field = field;
            return this;
        }

        public DictionaryPath setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        public DictionaryPath setValue(String value) {
            this.value = value;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public String getField() {
            return field;
        }

        public String getAttribute() {
            return attribute;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder()
                    .append(StringUtils.center(ObjectUtils.defaultIfNull(dictionary, StringUtils.EMPTY), 20)).append('.')
                    .append(StringUtils.center(ObjectUtils.defaultIfNull(message, StringUtils.EMPTY), 30)).append('.')
                    .append(StringUtils.center(ObjectUtils.defaultIfNull(field, StringUtils.EMPTY), 30)).append('.')
                    .append(StringUtils.center(ObjectUtils.defaultIfNull(attribute, StringUtils.EMPTY), 15)).append('.')
                    .append(StringUtils.center(ObjectUtils.defaultIfNull(value, StringUtils.EMPTY), 40));
            return builder.toString();
        }
    }

    public interface IDiffListener {
        void differnce(DistinctionType distinctionType, Object first, Object second, DictionaryPath dictionaryPath);
    }

    public enum EntityCheckType {
        ALL,
        MESSAGE,
        FIELD
    }

    public enum DistinctionType {
        Existing,
        Namespace,
        Description,
        DefaultValue,
        JavaType,
        IsCollection,
        IsRequired,
        IsServiceName,
        IsComplex,
        IsEnum,
        AttributeValue,
        EnumValue,
        FieldOrder
    }
}
