/******************************************************************************
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactpro.sf.actions;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class FIXMatrixUtilTest {

    @Test
    public void extractSeqNum() {
        checkSeqNumFromText("Sequence Number (1) < expected (3893)", 3893);
        checkSeqNumFromText("Wrong sequence number! Too small to recover. Received: 1, Expected: 237>", 237);
        checkSeqNumFromText("MsgSeqNum(34) too low, expecting 756 but received 358", 756);
    }

    private void checkSeqNumFromText(String text, long expected) {
        int seqNum = FIXMatrixUtil.extractSeqNum(text);
        Assert.assertEquals(expected, seqNum);
    }
}