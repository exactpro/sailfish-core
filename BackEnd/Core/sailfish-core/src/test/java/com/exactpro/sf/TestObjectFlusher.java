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

import org.junit.Assert;
import org.junit.Test;

import java.io.Closeable;

import com.exactpro.sf.storage.IMeasurable;
import com.exactpro.sf.storage.impl.ObjectFlusher;

public class TestObjectFlusher {

    private static final long timeToRun = 1000 * 10;

    @Test
    public void testOOM() throws Exception {

        ObjectFlusher<OmNomNom> objectFlusher = new ObjectFlusher<>(o -> {
            System.out.println("try to flush objects " + o.size());
            Assert.assertEquals(32, o.size());//check that limit is not exceeded
            System.out.println("objects flushed");
        }, 32*2, 32*1024*1024); //flush thereshold is 64 but limit lower (only 32 objects)

        try (Closeable closeable = objectFlusher::stop) {

            long startTime = System.currentTimeMillis();
            objectFlusher.start();
            while (System.currentTimeMillis() - startTime <= timeToRun) {
                objectFlusher.add(new OmNomNom());
            }
        }

    }

    private class OmNomNom implements IMeasurable {

        private byte[] eatRam = new byte[1024*1024];

        @Override
        public long getSize() {
            return eatRam.length;
        }

        public byte[] getEatRam() {
            return eatRam;
        }

        public void setEatRam(byte[] eatRam) {
            this.eatRam = eatRam;
        }
    }

}
