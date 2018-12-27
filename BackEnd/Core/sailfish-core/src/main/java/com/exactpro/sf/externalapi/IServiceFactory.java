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
package com.exactpro.sf.externalapi;

import java.io.InputStream;
import java.util.Set;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.externalapi.impl.ServiceFactoryException;

public interface IServiceFactory extends AutoCloseable {
    
    /**
     * Creates service proxy for client using setting exported from Sailfish.
     *
     * @param setting  the input stream with service in xml format
     * @param listener the user implementation of {@link IServiceListener}. This parameter can be null.
     * @return
     * @throws ServiceFactoryException if input stream contains incorrect format,
     *                                  or creating service failed
     * @throws IllegalArgumentException if input stream contains more then one settings set, 
     *                                  or unknown service type specified in settings
     */
    IServiceProxy createService(InputStream setting, IServiceListener listener) throws ServiceFactoryException;
    
    /**
     * Creates service proxy for initiator using name and service type.
     *
     * @param name        the name of service
     * @param serviceType the Sailfish URI to available service {@link #getServiceTypes()}
     * @param listener    the user implementation of {@link IServiceListener}. This parameter may be null
     * @return
     * @throws ServiceFactoryException if creating service failed
     * @throws IllegalArgumentException if unknown service type specified in settings.
     *                                  
     * @see ServiceName
     */
    IServiceProxy createService(ServiceName name, SailfishURI serviceType, IServiceListener listener) throws ServiceFactoryException;
    
    /**
     * Register new dictionary or overwrite existing.
     * During the registration process content from input stream will be saved to the file on the last layer.   
     *
     * @param name the name using for making {@link SailfishURI} 
     * @param dictionary the input stream to Sailfish dictionary in xml format 
     * @param overwrite
     * @return
     * @throws ServiceFactoryException if dictionary registration failure
     * @throws IllegalArgumentException if name has incorrect format 
     *                                  or overwrite false and dictionary already exist
     */
    SailfishURI registerDictionary(String name, InputStream dictionary, boolean overwrite) throws ServiceFactoryException;
    
    /**
     * @return static {@code set} of available services
     */
    Set<SailfishURI> getServiceTypes();
    
    /**
     * @return set of available dictionaries. This set may be changed by calling {@link #registerDictionary(String, InputStream, boolean)}
     */
    Set<SailfishURI> getDictionaries();

    /**
     * Create factory proxy for creating instance of IMessage.
     * @param serviceType
     * @return
     * @throws IllegalArgumentException if service type is unknown
     */
    IMessageFactoryProxy getMessageFactory(SailfishURI serviceType);
}
