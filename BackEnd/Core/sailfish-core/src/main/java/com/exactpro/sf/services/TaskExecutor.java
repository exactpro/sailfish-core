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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutor implements ITaskExecutor
{
	private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);
	private static final int POSSIBLE_DELAY = 10;
	private static final String EXECUTOR_PREFIX = TaskExecutor.class.getSimpleName() + '-';
	private static final AtomicLong taskCounter = new AtomicLong();
	
	private final ExecutorService threadPool; 
	private final ScheduledExecutorService scheduledThreadPool;

	public TaskExecutor() {
        this(0, 350, Runtime.getRuntime().availableProcessors() * 2);
    }
	
	public TaskExecutor(int minThreads, int maxThreads, int scheduledThreads) {
	    threadPool = new ThreadPoolExecutor(minThreads, maxThreads, 30L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	    scheduledThreadPool = Executors.newScheduledThreadPool(scheduledThreads);
	}

	@Override
	public synchronized <T> Future<T> addTask(Callable<T> task)
	{
		return threadPool.submit(new TaskCallableWrapper<T>(task));
	}

	@Override
	public synchronized Future<?> addTask(Runnable task)
	{
		return threadPool.submit(new TaskRunnableWrapper(task));
	}

    @Override
	public synchronized Future<?> schedule(Runnable task, long delay, TimeUnit timeUnit) {
		return scheduledThreadPool.schedule(new TaskRunnableWrapper(task, timeUnit.toMillis(delay)), delay, timeUnit);
	}

    @Override
    public synchronized <V> Future<?> schedule(Callable<V> task, long delay, TimeUnit timeUnit) {
        return scheduledThreadPool.schedule(new TaskCallableWrapper<V>(task, timeUnit.toMillis(delay)), delay, timeUnit);
    }

	@Override
	public synchronized Future<?> addRepeatedTask(Runnable task, long initialDelay, long delay, TimeUnit timeUnit) {
		return scheduledThreadPool.scheduleWithFixedDelay(new TaskRepeatableWrapper(task, initialDelay, delay), initialDelay, delay, timeUnit);
	}

	@Override
	public void dispose()
	{
		if(!this.threadPool.isShutdown() || !this.scheduledThreadPool.isShutdown()) {
			logger.info("TaskExecutor disposing started...");
			
			try {
				if (!this.threadPool.isShutdown()) {
					this.threadPool.shutdownNow();
					if (!this.threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                        logger.warn("Some Threads from cachedThreadPool remained alive");
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
	
			try {
				if (!this.scheduledThreadPool.isShutdown()) {
					this.scheduledThreadPool.shutdownNow();
					if (!this.scheduledThreadPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                        logger.warn("Some Threads from scheduledThreadPool remained alive");
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			
			if(!threadPool.isTerminated() || !scheduledThreadPool.isTerminated()) {
				logger.error("Not all tasks have been completed in timeout");
			}
		}
	}

	@Override
	public ExecutorService getThreadPool() {
		return threadPool;
	}

	private void setThreadName(Thread thread, String name) {
        try {
            thread.setName(name);
        } catch (Exception e) {
            logger.warn("Failed to set the thread name.", e);
        }
    }
	
	private String createThreadName(String taskName) {
	    return new StringBuilder(EXECUTOR_PREFIX)
	        .append(taskName).append('-')
	        .append(taskCounter.incrementAndGet()).toString();
	}
	
	/**
	 * Logging exception that were thrown in the task and
	 * checking delay time between creating and running task.
	 */
	private class TaskRunnableWrapper implements Runnable {

		protected final Runnable wrappedTask;
		protected final String threadName;
		protected long createTime;

		public TaskRunnableWrapper(Runnable wrappedTask, long delay) {
			this.wrappedTask = wrappedTask;
			this.threadName = createThreadName(wrappedTask.toString());
			this.createTime = System.currentTimeMillis() + delay;
		}
		
		public TaskRunnableWrapper(Runnable wrappedTask) {
			this(wrappedTask, 0);
		}

		@Override
		public void run() {
			long timeInQueue = System.currentTimeMillis() - this.createTime; 
			
			String oldName = null;
            Thread currentThread = null;
            
            if (this.threadName != null) {
                currentThread = Thread.currentThread();
                oldName = currentThread.getName();
            
                setThreadName(currentThread, this.threadName);
            }
            
            try {
                executeTask(timeInQueue);
            } finally {
                if (currentThread != null && this.threadName != null) {
                    setThreadName(currentThread, oldName);
                }
            }
		}
		
		private void executeTask(long timeInQueue) {
            if(timeInQueue > POSSIBLE_DELAY) {
                logger.warn("Task [{}] delay exceed on {}", wrappedTask, timeInQueue);
            }
            
            try {
                wrappedTask.run();
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
                throw e;
            }
		}
	}
	
	/**
	 * Logging exception that were thrown in the task and
	 * checking delay time between creating and running task.
	 */
	private class TaskCallableWrapper<T> implements Callable<T> {

		protected final Callable<T> wrappedTask;
		protected final String threadName;
		protected long createTime;

		public TaskCallableWrapper(Callable<T> wrappedTask, long delay) {
			this.wrappedTask = wrappedTask;
			this.threadName = createThreadName(wrappedTask.toString());
			this.createTime = System.currentTimeMillis() + delay;
		}

		public TaskCallableWrapper(Callable<T> wrappedTask) {
			this(wrappedTask, 0);
		}
		
		@Override
		public T call() throws Exception {
		    long timeInQueue = System.currentTimeMillis() - this.createTime; 
            
            String oldName = null;
            Thread currentThread = null;
            
            if (this.threadName != null) {
                currentThread = Thread.currentThread();
                oldName = currentThread.getName();
            
                setThreadName(currentThread, this.threadName);
            }
            
            try {
                return executeTask(timeInQueue);
            } finally {
                if (currentThread != null && this.threadName != null) {
                    setThreadName(currentThread, oldName);
                }
            }
		}
		
		private T executeTask(long timeInQueue) throws Exception {
            if(timeInQueue > POSSIBLE_DELAY) {
                logger.warn("Task [{}] delay exceed on {}", wrappedTask, timeInQueue);
            }
            
            return wrappedTask.call();
        }
	}

	/**
	 * Logging exception that were thrown in the task and
	 * checking delay time between creating and running task.
	 */
	private class TaskRepeatableWrapper extends TaskRunnableWrapper {

		private final long delay;
		
		public TaskRepeatableWrapper(Runnable wrappedTask, long initialDelay, long delay) {
			super(wrappedTask, initialDelay);
			this.delay = delay;
		}

		@Override
		public void run() {
			super.run();
			this.createTime = System.currentTimeMillis() + this.delay;
		}
	}
}
