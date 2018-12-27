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
package com.exactpro.sf.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.dom4j.QName;
import org.dom4j.dom.DOMElement;
import org.w3c.dom.Element;

import com.exactpro.sf.configuration.suri.SailfishURI;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class DisabledServiceSettings implements IServiceSettings, Serializable {
    private static final long serialVersionUID = 7271664531171177700L;
    private final List<Element> entries = new ArrayList<>();

    public DisabledServiceSettings() {}

    public DisabledServiceSettings(Map<String, String> settings) {
        setSettings(settings);
    }

    @Override
    public String getComment() {
        return null;
    }

    @Override
    public SailfishURI getDictionaryName() {
        return null;
    }

    @Override
    public void setDictionaryName(SailfishURI dictionaryName) {

    }

    @Override
    public long getExpectedTimeOfStarting() {
        return 0;
    }

    @Override
    public long getWaitingTimeBeforeStarting() {
        return 0;
    }

    @Override
    public boolean isPerformDump() {
        return false;
    }

    @Override
    public String getStoredMessageTypes() {
        return null;
    }

    @Override
    public boolean isPersistMessages() {
        return true;
    }

    @Override
    public void load(HierarchicalConfiguration config) {

    }

    @XmlTransient
    public Map<String, String> getSettings() {
        return convertJAXBElementListToMap();
    }

    public void setSettings(Map<String, String> settings) {
        this.entries.clear();
        convertMapToElementList(settings);
    }

    @XmlAnyElement
    public List<Element> getEntries() {
        return entries;
    }

    private void convertMapToElementList(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            DOMElement domElement = new DOMElement(new QName(entry.getKey()));
            domElement.setText(entry.getValue());
            entries.add(domElement);
        }
    }

    private Map<String, String> convertJAXBElementListToMap() {
        Map<String, String> settings = new HashMap<>();
        for (Element element : entries) {
            if (element != null && element.getFirstChild() != null) {
                settings.put(element.getTagName(), element.getFirstChild().getNodeValue());
            }
        }
        return settings;
    }
}
