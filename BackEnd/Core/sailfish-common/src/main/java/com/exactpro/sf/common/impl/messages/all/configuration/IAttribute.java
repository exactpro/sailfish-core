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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import java.io.Serializable;

/**
 *  <p> Java interface for dictionaries attributes
 */
public interface IAttribute extends Serializable {

    /**
     * Gets the value of the name.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    String getName();

    /**
     * Gets the value of the type.
     *
     * @return
     *     possible object is
     *     {@link JavaType }
     *
     */
    JavaType getType();

    /**
     * Gets the value of the value.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    String getValue();
}
