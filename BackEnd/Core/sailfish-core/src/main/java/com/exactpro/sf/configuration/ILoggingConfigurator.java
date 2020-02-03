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
package com.exactpro.sf.configuration;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.services.ServiceName;

/**
 * 
 * This interface defines methods for individual logging of services;
 * You can specify the same appender for several loggers (Codec, IService, ISession, etc.)
 * 
 * @author dmitry.zavodchikov
 *
 */
public interface ILoggingConfigurator {

    /**
     * Return expected logger name for an object
     * @param obj Object which contains logger
     */
    @NotNull
    static String getLoggerName(@NotNull Object obj) {
        return obj.getClass().getName() + '@' + Integer.toHexString(obj.hashCode());
    }

    /**
	 * Creates appender which is associated with a service
     * @param serviceName Service`s name
     * @see ServiceName
	 */
    void createAppender(@NotNull ServiceName serviceName);
	
	/**
	 * Removes appender which is associated with service.
     * @param serviceName Service`s name
     * @see ServiceName
	 */
    void destroyAppender(@NotNull ServiceName serviceName);

    /**
     * Adds service's appender to object's logger
     * @param obj Object which contains logger
     * @param serviceName Service`s name
     * @see ServiceName
     */
	void registerLogger(@NotNull Object obj, @NotNull ServiceName serviceName);

    /**
     * Creates appender which is associated with a service and adds service's appender to object's logger
     * @param serviceName Service`s name
     * @param obj Service object which contains logger
     */
	default void createAndRegister(@NotNull ServiceName serviceName, @NotNull Object obj) {
	    createAppender(serviceName);
	    registerLogger(obj, serviceName);
    }
	
	void enableIndividualAppenders();
	void disableIndividualAppenders();
	
	/**
	 * Returns relative folder (to LOGS folder) path for service log files 
	 * @param serviceName Service name
	 * @return
	 */
	String getLogsPath(@NotNull ServiceName serviceName);

    @NotNull ILoggingConfiguration getConfiguration();
}