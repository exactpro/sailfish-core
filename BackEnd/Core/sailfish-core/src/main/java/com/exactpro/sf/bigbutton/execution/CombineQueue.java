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
package com.exactpro.sf.bigbutton.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CombineQueue<T> {
    
    private final BlockingQueue<T> commonQueue = new LinkedBlockingQueue<>();
    
    private final Map<String, BlockingQueue<T>> exeutorToQueue = new HashMap<>();
    
    public void register(String name) {
        if (this.exeutorToQueue.put(name, new LinkedBlockingQueue<>()) != null) {
            throw new IllegalArgumentException("Name '"+ name +"' is already registered");
        }
    }

    public void add(String name, T element) {
        getQueue(name).add(element);
    }
    
    public void add(T element) {
        add(null, element);
    }
    
    public T poll(String name, long timeout, TimeUnit unit) throws InterruptedException {
        return getNamedOrCommonQueue(name).poll(timeout, unit);
    }
    
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return poll(null, timeout, unit);
    }
    
    private BlockingQueue<T> getNamedOrCommonQueue(String name) {
        BlockingQueue<T> result = getQueue(name);
        return result.isEmpty() ? this.commonQueue : result;
    }
    
    private BlockingQueue<T> getQueue(String name) {
        if (name != null) {
            BlockingQueue<T> queue = exeutorToQueue.get(name);
            if (queue != null) {
                return queue;
            } else {
                throw new IllegalArgumentException("Name '"+ name +"' is unregistered");
            }
        }
        
        return this.commonQueue;
    }
}
