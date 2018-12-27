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
package com.exactpro.sf.services.itch.multicast;

import com.exactpro.sf.common.messages.IMessage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by alexey.zarovny on 11/28/14.
 */
public class ITCHMulticastCache {

    private final int limit;
    private final Map<Byte, Map<Integer, IMessage>> cacheMap;
    private AtomicInteger size;

    public enum Status {
        REQUEST_ACCEPTED,
        INVALID_MARKET_DATA_GROUP,
        OUT_OF_RANGE,
        REPLAY_UNAVAILABLE
    }

    public ITCHMulticastCache(int limit) {
        cacheMap = new HashMap<>();
        if (limit == 0)
            throw new NullPointerException("Cache limit must not be null");
        this.limit = limit;
        size = new AtomicInteger(0);
    }

    public void add(int seqNumber, IMessage message, byte mdGroup) {
        Map<Integer, IMessage> mdGroupCache;
        mdGroupCache = cacheMap.get(mdGroup);
        if (mdGroupCache == null) {
            mdGroupCache = new TreeMap<>();
            cacheMap.put(mdGroup, mdGroupCache);
        }
        mdGroupCache.put(seqNumber, message);
        if (size.incrementAndGet() >= limit) {
            clearOldMessage();
        }
    }

    private void clearOldMessage() {
        byte candidateMDGroup = -1;
        int candidateSeqNumber = -1;
        for (byte mdGroup : cacheMap.keySet()) {
            int tmp = cacheMap.get(mdGroup).keySet().iterator().next();
            if (candidateSeqNumber > tmp) {
                candidateMDGroup = mdGroup;
                candidateSeqNumber = tmp;
            }
        }
        if (candidateSeqNumber != -1) {
            cacheMap.get(candidateMDGroup).remove(candidateSeqNumber);
        }
        size.decrementAndGet();
    }

    public Status getMessages(Integer startSeqNumber, int quantity, byte mdGroup, List<IMessage> result) {

        Map<Integer, IMessage> mdGroupCache = cacheMap.get(mdGroup);

            if (mdGroupCache == null)
                return Status.INVALID_MARKET_DATA_GROUP;

            if (mdGroupCache.size() < quantity)
                return Status.OUT_OF_RANGE;

            if ((Integer) mdGroupCache.keySet().iterator().next() > startSeqNumber)
                return Status.REPLAY_UNAVAILABLE;

            Iterator<Integer> it = mdGroupCache.keySet().iterator();
            int i = 0;
            while (it.hasNext() && i < quantity) {
                Integer seqNumber = (Integer) it.next();
                if (seqNumber >= startSeqNumber) {
                    result.add((IMessage) mdGroupCache.get(seqNumber));
                    i++;
                }
            }


        if (result.size() < quantity) {
            return Status.OUT_OF_RANGE;
        }
        return Status.REQUEST_ACCEPTED;
    }
}
