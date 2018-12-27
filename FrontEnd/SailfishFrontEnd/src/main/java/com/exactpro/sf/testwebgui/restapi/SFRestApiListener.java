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
package com.exactpro.sf.testwebgui.restapi;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFRestApiListener implements ApplicationEventListener {

	private static final Logger logger = LoggerFactory.getLogger(SFRestApiListener.class);

    @Override
    public void onEvent(ApplicationEvent applicationEvent) {
        switch (applicationEvent.getType()) {
            case INITIALIZATION_FINISHED:
                logger.info("Jersey application started.");
                break;
            case INITIALIZATION_START:
            	logger.info("Jersey INITIALIZATION_START");
            	break;
            case DESTROY_FINISHED:
            	logger.info("Jersey DESTROY_FINISHED");
            	break;
            case RELOAD_FINISHED:
            	logger.info("Jersey RELOAD_FINISHED");
            	break;
            case INITIALIZATION_APP_FINISHED:
                logger.info("Jersey INITIALIZATION_APP_FINISHED");
                break;
            default:
                break;
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return new MyRequestEventListener();
    }


    public static class MyRequestEventListener implements RequestEventListener {
        private volatile long methodStartTime;

        @Override
        public void onEvent(RequestEvent requestEvent) {
            switch (requestEvent.getType()) {
                case RESOURCE_METHOD_START:
                    methodStartTime = System.currentTimeMillis();
                    break;
                case RESOURCE_METHOD_FINISHED:
                    long methodExecution = System.currentTimeMillis() - methodStartTime;
                    final String methodName = requestEvent.getUriInfo().getMatchedResourceMethod().getInvocable().getHandlingMethod().getName();
                    logger.debug("Method '{}' executed. Processing time: {} ms", methodName, methodExecution);
                    break;
                case ON_EXCEPTION:
                	logger.error("Problem with API request", requestEvent.getException());
                	break;
                default:
                    break;
            }
        }
    }
}
