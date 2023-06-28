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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.exactpro.sf.common.util.EPSCommonException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class BugDescription implements Comparable<BugDescription> {

    @JsonProperty("category")
    private final Category category;
    private final String subject;

    @JsonCreator
    public BugDescription(@JsonProperty("subject") String subject, @JsonProperty("category") Category categories, @JsonProperty("categories") Category fake) {

        this.subject = subject;
        this.category = categories;
    }

    public BugDescription(String subject, String... categories) {
        if (StringUtils.isBlank(subject)) {
            throw new EPSCommonException("Description should't be blank");
        }
        this.subject = subject.toLowerCase();
        this.category = new Category(categories);
    }
    
    public String getSubject() {
        return subject;
    }

    @JsonProperty("categories")
    public Category getCategories() {
        return category;
    }
    
    @Override
    public int compareTo(BugDescription o) {
        if(o == null) {
            return 1;
        }
        
        CompareToBuilder builder = new CompareToBuilder();

        builder.append(category, o.category);
        builder.append(subject, o.subject);

        return builder.toComparison();
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }

        if(!(o instanceof BugDescription)) {
            return false;
        }

        BugDescription that = (BugDescription)o;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(category, that.category);
        builder.append(subject, that.subject);

        return builder.isEquals();
    }
    
    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(category);
        builder.append(subject);

        return builder.toHashCode();
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for(String category : category.categories) {
            builder.append(category).append('(');
        }
        builder.append(subject);
        for(int i = 0; i < category.categories.size(); i++) {
            builder.append(')');
        }
        return builder.toString();
    }

    public static class Category implements Comparable<Category> {

        @JsonProperty("categories")
        private final List<String> categories;

        @JsonSetter("categories")
        public void setCategories() {

        }

        @JsonCreator
        public Category(@JsonProperty("categories") String... categories) {
            if (categories.length > 0 && StringUtils.isAnyBlank(categories)) {
                throw new EPSCommonException("Categories should't be blank " + Arrays.toString(categories));
            }
            String[] lowerCaseCategories = categories;
            if (categories.length > 0) {
                lowerCaseCategories = new String[categories.length];
                for (int i = 0; i < categories.length; i++) {
                    lowerCaseCategories[i] = categories[i].toLowerCase();
                }
            }
            this.categories = ImmutableList.copyOf(lowerCaseCategories);
        }

        public List<String> list() {
            return categories;
        }

        @Override
        public int compareTo(Category o) {
            if(o == null) {
                return 1;
            }

            Comparator<String[]> categoriesComparator = new BugCategoriesComparator();

            return categoriesComparator.compare(Iterables.toArray(categories, String.class),
                    Iterables.toArray(o.categories, String.class));
        }

        @Override
        public boolean equals(Object o) {
            if(o == this) {
                return true;
            }

            if(!(o instanceof Category)) {
                return false;
            }

            Category that = (Category)o;
            EqualsBuilder builder = new EqualsBuilder();

            builder.append(categories, that.categories);

            return builder.isEquals();
        }

        @Override
        public int hashCode() {
            HashCodeBuilder builder = new HashCodeBuilder();

            builder.append(categories);

            return builder.toHashCode();
        }

        public String toString() {
            if(categories.isEmpty()) {
                return "";
            }

            StringBuilder builder = new StringBuilder(categories.get(0));

            for(int i = 1; i < categories.size(); i++) {
                builder.append('(').append(categories.get(i));
            }

            for(int i = 0; i < categories.size() - 1; i++) {
                builder.append(')');
            }
            return builder.toString();
        }
    }
}
