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
package com.exactpro.sf.storage.impl;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.storage.IObjectFlusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

public class ObjectFlusher<T> implements IObjectFlusher<T> {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private static final long JOIN_TIMEOUT = 2000;

    private final Lock monitor = new ReentrantLock(true);
    private final ReadWriteLock sourceMonitor = new ReentrantReadWriteLock();
    private final Condition needFlush = monitor.newCondition();
    private final IFlushProvider<T> provider;
    private final int bufferSize;

    private List<T> objects;
    private FlushTask flushTask;
    private Thread flushThread;

    static int a = 0;
    static Lock lock = new ReentrantLock(true);
    static CyclicBarrier b = new CyclicBarrier(2);
    
    public static void main(String[] args) throws InterruptedException {
        
        Runnable run = new Runnable() {
            
            @Override
            public void run() {
                try {
                    b.await();
                    System.out.println(Thread.currentThread().getId() + " started");
                    for (int i = 0; i < 500; i++) {
                        try {
                            lock.lock();
                            System.out.println(Thread.currentThread().getId() + " " + String.valueOf(a++));
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        
        Thread t1 = new Thread(run);
        Thread t2 = new Thread(run);
        
        t1.start();
        t2.start();
        
        t1.join();
        t2.join();
    }
    
    public ObjectFlusher(IFlushProvider<T> provider, int bufferSize) {
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
        this.bufferSize = bufferSize;
        this.objects = new ArrayList<>(bufferSize + 1);
    }

    @Override
    public void start() {
        try {
            this.sourceMonitor.writeLock().lock();
            if(flushTask != null) {
                throw new EPSCommonException("Cannot start flusher. Flusher is already started");
            }
    
            flushTask = new FlushTask();
            flushThread = new Thread(flushTask, logger.getName());
            flushThread.setDaemon(true);
            flushThread.start();
        } finally {
            this.sourceMonitor.writeLock().unlock();
        }
    }

    @Override
    public void stop() {
        flush();
        
        try {
            this.sourceMonitor.writeLock().lock();

            if(flushTask == null) {
                throw new EPSCommonException("Cannot stop flusher. Flusher is not started");
            }
    
            try {
                flushThread.interrupt();
                flushThread.join(JOIN_TIMEOUT);
    
                if(flushThread.isAlive()) {
                    logger.warn("Flush thread is still alive: {}", flushThread.getName());
                }
            } catch(InterruptedException e) {
                throw new EPSCommonException("Current thread interrupted", e); //FIXME: Throw origin InterruptedException
            } finally {
                flushTask = null;
                flushThread = null;
            }
        } finally {
            this.sourceMonitor.writeLock().unlock();
        }
    }

    @Override
    public void add(T object) {
        try {
            logger.debug("Try lock sourceMonitor read");
            this.sourceMonitor.readLock().lock();
            logger.debug("sourceMonitor locked");
            if(flushTask == null) {
                throw new EPSCommonException("Cannot add object. Flusher is not started");
            }
    
            try {
                logger.debug("Try lock monitor");
                monitor.lock();
                logger.debug("monitor locked");
                objects.add(object);
                logger.debug("Added object: {}", object);
    
                if(objects.size() >= bufferSize) {
                    logger.debug("Buffer is full. Requesting flush");
                    needFlush.signalAll();
                }
            } finally {
                monitor.unlock();
                logger.debug("monitor unlocked");
            }
        } finally {
            this.sourceMonitor.readLock().unlock();
            logger.debug("sourceMonitor read unlocked");
        }
    }

    @Override
    public void flush() {
        try {
            logger.debug("Try lock sourceMonitor read");
            this.sourceMonitor.readLock().lock();
            logger.debug("sourceMonitor read locked");
            if(flushTask == null) {
                throw new EPSCommonException("Cannot request flush. Flusher is not started");
            }
    
            try {
                logger.debug("Try lock monitor");
                monitor.lock();
                logger.debug("monitor locked");
                needFlush.signalAll();
            } finally {
                monitor.unlock();
                logger.debug("monitor unlocked");
            }
    
            synchronized (provider) {
                logger.debug("Provider: synchronized section started");
                try {
                    flushTask.runFlush(false);
                } catch (InterruptedException e) {
                    throw new EPSCommonException("Current thread interrupted", e); //FIXME: Throw origin InterruptedException
                } finally {
                    logger.debug("Provider: synchronized section ended");
                }
            }
        } finally {
            this.sourceMonitor.readLock().unlock();
            logger.debug("sourceMonitor read unlocked");
        }
    }
    
    public class FlushTask implements Runnable {
        private static final long TIMEOUT = 1000;

        @Override
        public void run() {
            try {
                while(true) {
                    Thread.sleep(5);
                    runFlush(true);
                }
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        void runFlush(boolean wait) throws InterruptedException {

            synchronized (provider) {

                List<T> temp;

                try {
                    logger.debug("Try lock monitor");
                    monitor.lock();
                    logger.debug("monitor locked");

                    if (wait) {
                        needFlush.await(TIMEOUT, TimeUnit.MILLISECONDS);
                    }

                    temp = objects;
                    objects = new ArrayList<>(bufferSize + 1);
                } finally {
                    monitor.unlock();
                    logger.debug("monitor unlocked");
                }

                if (!temp.isEmpty()) {
                    try {
                        logger.debug("Flushing {} objects", temp.size());
                        provider.flush(temp);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.error("Failed to flush objects", e);
                    }
                }
            }

        }
    }
}
