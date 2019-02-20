/*
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 */
package com.exactpro.sf.common.messages;

import com.exactpro.sf.common.messages.structures.IFieldStructure;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReorderFieldComparator implements Comparator<IFieldStructure> {

    private final Map<IFieldStructure, Integer> order = new LinkedHashMap<>();

    public ReorderFieldComparator(List<String> order, List<IFieldStructure> fields) {

        for (IFieldStructure fieldStructure : fields) {
            int index = order.indexOf(fieldStructure.getName());

            if (index == -1) continue;

            this.order.put(fieldStructure, index);
        }

        int counter = order.size();

        for (IFieldStructure fieldStructure : fields) {

            if (!this.order.containsKey(fieldStructure)) {
                this.order.put(fieldStructure, counter++);
            }
        }
    }

    @Override
    public int compare(IFieldStructure o1, IFieldStructure o2) {
        return this.order.get(o1) - this.order.get(o2);
    }
}
