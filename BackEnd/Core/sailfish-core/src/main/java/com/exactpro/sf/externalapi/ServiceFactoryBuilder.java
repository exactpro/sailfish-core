/*******************************************************************************
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

package com.exactpro.sf.externalapi;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.exactpro.sf.common.impl.messages.StrictMessageWrapper;
import com.exactpro.sf.configuration.suri.SailfishURIException;

/**
 * Builder for creating {@link ServiceFactory}
 */
public class ServiceFactoryBuilder {

    private final File[] workspaceLayers;
    private int minThreads = 0;
    private int maxThreads = 350;
    private int scheduledThreads = Runtime.getRuntime().availableProcessors() * 2;
    private boolean useResourceLayer = false;
    private boolean useStrictMessages = false;

    public ServiceFactoryBuilder(File... workspaceLayers) {
        this.workspaceLayers = Objects.requireNonNull(workspaceLayers, "'Workspace layers' parameter");
    }

    /**
     * Creates {@link ServiceFactory} with set parameters
     * @return
     * @throws IOException
     * @throws SailfishURIException
     */
    public ServiceFactory build() throws IOException, SailfishURIException {
        return new ServiceFactory(minThreads, maxThreads, scheduledThreads, useResourceLayer, useStrictMessages, workspaceLayers);
    }

    /**
     * Sets {@code minThreads} for {@link ServiceFactory}
     * @param minThreads the number of threads to keep in the pool, even
     *                  if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @return
     */
    public ServiceFactoryBuilder withMinThreadsCount(int minThreads) {
        this.minThreads = minThreads;
        return this;
    }

    /**
     * Sets {@code maxThreads} for {@link ServiceFactory}
     * @param maxThreads the maximum number of threads to allow in the pool
     * @return
     */
    public ServiceFactoryBuilder withMaxThreadsCount(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    /**
     * Sets {@code scheduledThreads} for {@link ServiceFactory}
     * @param scheduledThreads the number of threads to keep in the pool,
     *                         even if they are idle
     * @return
     */
    public ServiceFactoryBuilder withScheduledThreadsCount(int scheduledThreads) {
        this.scheduledThreads = scheduledThreads;
        return this;
    }

    /**
     * Sets {@code useResourceLayer} for {@link ServiceFactory}
     * @param useResourceLayer if true resource layer '{@link ServiceFactory#ROOT_PACKAGE}' will be plugged
     * @return
     */
    public ServiceFactoryBuilder useResourcesLayer(boolean useResourceLayer){
        this.useResourceLayer = useResourceLayer;
        return this;
    }

    /**
     * Sets {@code useStrictMessages} for {@link ServiceFactory}
     * @param useStrictMessages if true all creating messages in services will be instance of '{@link StrictMessageWrapper}'
     * @return
     */
    public ServiceFactoryBuilder useStrictMessages(boolean useStrictMessages){
        this.useStrictMessages = useStrictMessages;
        return this;
    }
}
