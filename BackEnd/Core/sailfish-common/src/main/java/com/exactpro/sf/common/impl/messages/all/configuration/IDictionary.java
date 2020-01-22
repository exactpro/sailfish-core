/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.common.impl.messages.all.configuration;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import java.io.Serializable;
import java.util.List;


/**
 * Java interface for readable structure of {@link IDictionaryStructure}
 */
public interface IDictionary extends Serializable {

    /**
     * Get dictionaries name.
     *
     * @return {@link String}
     */
    String getName();

    /**
     * Get dictionaries description.
     *
     * @return {@link String}
     */
    String getDescription();

    /**
     * Get dictionaries attributes.
     *
     * @return {@link List} of {@link IAttribute} or extends it
     */
    List<? extends IAttribute> getAttributes();

    /**
     * Get dictionaries fields.
     *
     * @return {@link List} of {@link IField} or extends it
     */
    List<? extends IField> getFields();

    /**
     * Get dictionaries messages.
     *
     * @return {@link List} of {@link IMessage} or extends it
     */
    List<? extends IMessage> getMessages();
}
