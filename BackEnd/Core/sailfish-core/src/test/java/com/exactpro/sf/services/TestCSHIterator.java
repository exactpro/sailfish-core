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
package com.exactpro.sf.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.common.util.EPSCommonException;

public class TestCSHIterator {
    private final static long UPDATE_INTERVAL = 100L;
    private final static List<Integer> SOURCE = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Test
    public void testTimeout() throws InterruptedException {
        for(int i = 1; i < SOURCE.size(); i++) {
            testStep(i * UPDATE_INTERVAL, 0);
        }
    }

    @Test
    public void testTimeoutWithCheckpoint() throws InterruptedException {
        for(int i = 1; i < SOURCE.size(); i++) {
            testStep(i * UPDATE_INTERVAL, i);
        }
    }

    private void testStep(long timeout, int offset) throws InterruptedException {
        CSHArrayList<Integer> target = new CSHArrayList<>();
        CheckPoint checkPoint = new CheckPoint(true);

        if(offset > 0) {
            target.putCheckPoint(checkPoint, offset);
            target.addAll(SOURCE.subList(SOURCE.size() - offset, SOURCE.size()));
        }

        Thread updateThread = new Thread(new UpdateTask(SOURCE, target, UPDATE_INTERVAL));
        CSHIterator<Integer> it = new CSHIterator<>(target, checkPoint);
        List<Integer> result = new ArrayList<>();
        long waitUntil = System.currentTimeMillis() + timeout;

        updateThread.start();

        while(it.hasNext(waitUntil - System.currentTimeMillis())) {
            result.add(it.next());
        }

        it.updateCheckPoint();
        updateThread.join(timeout);

        if(offset > 0) {
            Assert.assertEquals(result.size() + offset, target.getIndex(checkPoint));
        }

        Assert.assertEquals(timeout / UPDATE_INTERVAL, result.size());
        Assert.assertEquals(SOURCE.subList(0, result.size()), result);
    }

    private class UpdateTask implements Runnable {
        private final Iterator<Integer> source;
        private final List<Integer> target;
        private final long interval;

        public UpdateTask(List<Integer> source, List<Integer> target, long interval) {
            this.source = source.iterator();
            this.target = target;
            this.interval = interval;
        }

        @Override
        public void run() {
            while(source.hasNext()) {
                try {
                    Thread.sleep(interval / 2);

                    synchronized(target) {
                        target.add(source.next());
                        target.notifyAll();
                    }

                    Thread.sleep(interval / 2);
                } catch(InterruptedException e) {
                    throw new EPSCommonException(e);
                }
            }
        }
    }
}
