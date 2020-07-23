/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.actions;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@MatrixUtils
@ResourceAliases("XMLUtil")
public class XMLUtil extends AbstractCaller {
    private final XPathFactory xpathFactory = XPathFactory.newInstance();
    private final DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

    @Description("Returns a xml document with new value of selected node.<br>" +
            "<b>content</b> - xml document<br>" +
            "<b>xpath</b> - path to node<br>" +
            "<b>value</b> - new value of node<br>" +
            "Example:<br/>" +
            "#{setValueByXPath(content, xpath, value)}")
    @UtilityMethod
    public String setValueByXPath(String content, String xpath, String value) {
        try {
            DocumentBuilder docBuilder = documentFactory.newDocumentBuilder();

            Document doc = docBuilder.parse(new InputSource(new StringReader(content)));
            XPath xPath = xpathFactory.newXPath();
            NodeList nodes = (NodeList)xPath.compile(xpath).evaluate(doc, XPathConstants.NODESET);

            if (nodes.getLength() != 1) {
                throw new EPSCommonException("Should be selected only one node to set value.");
            }
            if (!hasOnlyTextNode(nodes.item(0))) {
                throw new EPSCommonException("The node has child nodes." + System.lineSeparator() + nodeToString(nodes.item(0)));
            }

            Node node = nodes.item(0);
            Node oldChild = nodeListToList(node.getChildNodes(), x -> x.getNodeType() != Node.COMMENT_NODE).get(0);
            Node newChild = doc.createTextNode(value);
            node.replaceChild(newChild, oldChild);

            return nodeToString(doc);
        } catch (SAXException | IOException e) {
            throw new EPSCommonException("There is a problem with input data.", e);
        } catch (ParserConfigurationException | XPathExpressionException e) {
            throw new EPSCommonException(e);
        }
    }

    @Description("Returns a value of node by xpath.<br>" +
            "<b>content</b> - xml document<br>" +
            "<b>xpath</b> - path to node<br>" +
            "Example:<br/>" +
            "#{getObjectByXPath(content, xpath)}")
    @UtilityMethod
    public String getValueByXPath(String content, String xpath) {
        NodeList nodes = getNodesByXPath(content, xpath);
        if (nodes.getLength() != 1 || !hasOnlyTextNode(nodes.item(0))) {
            throw new EPSCommonException("The node has child nodes." + System.lineSeparator() + nodeToString(nodes.item(0)));
        }
        return nodes.item(0).getTextContent();
    }

    @Description("Returns a object by xpath.<br>" +
            "<b>content</b> - xml document<br>" +
            "<b>xpath</b> - path to object<br>" +
            "Example:<br/>" +
            "#{getObjectByXPath(content, xpath)}")
    @UtilityMethod
    public Map<String, ?> getObjectByXPath(String content, String xpath) {
        NodeList nodes = getNodesByXPath(content, xpath);

        if (nodes.getLength() == 0) {
            return Collections.emptyMap();
        }
        if (nodes.getLength() != 1) {
            throw new EPSCommonException("There are several nodes for this xpath \"" + xpath + "\".");
        }
        if (hasOnlyTextNode(nodes.item(0))) {
            throw new EPSCommonException("Node isn't object.");
        }

        return nodeToMap(nodes.item(0));
    }

    @Description("Returns a list values by xpath.<br>" +
            "<b>content</b> - xml document<br>" +
            "<b>xpath</b> - path to list<br>" +
            "Example:<br/>" +
            "#{getListByXPath(content, xpath)}")
    @UtilityMethod
    public List<String> getListByXPath(String content, String xpath) {
        NodeList nodes = getNodesByXPath(content, xpath);
        if (nodes.getLength() == 0) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for(Node elem : nodeListToList(nodes, x -> x.getNodeType() != Node.COMMENT_NODE)) {
            if (!hasOnlyTextNode(elem)) {
                throw new EPSCommonException("The node has child nodes." + System.lineSeparator() + nodeToString(elem));
            }
            result.add(elem.getTextContent());
        }

        return result;
    }

    private NodeList getNodesByXPath(String content, String xpath) {
        try {
            DocumentBuilder docBuilder = documentFactory.newDocumentBuilder();

            Document doc = docBuilder.parse(new InputSource(new StringReader(content)));
            XPath xPath = xpathFactory.newXPath();
            return (NodeList)xPath.compile(xpath).evaluate(doc, XPathConstants.NODESET);
        } catch (SAXException | IOException e) {
            throw new EPSCommonException("There is a problem with input data.", e);
        } catch (ParserConfigurationException | XPathExpressionException e) {
            throw new EPSCommonException(e);
        }
    }

    private static boolean hasOnlyTextNode(Node node) {
        if (!node.hasChildNodes()) {
            return false;
        }
        List<Node> nodes = nodeListToList(node.getChildNodes(),
                x -> x.getNodeType() != Node.COMMENT_NODE);

        return nodes.size() == 1 && nodes.get(0).getNodeType() == Node.TEXT_NODE;
    }

    private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException e) {
            throw new EPSCommonException("Problem with transforming a node to a string.");
        }
    }

    private static Map<String, ?> nodeToMap(Node node) {
        Map<String, Object> map = new HashMap<>();
        if (node.hasChildNodes()) {
            NodeList nodeList = node.getChildNodes();
            for (int i = 0, len = nodeList.getLength(); len > i; i++) {
                Node thisNode = nodeList.item(i);

                if (thisNode.getNodeType() == Node.COMMENT_NODE) {
                    continue;
                }

                Object value = null;
                if (hasOnlyTextNode(thisNode)) {
                    value = thisNode.getTextContent();
                } else {
                    value = nodeToMap(thisNode);
                }

                if (map.containsKey(thisNode.getNodeName())) {
                    Object mValue = map.get(thisNode.getNodeName());
                    if (mValue instanceof List) {
                        ((List<Object>) mValue).add(value);
                    } else {
                        map.put(thisNode.getNodeName(), Lists.newArrayList(mValue, value));
                    }
                } else {
                    map.put(thisNode.getNodeName(), value);
                }
            }
        }
        return map;
    }

    private static List<Node> nodeListToList(NodeList nodeList, Predicate<Node> ... filters) {
        if (nodeList.getLength() == 0) {
            return Collections.emptyList();
        }
        Stream<Node> stream = Stream.iterate(nodeList.item(0), Node::getNextSibling)
                .limit(nodeList.getLength());
        for (Predicate<Node> filter : filters) {
            stream = stream.filter(filter);
        }
        return stream.collect(toList());
    }
}
