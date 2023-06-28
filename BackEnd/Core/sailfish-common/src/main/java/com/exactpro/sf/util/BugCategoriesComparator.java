/*******************************************************************************
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

import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;

public class BugCategoriesComparator implements Comparator<String[]> {
    @Override
    public int compare(String[] array1, String[] array2) {
        CompareToBuilder compareToBuilder = new CompareToBuilder();
        int minSize = Math.min(array1.length, array2.length);
        if (minSize > 0 && array1.length != array2.length) {
            // compare arrays like they have the same size
            compareToBuilder.append(ArrayUtils.subarray(array1, 0, minSize),
                    ArrayUtils.subarray(array2, 0, minSize), String.CASE_INSENSITIVE_ORDER);
            compareToBuilder.append(array1.length, array2.length);
        } else {
            compareToBuilder.append(array1, array2, String.CASE_INSENSITIVE_ORDER);
        }
        return compareToBuilder.toComparison();
    }
}
