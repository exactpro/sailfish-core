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
import static com.exactpro.sf.services.soap.SOAPMessageHelper.IGNORE_ATTRIBUTE;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.util.EPSCommonException;

public class SOAPVisitorEncode extends DefaultMessageStructureVisitor {

    final SOAPElement rootNode;
    private final String targetNamespace;
    private boolean defaulNamespaceInstalled;

    private static final Logger logger = LoggerFactory.getLogger(SOAPVisitorEncode.class);

    public SOAPVisitorEncode(SOAPElement node, String targetNamespace) throws SOAPException {
        this.rootNode = node;
        this.targetNamespace = targetNamespace;
        if (targetNamespace == null) {
            defaulNamespaceInstalled = true;
        }
    }
    
    @Override
    public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));

        if (isAttribute) {
            processAttributeNode(value, fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(value, fieldName, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
        
        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));

        if (isAttribute) {
            processAttributeNode(value, fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(value, fieldName, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
        
        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));

        if (isAttribute) {
            processAttributeNode(value, fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(value, fieldName, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {

        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));

        if (isAttribute) {
            processAttributeNode(value, fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(value, fieldName, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {

        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));

        if (isAttribute) {
            processAttributeNode(value, fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(value, fieldName, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {

        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));

        // Any way we add the value as a String value.
        // So use toPlainString() to avoid exponent in the value.
        if (isAttribute) {
            processAttributeNode(value.toPlainString(), fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(value.toPlainString(), fieldName, fldStruct, isDefault);
        }
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> value, IFieldStructure fldStruct, boolean isDefault) {
        
        if (value == null) {
            return;
        }
        
        for (String val : value) {
            processSimpleNode(val, fieldName, fldStruct, isDefault);
        }
    }
    
    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        
        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));
     
        String asString = value.format(DateTimeFormatter.ISO_DATE);
        
        if (isAttribute) {
            processAttributeNode(asString, fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(asString, fieldName, fldStruct, isDefault);
        }
    }
    
    @Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
        
        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));
        
        String asString = value.format(DateTimeFormatter.ISO_DATE_TIME);
        
        if (isAttribute) {
            processAttributeNode(asString, fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(asString, fieldName, fldStruct, isDefault);
        }
        
    }
    
    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        
        if (value == null) {
            return;
        }

        boolean isAttribute = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, SOAPMessageHelper.IS_ATTRIBUTE));
     
        String asString = value.format(DateTimeFormatter.ISO_TIME);
        
        if (isAttribute) {
            processAttributeNode(asString, fieldName, fldStruct, isDefault);
        } else {
            processSimpleNode(asString, fieldName, fldStruct, isDefault);
        }
        
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure fldStruct, boolean isDefault) {
        
        if (message == null) {
            return;
        }
        
        SOAPElement nestedElement;
        DefaultMessageStructureVisitor nestedVistor = null;
        String newTargetNamespace = getAttributeValue(fldStruct, SOAPMessageHelper.TNS);
        try {
            nestedElement = addSimpleNode(fieldName, fldStruct);
            if(nestedElement == null) {
                return;
            }
            nestedVistor = new SOAPVisitorEncode(nestedElement, defaultIfNull(newTargetNamespace, targetNamespace));
        } catch (SOAPException e) {
            logger.error(e.getMessage(), e);
        }
        MessageStructureReader.READER.traverse(nestedVistor, fldStruct.getFields(), message, MessageStructureReaderHandlerImpl.instance());
    }

    @Override
    public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure fldStruct, boolean isDefault) {
        
        if (message == null) {
            return;
        }
        
        for (IMessage nestedMessage : message) {
            visit(fieldName, nestedMessage, fldStruct, isDefault);
        }
    }

    @Override
    public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
        
        if (value == null) {
            return;
        }
        
        for (Integer val : value) {
            processSimpleNode(val, fieldName, fldStruct, isDefault);
        }
    }

    public void visitBooleanCollection(String fieldName, List<Boolean> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value == null) {
            return;
        }

        for (Boolean val : value) {
            processSimpleNode(val, fieldName, fldStruct, isDefault);
        }
    }

    public void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault)
    {
        if (value == null) {
            return;
        }

        for (Long val : value) {
            processSimpleNode(val, fieldName, fldStruct, isDefault);
        }
    }

    public void visitByteCollection(String fieldName, List<Byte> value, IFieldStructure fldStruct, boolean isDefault)
    {
        if (value == null) {
            return;
        }

        for (Byte val : value) {
            processSimpleNode(val, fieldName, fldStruct, isDefault);
        }
    }

    public void visitShortCollection(String fieldName, List<Short> value, IFieldStructure fldStruct, boolean isDefault)
    {
        if (value == null) {
            return;
        }

        for (Short val : value) {
            processSimpleNode(val, fieldName, fldStruct, isDefault);
        }
    }

    public void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault)
    {
        if (value == null) {
            return;
        }

        for (Character val : value) {
            processSimpleNode(val, fieldName, fldStruct, isDefault);
        }
    }

    private void processSimpleNode(Object value, String fieldName, IFieldStructure fldStruct, boolean isDefault) {
        try {
            SOAPElement node = addSimpleNode(fieldName, fldStruct);
            if(node == null) {
                return;
            }
            node.addTextNode(value.toString());
        } catch (SOAPException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void processAttributeNode(Object value, String fieldName, IFieldStructure fldStruct, boolean isDefault) {
        String xmlns = getAttributeValue(fldStruct, SOAPMessageHelper.XMLNS);
        String prefix = getAttributeValue(fldStruct, SOAPMessageHelper.PREFIX);

        try {
            rootNode.addAttribute(buildNameAttr(xmlns, prefix, fieldName), value.toString());
        } catch (SOAPException e) {
            throw new EPSCommonException(e.getMessage(), e);
        }
    }

    private @Nullable SOAPElement addSimpleNode(String fieldName, IFieldStructure fldStruct) throws SOAPException {
        // FIXME how to manage xml namespace and his prefixes
        String xmlns = getAttributeValue(fldStruct, SOAPMessageHelper.XMLNS);
        String prefix = getAttributeValue(fldStruct, SOAPMessageHelper.PREFIX);
        boolean ignore = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, IGNORE_ATTRIBUTE));
        if(ignore) {
            return null;
        }

        if (prefix != null) {
            if (xmlns != null) {
                return rootNode.addChildElement(fieldName, prefix, xmlns);
            } else {
                return rootNode.addChildElement(fieldName, prefix);
            }
        } else {
            if (!defaulNamespaceInstalled) {
                defaulNamespaceInstalled = true;
                return rootNode.addChildElement(fieldName, "", targetNamespace);
            } else {
                return rootNode.addChildElement(fieldName);
            }           

        }

    }

    private QName buildNameAttr(String xmlns, String prefix, String localName) {
        return new QName(StringUtils.defaultString(xmlns), localName, StringUtils.defaultString(prefix));
    }

}
