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
package com.exactpro.sf.common.impl.messages.json.configuration;

import java.util.Collections;
import java.util.List;

import com.exactpro.sf.common.impl.messages.all.configuration.IDictionary;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.AttributesDeserializeConverter;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.AttributesSerializeConverter;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.FieldsDeserializeConverter;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.FieldsSerializeConverter;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.MessagesDeserializeConverter;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.MessagesSerializeConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Json class for reading IDictionary from JSON/YAML formats.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonYamlDictionary implements IDictionary {

    private static final long serialVersionUID = -5925008026309305864L;

    @JsonInclude(Include.NON_EMPTY)
    protected String description;

    @JsonDeserialize(converter = AttributesDeserializeConverter.class)
    @JsonSerialize(converter = AttributesSerializeConverter.class)
    @JsonInclude(Include.NON_EMPTY)
    protected List<JsonAttribute> attributes;

    @JsonDeserialize(converter = FieldsDeserializeConverter.class)
    @JsonSerialize(converter = FieldsSerializeConverter.class)
    @JsonInclude(Include.NON_EMPTY)
    protected List<JsonField> fields;

    @JsonDeserialize(converter = MessagesDeserializeConverter.class)
    @JsonSerialize(converter = MessagesSerializeConverter.class)
    @JsonInclude(Include.NON_EMPTY)
    protected List<JsonMessage> messages;

    @JsonInclude(Include.NON_EMPTY)
    protected String name;

    @JsonCreator
    public JsonYamlDictionary(@JsonProperty(value = "name", required = true) String name) {
        this.name = name;
    }

    /**
     * Gets the value of the description property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the attributes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attributes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttributes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JsonAttribute }
     *
     *
     */
    @Override
    public List<JsonAttribute> getAttributes() {
        return attributes == null ? Collections.emptyList() : Collections.unmodifiableList(attributes);
    }

    /**
     * Gets the value of the fields property.
     *
     * @return
     *     possible object is
     *     {@link List<JsonField> }
     *
     */
    @Override
    public List<JsonField> getFields() {
        return fields == null ? Collections.emptyList() : Collections.unmodifiableList(fields);
    }

    /**
     * Sets the value of the fields property.
     *
     * @param value
     *     allowed object is
     *     {@link List<JsonField> }
     *
     */
    public void setFields(List<JsonField> value) {
        this.fields = value;
    }

    /**
     * Gets the value of the messages property.
     *
     * @return
     *     possible object is
     *     {@link List<JsonMessage> }
     *
     */
    @Override
    public List<JsonMessage> getMessages() {
        return messages == null ? Collections.emptyList() : Collections.unmodifiableList(messages);
    }

    /**
     * Sets the value of the messages property.
     *
     * @param value
     *     allowed object is
     *     {@link List<JsonMessage>}
     *
     */
    public void setMessages(List<JsonMessage> value) {
        this.messages = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }
}
