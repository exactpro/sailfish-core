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

import com.exactpro.sf.common.services.ServiceName;

public interface IService 
{
	
	/**
	 * 
	 * The method initializes service. It is required that implementation of
	 * the method does not allocate any system resources: file descriptors, sockets,
	 * memory. All allocations must be done in start method. This requirement is result of
	 * the early service initialization that currently happens during SF starting.
	 * @param serviceContext
	 * @param serviceMonitor
	 * @param handler
	 * @param settings
	 * @param name
	 * 
	 */
	void init(final IServiceContext serviceContext,
				final IServiceMonitor serviceMonitor,
					final IServiceHandler handler,
						final IServiceSettings settings,
							final ServiceName name);

	void start();

	void dispose();

	IServiceHandler getServiceHandler();
	
	void setServiceHandler(IServiceHandler handler);

	/**
	 * @return String representation of ServiceName (for example: 'default@myservice')
	 */
	String getName();
	
	ServiceName getServiceName();

	ServiceStatus getStatus();
	
	IServiceSettings getSettings();
}
