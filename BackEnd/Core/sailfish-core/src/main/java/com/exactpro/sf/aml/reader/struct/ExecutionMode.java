/******************************************************************************
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactpro.sf.aml.reader.struct;

import com.exactpro.sf.aml.AMLLangConst;

public enum ExecutionMode {
    EXECUTABLE(AMLLangConst.YES),
    OPTIONAL(AMLLangConst.OPTIONAL),
    NOT_EXECUTABLE(AMLLangConst.NO);

    private final String value;

    ExecutionMode(String value) {
        this.value = value;
    }

    public static ExecutionMode from(String value) {
        for (ExecutionMode executionMode : ExecutionMode.values()) {
            if (executionMode.value.equalsIgnoreCase(value)) {
                return executionMode;
            }
        }
        return EXECUTABLE;
    }
}
