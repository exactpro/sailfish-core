//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.12.12 at 02:34:49 PM GMT+04:00 
//


package com.exactpro.sf.common.impl.messages.all.configuration.sample;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.exactpro.sf.common.impl.messages.all.configuration.sample.iface.IDictionary;

/**
 * <p>Java class for Dictionary complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Dictionary">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;sequence minOccurs="0">
 *           &lt;element name="attribute" type="{http://exactprosystems.com/dictionary}Attribute" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;/sequence>
 *         &lt;element name="fields" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="field" type="{http://exactprosystems.com/dictionary}Field" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="messages" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="message" type="{http://exactprosystems.com/dictionary}Message" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}NMTOKEN" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Dictionary", propOrder = {
    "description",
    "attributes",
    "fields",
    "messages"
})
@XmlRootElement(name = "dictionary")
public class Dictionary implements IDictionary, Serializable
{

    private final static long serialVersionUID = -1L;
    protected String description;
    
    @XmlElement(name = "attribute", type = Attribute.class)
    protected List<Attribute> attributes;
    
    @XmlElementWrapper(name = "fields")
    @XmlElement(name = "field")
    protected List<Field> fields;
    
    @XmlElementWrapper(name = "messages")
    @XmlElement(name = "message")
    protected List<Message> messages;
    
    @XmlAttribute(name = "name", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    protected String name;

    /**
     * Gets the value of the description property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
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
     * {@link Attribute }
     *
     *
     */
    public List<Attribute> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        return this.attributes;
    }

    /**
     * Gets the value of the fields property.
     *
     * @return
     *     possible object is
     *     {@link Field }
     *
     */
    public List<Field> getFields() {
        return fields;
    }

    /**
     * Sets the value of the fields property.
     *
     * @param fields
     *     allowed object is
     *     {@link Field }
     *
     */
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    /**
     * Gets the value of the messages property.
     *
     * @return
     *     possible object is
     *     {@link Message }
     *
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Sets the value of the messages property.
     *
     * @param messages
     *     allowed object is
     *     {@link Message }
     *
     */
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
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
