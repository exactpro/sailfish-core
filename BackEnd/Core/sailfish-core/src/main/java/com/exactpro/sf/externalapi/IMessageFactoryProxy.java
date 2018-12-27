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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.suri.SailfishURI;

public interface IMessageFactoryProxy {

    /**
     * Creates instance of IMessage with name from dictionary.
     *
     * @param dictionary  Sailfish URI to available dictionary
     * @param name        the name of message to create
     * @return
     * @throws IllegalArgumentException if dictionary is unknown,
     *                                  or message name is not present in dictionary
     */
    IMessage createMessage(SailfishURI dictionary, String name);

    /**
     * @deprecated - this method has been deprecated, will be REMOVED in future releases
     * Please set true for useStrictMessage in {@link com.exactpro.sf.externalapi.ServiceFactory} for using strict messages in ExternalAPI
     *
     * Creates instance of {@link com.exactpro.sf.common.messages.IMessage
     * IMessage} with name passed in arguments. Strict message tries to convert
     * addition value to target type otherwise throws {@link RuntimeException}.
     * <p>
     * If added value is a collection but field in dictionary isn't collection -
     * implementation try to extract single value from collection. If the collection
     * has more than 1 element implementation throws {@link RuntimeException}.
     * <p>
     * If added value is single value but field in dictionary is collection -
     * implementation wrapped single value in
     * {@link java.util.Collections#checkedList(java.util.List, Class) CheckedList}.
     * All collection added in this implementation also wrapped in this type of
     * {@code List}
     * 
     * @param dictionary
     *            Sailfish URI to available dictionary
     * @param name
     *            the name of message to create
     * @return
     * @throws IllegalArgumentException
     *             if dictionary is unknown, or message name is not present in
     *             dictionary
     */
    @Deprecated
    IMessage createStrictMessage(SailfishURI dictionary, String name);
}
