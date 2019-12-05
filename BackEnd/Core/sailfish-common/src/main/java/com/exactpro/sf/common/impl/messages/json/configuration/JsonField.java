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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;

import com.exactpro.sf.common.impl.messages.all.configuration.IField;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.AttributesDeserializeConverter;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.AttributesSerializeConverter;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.JavaTypeDeserializeConverter;
import com.exactpro.sf.common.impl.messages.json.configuration.Converters.JavaTypeSerializeConverter;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Java class for reading IField from JSON/YAML formats.
 */
public class JsonField implements IField {

    private static final long serialVersionUID = -1693691200344421640L;

    @JsonIgnore
    @JsonInclude(Include.NON_EMPTY)
    protected String name;

    @JsonInclude(Include.NON_EMPTY)
    @JsonAlias("desc")
    protected String description;

    @JsonDeserialize(converter = AttributesDeserializeConverter.class)
    @JsonSerialize(converter = AttributesSerializeConverter.class)
    @JsonInclude(Include.NON_EMPTY)
    protected List<JsonAttribute> attributes;

    @JsonDeserialize(converter = AttributesDeserializeConverter.class)
    @JsonSerialize(converter = AttributesSerializeConverter.class)
    @JsonInclude(Include.NON_EMPTY)
    protected List<JsonAttribute> values;

    @JsonProperty("isServiceName")
    @JsonInclude(Include.NON_NULL)
    protected Boolean isServiceName;

    @JsonProperty("isCollection")
    @JsonInclude(Include.NON_NULL)
    protected Boolean isCollection;

    @JsonInclude(Include.NON_NULL)
    protected String defaultValue;

    @JsonInclude(Include.NON_NULL)
    @JsonDeserialize(converter = JavaTypeDeserializeConverter.class)
    @JsonSerialize(converter = JavaTypeSerializeConverter.class)
    protected JavaType type;

    @JsonIgnore
    protected JsonField reference;

    @JsonInclude(Include.NON_NULL)
    @JsonAlias("ref")
    protected String referenceName;

    @JsonProperty("required")
    @JsonInclude(Include.NON_DEFAULT)
    protected Boolean required;

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
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        return attributes;
    }

    /**
     * Gets the value of the values property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the values property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getValues().add(newItem);
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
    public List<JsonAttribute> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }

    /**
     * Gets the value of the isServiceName property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Override
    public boolean isIsServiceName() {
        return BooleanUtils.isTrue(isServiceName);
    }


    @JsonProperty("isServiceName")
    public Boolean getIsServiceName(){
        return isServiceName;
    }

    /**
     * Sets the value of the isServiceName property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @JsonProperty("isServiceName")
    public void setIsServiceName(Boolean value) {
        this.isServiceName = value;
    }

    /**
     * Gets the value of the isCollection property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Override
    public boolean isIsCollection() {
        return BooleanUtils.isTrue(isCollection);
    }


    @JsonProperty("isCollection")
    public Boolean getIsCollection(){
        return isCollection;
    }

    /**
     * Sets the value of the isCollection property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @JsonProperty("isCollection")
    public void setIsCollection(Boolean value) {
        this.isCollection = value;
    }

    /**
     * Gets the value of the defaultvalue property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the value of the defaultvalue property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDefaultValue(String value) {
        this.defaultValue = value;
    }

    /**
     * Gets the value of the type property.
     *
     * @return
     *     possible object is
     *     {@link JavaType }
     *
     */
    @Override
    public JavaType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value
     *     allowed object is
     *     {@link JavaType }
     *
     */
    public void setType(JavaType value) {
        this.type = value;
    }

    /**
     * Get value of the <b>id</b> property if it is not null, else get value of <b>name</b> property
     * @return {@link String} - <b>id</b> if not null, else <b>name</b>
     */
    @Override
    @JsonIgnore
    public String getId() {
        return name;
    }

    /**
     * Gets the value of the reference property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    @Override
    public JsonField getReference() {
        return reference;
    }

    /**
     * Sets the value of the reference property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setReference(JsonField value) {
        this.reference = value;
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

    /**
     * Gets the value of the required property.
     *
     * @return if property isn`t null {@link Boolean }, else <b><i>false</i></b>
     *
     */
    @Override
    public boolean isRequired() {
        return BooleanUtils.isTrue(required);
    }

    /**
     * Sets the value of the required property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setRequired(Boolean value) {
        this.required = value;
    }

    /**
     * Sets the value of the <b>referenceName</b> property
     *
     * Read json/yaml property <b>reference</b>
     * @param referenceName {@link String}
     */
    @JsonProperty("reference")
    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    /**
     * Get the value of the <b>referenceName</b> property
     *
     * Write json/yaml property <b>reference</b>
     * @return {@link String}
     */
    @JsonProperty("reference")
    public String getReferenceName() {
        return referenceName;
    }
}
