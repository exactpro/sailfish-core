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
package com.exactpro.sf.configuration.dictionary.converter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.exactpro.sf.common.messages.MessageNotFoundException;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.fix.FixMessageHelper;

public class SailfishDictionaryToQuckfixjConverter {

    // constants
    private static final Logger logger = LoggerFactory.getLogger(SailfishDictionaryToQuckfixjConverter.class);
    private final IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private final DocumentBuilder documentBuilder;
    private final Transformer transformer;
    private final String IS_ADMIN = "IsAdmin";
    private final String REQUIRED = "required";
    private final String NAME = "name";
    private final String ENUM = "enum";
    private final String NUMBER = "number";

    // variable
    private Set<String> componentNames;
    private Set<String> fieldNames;
    private IDictionaryStructure sailfishDictionary;
    private List<Element> accumulateList;
    private List<Element> accumulateValueList;
    private int minor, major;

    public SailfishDictionaryToQuckfixjConverter()
            throws ParserConfigurationException, TransformerConfigurationException {
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        componentNames = new HashSet<>();
        fieldNames = new HashSet<>();
        accumulateList = new ArrayList<>();
        accumulateValueList = new ArrayList<>();
    }

    public void convertToQuickFixJ(InputStream sailfishXML, String outputDir)
            throws IOException, DOMException, MessageNotFoundException, TransformerException {
        Map<String, Document> outputXMLs = new HashMap<>();
        sailfishDictionary = loader.load(sailfishXML);
        String[] versionFix = sailfishDictionary.getNamespace().split("_");
        minor = Integer.parseInt(versionFix[versionFix.length - 1]);
        major = Integer.parseInt(versionFix[versionFix.length - 2]);

        if (major < 5) {
            outputXMLs.put("FIX" + major + minor + ".xml",
                    createQuickFixJDictionaryStructure(documentBuilder.newDocument(), Mode.ALL));
        } else {
            outputXMLs.put("FIXT11.xml", createQuickFixJDictionaryStructure(documentBuilder.newDocument(), Mode.ADMIN));
            outputXMLs.put("FIX" + major + minor + ".xml",
                    createQuickFixJDictionaryStructure(documentBuilder.newDocument(), Mode.APP));
        }
        StreamResult outputResult = new StreamResult();
        for (String fileName : outputXMLs.keySet()) {
            try (OutputStream out = FileUtils.openOutputStream(
                    new File(outputDir, fileName))) {
                outputResult.setOutputStream(out);
                transformer.transform(new DOMSource(outputXMLs.get(fileName)), outputResult);
            }
        }
    }

    /**
     * 
     * @param args mode
     *              -c: output dictionary, input Sailfish FIX dictionaries
     *              -n: input QuickFIXJ dictionaries
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please use one of modes:\r\n\t-c 'output dictionary' 'input Sailfish FIX dictionaries'\r\n\t-n 'input QuickFIXJ dictionaries'");
            System.exit(1);
        }
        try {
            switch (args[0]) {
            case "-c":
                System.out.println("Convertation mode:\r\n\t-c 'output dictionary' 'input Sailfish FIX dictionaries'");
                if (args.length < 3) {
                    System.exit(2);
                }
                convert(args);
                break;
            case "-n":
                System.out.println("Normalization mode:\r\n\t-n 'input QuickFIXJ dictionaries'");
                if (args.length < 2) {
                    System.exit(2);
                }
                normalizate(args);
                break;
            default:
                System.out.println("Please use one of modes: \r\n\t-c 'output dictionary' 'input Sailfish FIX dictionaries'\r\n\t-n 'input QuickFIXJ dictionaries'");
                System.exit(3);
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args
     * @throws ParserConfigurationException 
     * @throws TransformerException 
     * @throws IOException 
     * @throws SAXException 
     */
    private static void normalizate(String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        SailfishDictionaryToQuckfixjConverter converter = new SailfishDictionaryToQuckfixjConverter();
        
        for (int i = 1; i < args.length; i++) {
            Path input = Paths.get(args[i]).toAbsolutePath();
            System.out.println(input);
            resave(converter, input);
        }
    }

    /**
     * @param args
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws TransformerException 
     * @throws MessageNotFoundException 
     * @throws DOMException 
     */
    private static void convert(String[] args) throws IOException, ParserConfigurationException, DOMException, MessageNotFoundException, TransformerException {
        String output = Paths.get(args[1]).toAbsolutePath().toString();
        System.out.println(output);
        
        SailfishDictionaryToQuckfixjConverter converter = new SailfishDictionaryToQuckfixjConverter();
        
        for (int i = 2; i < args.length; i++) {
            Path input = Paths.get(args[i]).toAbsolutePath();
            System.out.println(input);
            try (InputStream sailfishXML = Files.newInputStream(input)) {
                converter.convertToQuickFixJ(sailfishXML, output);
            }
        }
    }

