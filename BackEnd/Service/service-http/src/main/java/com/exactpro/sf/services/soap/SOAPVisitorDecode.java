/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.soap;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPElement;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.comparison.conversion.MultiConverter;

public class SOAPVisitorDecode extends DefaultMessageStructureVisitor {

    final SOAPElement rootNode;
    final IMessageFactory msgFactory;
    final IMessage message;
    private final String targetNamespace;

    private static final Logger logger = LoggerFactory.getLogger(SOAPVisitorDecode.class);

    public SOAPVisitorDecode(SOAPElement node, IMessageFactory msgFactory, IMessage message, String targetNameSpace) {
        this.rootNode = node;
        this.msgFactory = msgFactory;
        this.message = message;
        this.targetNamespace = targetNameSpace;
    }

    @Override
    public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {

        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, Boolean.valueOf(val));
    }

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, LocalDate.parse(val, DateTimeFormatter.ISO_DATE));
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {

        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, LocalTime.parse(val, DateTimeFormatter.ISO_TIME));
    }

    @Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {

        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, LocalDateTime.parse(val, DateTimeFormatter.ISO_DATE_TIME));
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> value, IFieldStructure fldStruct, boolean isDefault) {
        List<String> values = extractValues(fldStruct, fieldName, String.class);

        if(markAsRejected(fieldName, fldStruct, values)) {
            return;
        }

        message.addField(fieldName, values);
    }

    @Override
    public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Integer> values = extractValues(fldStruct, fieldName, Integer.class);

        if(markAsRejected(fieldName, fldStruct, values)) {
            return;
        }

        message.addField(fieldName, values);
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {

        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, val);
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, val == null ? null : Integer.valueOf(val));
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, val == null ? null : Long.valueOf(val));
    }

    @Override
    public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, val == null ? null : Double.valueOf(val));
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        String val = extractValue(fldStruct, fieldName);
        if (markAsRejected(fieldName, fldStruct, val)) {
            return;
        }

        message.addField(fieldName, val == null ? null : new BigDecimal(val));
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure fldStruct, boolean isDefault) {

        String xmlns = getAttributeValue(fldStruct, SOAPMessageHelper.XMLNS);
        String prefix = getAttributeValue(fldStruct, SOAPMessageHelper.PREFIX);
        String newTargetNamespace = getAttributeValue(fldStruct, SOAPMessageHelper.TNS);

        @SuppressWarnings("unchecked")
        Iterator<Node> it = rootNode.getChildElements(buildName(xmlns, prefix, fieldName));
        if (markAsRejected(fieldName, fldStruct, it.hasNext() ? it : null)) {
            return;
        }
        SOAPElement element = (SOAPElement)it.next();
        IMessage nested = msgFactory.createMessage(fieldName, fldStruct.getNamespace());
        IMessageStructureVisitor visitor = new SOAPVisitorDecode(element, msgFactory, nested, defaultIfNull(newTargetNamespace, targetNamespace));
        MessageStructureWriter.WRITER.traverse(visitor, fldStruct.getFields());
        this.message.addField(fieldName, nested);
        if (nested.getMetaData().isRejected()) {
            markAsRejected(fieldName, fldStruct, null);
        }
    }

    @Override
    public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure fldStruct, boolean isDefault) {
        String xmlns = getAttributeValue(fldStruct, SOAPMessageHelper.XMLNS);
        String prefix = getAttributeValue(fldStruct, SOAPMessageHelper.PREFIX);
        String newTargetNamespace = getAttributeValue(fldStruct, SOAPMessageHelper.TNS);
        @SuppressWarnings("unchecked")
        Iterator<Node> it = rootNode.getChildElements(buildName(xmlns, prefix, fieldName));
        if (markAsRejected(fieldName, fldStruct, it.hasNext() ? it : null)) {
            return;
        }

        List<IMessage> messages = new ArrayList<>();
        while (it.hasNext()) {
            SOAPElement element = (SOAPElement)it.next();
            IMessage nested = msgFactory.createMessage(fieldName, fldStruct.getNamespace());

            IMessageStructureVisitor visitor = new SOAPVisitorDecode(element, msgFactory, nested, defaultIfNull(newTargetNamespace, targetNamespace));
            MessageStructureWriter.WRITER.traverse(visitor, fldStruct.getFields());
            messages.add(nested);

            if (nested.getMetaData().isRejected()) {
                this.message.getMetaData().setRejected(true);
            }
        }
        this.message.addField(fieldName, messages);
    }

    private QName buildName(String xmlns, String prefix, String localName) {
        return new QName(StringUtils.defaultString(xmlns, targetNamespace), localName, StringUtils.defaultString(prefix));
    }

    private QName buildNameAttr(String xmlns, String prefix, String localName) {
        return new QName(StringUtils.defaultString(xmlns), localName, StringUtils.defaultString(prefix));
    }

    private String extractValue(IFieldStructure fldStruct, String fieldName) {

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));
        // TODO namespace
        String xmlns = getAttributeValue(fldStruct, SOAPMessageHelper.XMLNS);
        String prefix = getAttributeValue(fldStruct, SOAPMessageHelper.PREFIX);
        if (isAttribute) {
            return rootNode.getAttributeValue(buildNameAttr(xmlns, prefix, fieldName));
        }
        @SuppressWarnings("unchecked")
        Iterator<Node> elements = rootNode.getChildElements(buildName(xmlns, prefix, fieldName));
        return (elements == null || !elements.hasNext()) ? null : elements.next().getTextContent();

    }

    private <T> List<T> extractValues(IFieldStructure fldStruct, String fieldName, Class<T> targetElementClass) {
        NodeList elements = rootNode.getElementsByTagName(fieldName);

        if(elements == null || elements.getLength() == 0) {
            return null;
        }

        List<T> values = new ArrayList<>();

        for(int i = 0; i < elements.getLength(); i++) {
            values.add(MultiConverter.convert(elements.item(i).getTextContent(), targetElementClass));
        }

        return values;
    }

    private boolean markAsRejected(String fieldName, IFieldStructure fldStruct, Object value) {

        if (value == null && fldStruct.isRequired()) {
            logger.debug("Required field [{}] missing in message [{}::{}]", fieldName, message.getNamespace(), message.getName());
            message.getMetaData().setRejected(true);
        }

        //Nothing to parse
        return value == null;
    }


}
