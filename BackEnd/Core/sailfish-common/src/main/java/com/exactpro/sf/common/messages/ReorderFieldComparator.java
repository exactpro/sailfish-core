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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReorderFieldComparator implements Comparator<String> {

    private final Map<String, Integer> order = new LinkedHashMap<>();

    public ReorderFieldComparator(List<String> order, Collection<String> names) {

        for (String name : names) {
            int index = order.indexOf(name);

            if(index == -1) {
                continue;
            }

            this.order.put(name, index);
        }

        int counter = order.size();

        for (String name : names) {

            if (!this.order.containsKey(name)) {
                this.order.put(name, counter++);
            }
        }
    }

    @Override
    public int compare(String o1, String o2) {
        return order.get(o1) - order.get(o2);
    }
}
