/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.IMeasurable;
import com.exactpro.sf.storage.impl.ObjectFlusher;

public class TestObjectFlusher {

    private static final int COUNT_ELEMENTS = 32;
    private static final int SIZE_OF_OBJECT = 1024 * 1024;
    private static final int BUFFER_SIZE = COUNT_ELEMENTS * 2;
    private static final int ADD_ELEMENTS_FACTOR = 64;
    private static final long TASK_TIMEOUT = 5;

    private static final Logger logger = LoggerFactory.getLogger(TestObjectFlusher.class);

    @Test
    public void testOOM() throws Exception {

        AtomicInteger consumerCounter = new AtomicInteger();
        ObjectFlusher<OmNomNom> objectFlusher = new ObjectFlusher<>(
                list -> consumerCounter.addAndGet(list.size()),
                COUNT_ELEMENTS * 2,
                COUNT_ELEMENTS * SIZE_OF_OBJECT,
                TASK_TIMEOUT
        );

        objectFlusher.start();

        try (Closeable closeable = objectFlusher::stop) {
            for (int i = 0; i < BUFFER_SIZE * ADD_ELEMENTS_FACTOR; i++) {
                objectFlusher.add(new OmNomNom());
            }
        }

        logger.debug("Counting elements: %d. Max elements: %d", consumerCounter.get(), BUFFER_SIZE * ADD_ELEMENTS_FACTOR);
        System.out.println(consumerCounter.get());
        Assert.assertTrue(consumerCounter.get() < BUFFER_SIZE * ADD_ELEMENTS_FACTOR);
    }

    private class OmNomNom implements IMeasurable {

        @Override
        public long getSize() {
            return SIZE_OF_OBJECT;
        }
    }

}
