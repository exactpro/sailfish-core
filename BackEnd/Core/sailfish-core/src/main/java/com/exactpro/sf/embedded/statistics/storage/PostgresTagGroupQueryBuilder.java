/******************************************************************************
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactpro.sf.embedded.statistics.storage;

public class PostgresTagGroupQueryBuilder extends AbstractTagGroupQueryBuilder {
    @Override
    protected String getTotalTimeField() {
        return "extract ('epoch' from MR.finishTime - MR.startTime)";
    }

    @Override
    protected String formatTimestamp(String value) {
        return " timestamp '" + value + "' ";
    }

    @Override
    public PostgresTagGroupQueryBuilder clone() {
        PostgresTagGroupQueryBuilder clone = new PostgresTagGroupQueryBuilder();
        cloneTo(clone);
        return clone;
    }
}
