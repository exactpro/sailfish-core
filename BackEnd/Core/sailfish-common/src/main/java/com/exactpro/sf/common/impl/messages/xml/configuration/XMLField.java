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
package com.exactpro.sf.common.impl.messages.xml.configuration;

import com.exactpro.sf.common.impl.messages.all.configuration.IAttribute;
import com.exactpro.sf.common.impl.messages.all.configuration.IField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;


/**
 * Json class for adapter for {@link Field}.
 */
public class XMLField implements IField {

    private static final long serialVersionUID = -140962765064077051L;

    protected final String id;
    protected final String name;
    protected final String description;
    protected final List<XMLAttribute> attributes;
    protected final List<XMLAttribute> values;
    protected final String defaultValue;
    protected final JavaType type;
    protected XMLField reference;
    protected final Boolean isServiceName;
    protected final Boolean isCollection;
    protected final Boolean required;

    private static List<XMLAttribute> createAdaptersForAttributes(List<Attribute> attributes) {
        if (attributes != null && attributes.size() > 0) {
            List<XMLAttribute> resultList = new ArrayList<>(attributes.size());
            for (Attribute attribute : attributes) {
                resultList.add(new XMLAttribute(attribute));
            }
            return resultList;
        } else {
            return Collections.emptyList();
        }
    }

    public XMLField(Field field) {

        this.id = field.getId();
        this.name = field.getName();
        this.description = field.getDescription();

        this.attributes = createAdaptersForAttributes(field.getAttributes());
        this.values = createAdaptersForAttributes(field.getValues());

        this.defaultValue = field.getDefaultvalue();
        this.type = field.getType();
        this.isServiceName = field.isIsServiceName();
        this.isCollection = field.isIsCollection();
        this.required = field.isRequired();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<XMLAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public List<XMLAttribute> getValues() {
        return values;
    }

    @Override
    public boolean isIsServiceName() {
        return isServiceName;
    }

    @Override
    public boolean isIsCollection() {
        return isCollection;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public JavaType getType() {
        return type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IField getReference() {
        return reference;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    public void setReference(XMLField reference) {
        this.reference = reference;
    }
}
