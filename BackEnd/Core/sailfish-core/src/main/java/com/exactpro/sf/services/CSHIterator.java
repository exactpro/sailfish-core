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

import java.util.List;
import java.util.NoSuchElementException;

import com.exactpro.sf.aml.script.CheckPoint;

public class CSHIterator<E> implements ICSHIterator<E> {
    private final CSHArrayList<E> list;
    private final CheckPoint checkPoint;

    private List<E> cache;
    private int offset;
    private int index = 0;

    public CSHIterator(CSHArrayList<E> list, CheckPoint checkPoint) {
        this.list = list;
        this.checkPoint = checkPoint;

        synchronized(list) {
            offset = list.getIndex(checkPoint);
            cache = list.subList(offset);
        }
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.ICSHIterator#hasNext(long)
     */
    public boolean hasNext(long timeout) throws InterruptedException {
        if(!hasNext()) {
            if(timeout <= 0) {
                return false;
            }

            long waitUntil = timeout + System.currentTimeMillis();

            synchronized(list) {
                int listSize = cache.size() + offset;

                while(listSize == list.size() && waitUntil > System.currentTimeMillis()) {
                    list.wait(waitUntil - System.currentTimeMillis());
                }

                if(listSize < list.size()) {
                    cache.addAll(list.subList(listSize));
                }
            }
        }

        return hasNext();
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.ICSHIterator#hasNext()
     */
    public boolean hasNext() {
        return cache.size() > index;
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.ICSHIterator#next()
     */
    public E next() {
        if(!hasNext()) {
            throw new NoSuchElementException();
        }

        return cache.get(index++);
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.ICSHIterator#updateCheckPoint()
     */
    public void updateCheckPoint() {
        if(checkPoint != null && checkPoint.isSmart()) {
            synchronized(list) {
                list.putCheckPoint(checkPoint, index + offset);
            }
        }
    }
}
