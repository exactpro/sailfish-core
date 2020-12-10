/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.testwebgui.structures;

import java.util.ArrayList;
import java.util.List;

import com.exactpro.sf.common.messages.structures.DictionaryComparator.DictionaryPath;
import com.exactpro.sf.common.messages.structures.DictionaryComparator.DistinctionType;
import com.exactpro.sf.common.messages.structures.DictionaryComparator.IDiffListener;

public class DifferenceListener implements IDiffListener {

    List<String> differences = new ArrayList<>();

    @Override
    public void differnce(DistinctionType distinctionType, Object first, Object second,
            DictionaryPath dictionaryPath) {

        StringBuilder differenceStorage = new StringBuilder();

        differenceStorage.append('[');
        if (first != null) {
            differenceStorage.append(first.getClass().getSimpleName()).append(" : ");
        }
        differenceStorage.append(first).append(']')
                .append(' ')
                .append('[');
        if (second != null) {
            differenceStorage.append(second.getClass().getSimpleName()).append(" : ");
        }
        differenceStorage.append(second).append(']');
        if (distinctionType != null) {
            differenceStorage.append(" - ").append(distinctionType);
        }

        differences.add(differenceStorage.toString());
    }

    List<String> getDifferences() {
        return differences;
    }
}


