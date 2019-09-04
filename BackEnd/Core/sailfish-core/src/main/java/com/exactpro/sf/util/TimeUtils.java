/*******************************************************************************
 *  Copyright (c) 2009-2019, Exactpro Systems LLC
 *  www.exactpro.com
 *  Build Software to Test Software
 *
 *  All rights reserved.
 *  This is unpublished, licensed software, confidential and proprietary
 *  information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/

package com.exactpro.sf.util;

import static java.util.concurrent.locks.LockSupport.parkNanos;

import java.util.concurrent.TimeUnit;

/**
 * time utils
 */
public class TimeUtils {
    /**
     * Returns the number of milliseconds elapsed for this JVM, based on System.nanoTime()
     * @return current value of the running JVM's high-resolution
     * time source, in milliseconds.
     */
    public static long jvmTimeInMillis() {
        return TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    /**
     * Robust sleep based on parkNanos() with verification through System.nanoTime()
     * @param millis timeout in milliseconds
     */
    public static void reliableSleep(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException(String.format("Timeout value is negative: [%d]", millis));
        }
        long nanoTime = System.nanoTime();
        long sleepUntil = nanoTime + TimeUnit.MILLISECONDS.toNanos(millis);

        while ((nanoTime = System.nanoTime()) < sleepUntil) {
            parkNanos(sleepUntil - nanoTime);
        }
    }

}