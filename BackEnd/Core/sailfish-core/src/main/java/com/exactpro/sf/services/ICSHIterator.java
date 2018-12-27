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

import java.util.NoSuchElementException;

public interface ICSHIterator<E> {
    
    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @param timeout amount of time to wait for new elements in case of absence of new elements in cache
     * @return {@code true} if the iteration has more elements
     */
    boolean hasNext(long timeout) throws InterruptedException;
    
    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     * <br><br><b>WARNING</b>: call of this method doesn't trigger cache renewal procedure
     *
     * @return {@code true} if the iteration has more elements
     */
    boolean hasNext();
    
    /**
     * Returns the next element in the iteration.
     * <br><br><b>WARNING</b>: call of this method doesn't trigger cache renewal procedure
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    E next();
    
    /**
     * Updates associated checkpoint with position of next element
     */
    void updateCheckPoint();
}