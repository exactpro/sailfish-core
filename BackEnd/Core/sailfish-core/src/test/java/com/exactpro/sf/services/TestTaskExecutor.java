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

import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nikita.smirnov
 *
 */
public class TestTaskExecutor {

	@BeforeClass
	public static void init() {
		ConsoleAppender ca = new ConsoleAppender();
		ca.setWriter(new OutputStreamWriter(System.out));
		ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
		BasicConfigurator.configure(ca);
	}
	
	@Test
	public void testException() throws InterruptedException {
		ITaskExecutor taskExecutor = new TaskExecutor();
		Task task = new Task();
		
		try {
			boolean error = false;
			try {
				taskExecutor.addTask((Runnable)task).get();
			} catch (ExecutionException e) {
				Assert.assertEquals("Test", e.getCause().getMessage());
				error = true;
			}
			Assert.assertEquals("Exception run", true, error);
			
			error = false;
			try {
				taskExecutor.schedule((Runnable)task, 10, TimeUnit.MILLISECONDS).get();
			} catch (ExecutionException e) {
				Assert.assertEquals("Test", e.getCause().getMessage());
				error = true;
			}
			Assert.assertEquals("Exception run with delay", true, error);
			
			error = false;
			try {
				taskExecutor.addTask((Callable<String>)task).get();
			} catch (ExecutionException e) {
				Assert.assertEquals("Test", e.getCause().getMessage());
				error = true;
			}
			Assert.assertEquals("Exception call", true, error);
			
			error = false;
			try {
				taskExecutor.schedule((Callable<String>)task, 10, TimeUnit.MILLISECONDS).get();
			} catch (ExecutionException e) {
				Assert.assertEquals("Test", e.getCause().getMessage());
				error = true;
			}
			Assert.assertEquals("Exception call with delay", true, error);
			
			error = false;
			task.setMaxRun(3);
			try {
				taskExecutor.addRepeatedTask((Runnable)task, 10, 10, TimeUnit.MILLISECONDS).get();
			} catch (ExecutionException e) {
				Assert.assertEquals("Test", e.getCause().getMessage());
				error = true;
			}
			Assert.assertEquals(3, task.getRunCount());
			Assert.assertEquals("Exception repeate", true, error);
			
		} finally {
			taskExecutor.dispose();
		}
	}
	
	private class Task implements Runnable, Callable<String> { 
		
		private volatile int maxRun = 0;
		private volatile int runCount = 0;
		
		@Override
		public void run() {
		    System.out.println(Thread.currentThread().getName());
			if (++this.runCount >= this.maxRun) {
				throw new RuntimeException("Test");
			}
		}

		@Override
		public String call() throws Exception {
		    System.out.println(Thread.currentThread().getName());
			throw new RuntimeException("Test");
		}

		public int getRunCount() {
			return this.runCount;
		}

		public void setMaxRun(int value) {
			this.maxRun = value;
			this.runCount = 0;
		}
		
		@Override
		public String toString() {
			return "TestTask";
		}
	}
}
