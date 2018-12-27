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
package com.exactpro.sf.comparison.conversion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ConvertingComparator implements Comparator<Object> {
    private final static List<Class<?>> PRIORITIES = new ArrayList<>();

    static {
        PRIORITIES.add(String.class);
        PRIORITIES.add(Boolean.class);
        PRIORITIES.add(Byte.class);
        PRIORITIES.add(Short.class);
        PRIORITIES.add(Integer.class);
        PRIORITIES.add(Long.class);
        PRIORITIES.add(Float.class);
        PRIORITIES.add(Double.class);
        PRIORITIES.add(BigDecimal.class);
        PRIORITIES.add(Character.class);
        PRIORITIES.add(LocalDate.class);
        PRIORITIES.add(LocalTime.class);
        PRIORITIES.add(LocalDateTime.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(Object o1, Object o2) {
        if(o1 == o2) {
            return 0;
        } else if(o1 == null || o2 == null) {
            return o1 == null ? -1 : 1;
        }

        Class<?> class1 = o1.getClass();
        Class<?> class2 = o2.getClass();

        int priority1 = PRIORITIES.indexOf(class1);
        int priority2 = PRIORITIES.indexOf(class2);

        if(priority1 == -1 ^ priority2 == -1) {
            throw new ClassCastException(String.format("Cannot compare %s (%s) to %s (%s)", class1, o1, class2, o2));
        }

        int maxPriority = Math.max(priority1, priority2);

        if(maxPriority != -1) {
            Class<?> clazz = PRIORITIES.get(maxPriority);
            o1 = MultiConverter.convert(o1, clazz);
            o2 = MultiConverter.convert(o2, clazz);
        }

        if(o1.getClass() == o2.getClass() && o1 instanceof Comparable<?>) {
            return ((Comparable<Object>)o1).compareTo(o2);
        }

        throw new UnsupportedOperationException(String.format("Cannot compare %s (%s) to %s (%s)", class1, o1, class2, o2));
    }
}
