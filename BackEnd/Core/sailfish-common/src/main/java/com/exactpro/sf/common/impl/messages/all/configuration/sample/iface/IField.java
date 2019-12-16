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
package com.exactpro.sf.common.impl.messages.all.configuration.sample.iface;

import java.io.Serializable;
import java.util.List;

import com.exactpro.sf.common.impl.messages.all.configuration.sample.JavaType;

/**
 * <p> Java interface for dictionaries fields
 */
public interface IField extends Serializable {

    /**
     * Gets the description.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    String getDescription();

    /**
     * Gets the fields attributes.
     *
     * @return
     *     possible object is
     *     {@link List } of {@link IAttribute}
     *
     */
    List<? extends IAttribute> getAttributes();

    /**
     * Gets the fields values.
     *
     * @return
     *     possible object is
     *     {@link List } of {@link IAttribute}
     *
     */
    List<? extends IAttribute> getValues();

    /**
     * @return <b>true</b> if it is service name.
     *
     */
    boolean isIsServiceName();

    /**
     * @return <b>true</b> if it is collection.
     *
     */
    boolean isIsCollection();

    /**
     * Gets the default value.
     *
     * @return
     *     possible object is
     *      {@link String}
     *
     */
    String getDefaultValue();

    /**
     * Gets the fields value type.
     *
     * @return
     *     possible object is
     *      {@link JavaType}
     *
     */
    com.exactpro.sf.common.impl.messages.all.configuration.sample.JavaType getType();

    /**
     * Gets the fields id.
     *
     * @return
     *     possible object is
     *      {@link String}
     *
     */
    String getId();

    /**
     * Gets the fields name.
     *
     * @return
     *     possible object is
     *      {@link String}
     *
     */
    String getName();

    /**
     * Gets the reference field.
     *
     * @return
     *     possible object is
     *      {@link IField}
     *
     */
    IField getReference();

    /**
     * @return <b>true</b> if it is required
     *
     */
    boolean isRequired();
}
