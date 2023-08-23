/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.storage;

import com.exactpro.sf.storage.impl.BroadcastServiceStorage;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

public class TestBroadcastServiceStorage {

    private static class TestEmptyServiceStorage extends EmptyServiceStorage {
        public Instant lastOlderThan = null;

        @Override
        public void removeServiceEvents(Instant olderThan) {
            lastOlderThan = olderThan;
        }
    }

    @Test
    public void testRemoveServiceEventsInstantWithCustomStorage() {
        TestEmptyServiceStorage emptyServiceStorage = new TestEmptyServiceStorage();
        BroadcastServiceStorage serviceStorage = new BroadcastServiceStorage(emptyServiceStorage);

        Instant testInstant = Instant.now();
        serviceStorage.removeServiceEvents(testInstant);
        Assert.assertEquals(
                "Storage instant does not match the expected",
                testInstant, emptyServiceStorage.lastOlderThan
        );
    }

}
