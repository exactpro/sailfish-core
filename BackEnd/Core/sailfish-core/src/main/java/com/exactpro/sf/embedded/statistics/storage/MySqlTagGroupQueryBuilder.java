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

public class MySqlTagGroupQueryBuilder extends AbstractTagGroupQueryBuilder {
    @Override
    protected String getTotalTimeField() {
        return "TIMESTAMPDIFF(SECOND, MR.startTime, MR.finishTime)";
    }

    @Override
    protected String formatTimestamp(String value) {
        return " '" + value + "' ";
    }

    @Override
    public MySqlTagGroupQueryBuilder clone() {
        MySqlTagGroupQueryBuilder clone = new MySqlTagGroupQueryBuilder();
        cloneTo(clone);
        return clone;
    }
}
