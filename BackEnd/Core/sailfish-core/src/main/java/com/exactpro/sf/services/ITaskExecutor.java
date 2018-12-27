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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.exactpro.sf.center.IDisposable;

public interface ITaskExecutor extends IDisposable
{
	<T> Future<T> addTask(Callable<T> task);
	Future<?> addTask(Runnable task);

	Future<?> schedule(Runnable task, long delay, TimeUnit timeUnit);

	<V> Future<?> schedule(Callable<V> task, long delay, TimeUnit timeUnit);

	Future<?> addRepeatedTask(Runnable task, long initialDelay, long delay, TimeUnit timeUnit);

	ExecutorService getThreadPool();
}