    /**
     * @param converter
     * @param args
     * @param index TODO
     * @throws SAXException
     * @throws IOException
     * @throws TransformerException
     */
    private static void resave(SailfishDictionaryToQuckfixjConverter converter, Path quickFIXJDictionary)
            throws SAXException, IOException, TransformerException {
        Document document = null;
        try (InputStream sailfishXML = Files.newInputStream(quickFIXJDictionary)) {
            document = converter.documentBuilder.parse(sailfishXML);
        }
        
        Node fixNode = document.getDocumentElement();
        NodeList list = fixNode.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            switch (node.getNodeName()) {
            case "messages":
            case "components":
                reorder(node, Comparators.NAME, null);
                break;
            case "fields":
                reorder(node, Comparators.NUMBER, Comparators.ENUM);
                break;
            }
        }
        
        StreamResult outputResult = new StreamResult();
        try (OutputStream out = Files.newOutputStream(quickFIXJDictionary)) {
            outputResult.setOutputStream(out);
            converter.transformer.transform(new DOMSource(document), outputResult);
        }
    }
    
    private static void reorder(Node root, Comparators comparator, Comparators comparator2) {
        List<Element> elementList = new ArrayList<>();
        NodeList listNode = root.getChildNodes();
        List<Node> list = new ArrayList<>(listNode.getLength());
        for (int i = 0; i < listNode.getLength(); i++) {
            Node node = listNode.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (comparator2 != null) {
                    reorder(node, comparator2, null);
                }
                elementList.add((Element)node);
            }
            list.add(node);
        }

        for (Node node : list) {
            root.removeChild(node);
        }

        elementList.sort(comparator.getComparator());
        for (Node node : elementList) {
            root.appendChild(node);
        }
    }
    
    private Document createQuickFixJDictionaryStructure(Document doc, Mode mode)
            throws DOMException, EPSCommonException, MessageNotFoundException {

        // clear old components and fields references
        componentNames.clear();
        fieldNames.clear();

        Element header, trailer;

        if (mode != Mode.APP) {
            header = (Element) createHeader(sailfishDictionary.getMessageStructure(FixMessageHelper.HEADER), doc);
            trailer = (Element) createTrailer(sailfishDictionary.getMessageStructure(FixMessageHelper.TRAILER), doc);
        } else {
            header = (Element) createHeader(null, doc);
            trailer = (Element) createTrailer(null, doc);
        }

        Element messages = doc.createElement("messages");
        accumulateList.clear();
        for (IMessageStructure message : sailfishDictionary.getMessageStructures()) {
            if (FixMessageHelper.MESSAGE_ENTITY
                    .equals(message.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_ENTITY_TYPE))) {
                if (mode == Mode.ALL || !((boolean) message.getAttributeValueByName(IS_ADMIN) ^ (mode == Mode.ADMIN))) {
                    accumulateList.add((Element) createMessageElement(message, doc));
                }
            }
        }

        Collections.sort(accumulateList, Comparators.NAME.getComparator());
        for (Element message : accumulateList) {
            messages.appendChild(message);
        }

        Element components = doc.createElement("components");

        if (!componentNames.isEmpty()) {
            accumulateList.clear();
            for (String id : componentNames) {
                accumulateList.add(
                        (Element) createComponent(Objects.requireNonNull(sailfishDictionary.getMessageStructure(id),
                                "Component with id '" + id + "' not found"), doc));
            }

            Collections.sort(accumulateList, Comparators.NAME.getComparator());
            for (Element component : accumulateList) {
                components.appendChild(component);
            }
        }

        Element fields = doc.createElement("fields");
        accumulateList.clear();
        for (String id : fieldNames) {
            accumulateList.add((Element) createField(Objects.requireNonNull(sailfishDictionary.getFieldStructure(id),
                    "Field with id '" + id + "' not found"), doc));
        }

        Collections.sort(accumulateList, Comparators.NUMBER.getComparator());
        for (Element field : accumulateList) {
            fields.appendChild(field);
        }

        Element fix = doc.createElement("fix");
        if (mode != Mode.ADMIN) {
            fix.setAttribute("minor", Integer.toString(minor));
            fix.setAttribute("major", Integer.toString(major));
        } else {
            fix.setAttribute("minor", Integer.toString(1));
            fix.setAttribute("major", Integer.toString(1));
            fix.setAttribute("type", "FIXT");
        }
        fix.appendChild(header);
        fix.appendChild(trailer);
        fix.appendChild(messages);
        fix.appendChild(components);
        fix.appendChild(fields);
        doc.appendChild(fix);

        return doc;
    }

    /**
     * Creates field Node for fields
     * 
     * @param fieldStructure
     * @param doc
     * @return
     */
    private Node createField(IFieldStructure fieldStructure, Document doc) {
        Element field = doc.createElement("field");
        field.setAttribute(NUMBER, fieldStructure.getAttributes().get(FixMessageHelper.ATTRIBUTE_TAG).getValue());
        field.setAttribute(NAME, fieldStructure.getName());
        field.setAttribute("type", fieldStructure.getAttributes().get("fixtype").getValue());
        if (fieldStructure.isEnum()) {
            accumulateValueList.clear();
            for (IAttributeStructure value : fieldStructure.getValues().values()) {
                accumulateValueList.add((Element) createValue(value, doc));
            }
            Collections.sort(accumulateValueList, Comparators.ENUM.getComparator());
            for (Element value : accumulateValueList) {
                field.appendChild(value);
            }
        }
        return field;
    }

    /**
     * Creates value Node for field
     * 
     * @param valueStructure
     * @param doc
     * @return
     */
    private Node createValue(IAttributeStructure valueStructure, Document doc) {
        Element value = doc.createElement("value");
        value.setAttribute(ENUM, valueStructure.getValue());
        value.setAttribute("description", valueStructure.getName());
        return value;
    }

    /**
     * Creates component Node for components
     * 
     * @param messageStructure
     * @param doc
     * @return
     * @throws DOMException
     * @throws FileNotFoundException
     * @throws MessageNotFoundException
     */
    private Node createComponent(IMessageStructure messageStructure, Document doc)
            throws DOMException, EPSCommonException, MessageNotFoundException {
        Element component = doc.createElement("component");
        component.setAttribute(NAME, messageStructure.getName());
        for (IFieldStructure field : messageStructure.getFields()) {
            component.appendChild(createMessageFieldElement(field, doc, false));
        }
        return component;
    }

    /**
     * Creates header Node
     * 
     * @param messageStructure
     * @param doc
     * @return
     * @throws DOMException
     * @throws FileNotFoundException
     * @throws MessageNotFoundException
     */
    private Node createHeader(IMessageStructure messageStructure, Document doc)
            throws DOMException, EPSCommonException, MessageNotFoundException {
        Element header = doc.createElement(FixMessageHelper.HEADER);
        if (messageStructure != null) {
            for (IFieldStructure field : messageStructure.getFields()) {
                header.appendChild(createMessageFieldElement(field, doc, true));
            }
        }
        return header;
    }

    /**
     * Creates trailer Node
     * 
     * @param messageStructure
     * @param doc
     * @return
     * @throws DOMException
     * @throws FileNotFoundException
     * @throws MessageNotFoundException
     */
    private Node createTrailer(IMessageStructure messageStructure, Document doc)
            throws DOMException, EPSCommonException, MessageNotFoundException {
        Element trailer = doc.createElement(FixMessageHelper.TRAILER);
        if (messageStructure != null) {
            for (IFieldStructure field : messageStructure.getFields()) {
                trailer.appendChild(createMessageFieldElement(field, doc, true));
            }
        }
        return trailer;
    }

    /**
     * Creates message Node
     * 
     * @param messageStructure
     * @param doc
     * @return
     * @throws DOMException
     * @throws FileNotFoundException
     * @throws MessageNotFoundException
     */
    private Node createMessageElement(IMessageStructure messageStructure, Document doc)
            throws DOMException, EPSCommonException, MessageNotFoundException {
        Element tempElement = doc.createElement("message");
        tempElement.setAttribute(NAME, messageStructure.getName());
        tempElement.setAttribute("msgtype",
                messageStructure.getAttributeValueByName(FixMessageHelper.MESSAGE_TYPE_ATTR_NAME).toString());
        if ((boolean) messageStructure.getAttributeValueByName(IS_ADMIN)) {
            tempElement.setAttribute("msgcat", "admin");
        } else {
            tempElement.setAttribute("msgcat", "app");
        }

        boolean isEmpty = true;
        for (IFieldStructure field : messageStructure.getFields()) {
            if (!FixMessageHelper.HEADER.equals(field.getReferenceName())
                    && !FixMessageHelper.TRAILER.equals(field.getReferenceName())) {
                tempElement.appendChild(createMessageFieldElement(field, doc, true));
                isEmpty = false;
            }
        }
        if (isEmpty) {
            //Hack for printing empty message as pair of open and close  tags
            tempElement.appendChild(doc.createTextNode("\n        "));
        }
        return tempElement;
    }

    /**
     * Creates different Node for message
     * 
     * @param field
     * @param doc
     * @param addToSet
     * @return
     * @throws FileNotFoundException
     * @throws MessageNotFoundException
     */
    private Node createMessageFieldElement(IFieldStructure field, Document doc, boolean addToSet)
            throws EPSCommonException, MessageNotFoundException {
        if (FixMessageHelper.GROUP_ENTITY
                .equals(field.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_ENTITY_TYPE))) {
            return createGroupElement(field, doc, addToSet);
        }
        if (FixMessageHelper.COMPONENT_ENTITY
                .equals(field.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_ENTITY_TYPE))) {
            return createMessageComponentElement(field, doc, addToSet);
        }

        Element fieldEl = doc.createElement("field");
        String name = sailfishDictionary.getFieldStructure(field.getReferenceName()).getName();
        if (name != null) {
            fieldEl.setAttribute(NAME, name);
        } else {
            logger.error("Field with id '{}' doesn't found.", field.getReferenceName());
            throw new EPSCommonException("Field with id '" + field.getReferenceName() + "' doesn't found.");
        }

        // add necessary field to set
        if (addToSet) {
            addAllNecessaryReference(field);
        }

        fieldEl.setAttribute(REQUIRED, field.isRequired() ? "Y" : "N");
        return fieldEl;
    }

    /**
     * Creates component Node for message
     * 
     * @param component
     * @param doc
     * @param addToSet
     * @return
     * @throws MessageNotFoundException
     */
    private Node createMessageComponentElement(IFieldStructure component, Document doc, boolean addToSet)
            throws MessageNotFoundException {
        Element componentEl = doc.createElement("component");
        String name = sailfishDictionary.getMessageStructure(component.getReferenceName()).getName();
        if (name != null) {
            componentEl.setAttribute(NAME, name);
        } else {
            logger.error("Component with id '{}' doesn't found.", component.getReferenceName());
            throw new MessageNotFoundException(
                    "Component with id '" + component.getReferenceName() + "' doesn't found.");
        }

        componentEl.setAttribute(REQUIRED, component.isRequired() ? "Y" : "N");

        // add all necessary components and fields to set
        if (addToSet) {
            addAllNecessaryReference(component);
        }
        return componentEl;
    }

    /**
     * Add all using fields and components in sets
     * 
     * @param element
     */
    private void addAllNecessaryReference(IFieldStructure element) {
        if (element.isComplex()) {
            if (element.isCollection()) {
                fieldNames.add(element.getName());
                for (IFieldStructure subfield : element.getFields()) {
                    addAllNecessaryReference(subfield);
                }
            } else {
                if (!componentNames.add(element.getReferenceName())) {
                    return;
                }
            }
            for (IFieldStructure field : sailfishDictionary.getMessageStructure(element.getReferenceName())
                    .getFields()) {
                addAllNecessaryReference(field);
            }
        } else {
            fieldNames.add(element.getReferenceName());
        }

    }

    /**
     * Creates group Node
     * 
     * @param group
     * @param doc
     * @param addToSet
     * @return
     * @throws MessageNotFoundException
     * @throws DOMException
     * @throws FileNotFoundException
     */
    private Node createGroupElement(IFieldStructure group, Document doc, boolean addToSet)
            throws MessageNotFoundException, DOMException, EPSCommonException {
        Element groupEl = doc.createElement("group");
        IMessageStructure gropStructure = sailfishDictionary.getMessageStructure(group.getReferenceName());
        if (gropStructure != null) {
            groupEl.setAttribute(NAME, group.getName());
        } else {
            logger.error("Group with id '{}' doesn't found.", group.getReferenceName());
            throw new MessageNotFoundException("Group with id '" + group.getReferenceName() + "' doesn't found.");
        }
        groupEl.setAttribute(REQUIRED, group.isRequired() ? "Y" : "N");

        // add necessary fields to set
        if (addToSet) {
            addAllNecessaryReference(group);
        }

        for (IFieldStructure field : gropStructure.getFields()) {
            groupEl.appendChild(createMessageFieldElement(field, doc, addToSet));
        }
        return groupEl;
    }

    private enum Mode {
        ALL, APP, ADMIN;
    }
}
