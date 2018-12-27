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
package com.exactpro.sf.services.fix.converter.dirty.struct;

import java.util.Comparator;
import java.util.List;

import com.exactpro.sf.services.fix.converter.dirty.FieldConst;

/**
 * header > ordered fields > extra fields > trailer
 * @author nikita.smirnov
 *
 */
public class FieldComparator implements Comparator<Field> {
    private final List<String> order;
    private final boolean containsHeader;
    private final boolean containsTrailer;

    public FieldComparator(List<String> order) {
        this.order = order;
        this.containsHeader = order.contains(FieldConst.HEADER);
        this.containsTrailer = order.contains(FieldConst.TRAILER);
    }

    @Override
    public int compare(Field o1, Field o2) {
        if(!o1.getName().equals(o2.getName())) {
            if((!containsHeader && FieldConst.HEADER.equals(o1.getName())) || (!containsTrailer && FieldConst.TRAILER.equals(o2.getName()))) {
                return -1;
            } else if((!containsHeader && FieldConst.HEADER.equals(o2.getName())) || (!containsTrailer && FieldConst.TRAILER.equals(o1.getName()))) {
                return 1;
            } else {
                int indexO1 = getNthIndex(order, o1.getName(), o1.getIndex() + 1);
                int indexO2 = getNthIndex(order, o2.getName(), o2.getIndex() + 1);

                if(indexO1 == -1 && indexO2 == -1) {
                    return o1.getName().compareTo(o2.getName());
                } else if(indexO1 == -1) {
                    return 1;
                } else if(indexO2 == -1) {
                    return -1;
                } else {
                    return Integer.compare(indexO1, indexO2);
                }
            }
        }

        return 0;
    }

    private int getNthIndex(List<String> list, String value, int n) {
        for(int i = 0; i < list.size(); i++) {
            if(list.get(i).equals(value) && --n == 0) {
                return i;
            }
        }

        return -1;
    }
}
