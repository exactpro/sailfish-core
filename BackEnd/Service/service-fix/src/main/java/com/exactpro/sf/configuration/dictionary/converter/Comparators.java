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
package com.exactpro.sf.configuration.dictionary.converter;

import java.util.Comparator;

import org.w3c.dom.Element;

public enum Comparators {

    NAME(new Comparator<Element>() {

        @Override
        public int compare(Element o1, Element o2) {
            return o1.getAttribute("name").compareTo(o2.getAttribute("name"));
        }
    }),

    ENUM(new Comparator<Element>() {

        @Override
        public int compare(Element o1, Element o2) {
            return o1.getAttribute("enum").compareTo(o2.getAttribute("enum"));
        }
    }),

    NUMBER(new Comparator<Element>() {

        @Override
        public int compare(Element o1, Element o2) {
            return Integer.valueOf(o1.getAttribute("number")).compareTo(Integer.valueOf(o2.getAttribute("number")));
        }
    });

    private final Comparator<Element> comparator;

    Comparator<Element> getComparator() {
        return comparator;
    }

    Comparators(Comparator<Element> comparator) {
        this.comparator = comparator;
    }
}
