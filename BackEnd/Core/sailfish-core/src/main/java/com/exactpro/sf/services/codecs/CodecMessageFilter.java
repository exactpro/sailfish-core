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
package com.exactpro.sf.services.codecs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * The {@code CodecMessageFilter} class for filters messages.
 */

public class CodecMessageFilter {
    
	private static final Logger logger = LoggerFactory.getLogger(CodecMessageFilter.class);
    
	private static final String DROP_SUB_FILTER = "DropSubFilter";
    private static final String DELIMITER = ",";
    private final Map<IMessageStructure, Multimap<SpecialAttribute, String>> messageSpecialField;

    /**
     * Allowed values for fields.
     */
    private final Set<String> filterValues;

    public CodecMessageFilter(String filterValuesString) {
        messageSpecialField = new HashMap<>();
        filterValues = parseFilterValues(filterValuesString);

        logger.info("Filter values: {}", this.filterValues);

    }

    public void init(IDictionaryStructure dictionary){
        for (IMessageStructure msgStruct : dictionary.getMessageStructures()) {
            if (!filterValues.isEmpty()) {
                for (IFieldStructure fieldStructure : msgStruct.getFields()) {
                    for (SpecialAttribute specialAttribute : SpecialAttribute.values()) {
                        if (fieldStructure.getAttributeValueByName(specialAttribute.getAttribut()) != null) {
                            Multimap<SpecialAttribute, String> attributeToField = messageSpecialField.get(msgStruct);
                            if (attributeToField == null) {
                                attributeToField =  HashMultimap.create();
                                messageSpecialField.put(msgStruct, attributeToField);
                            }

                            attributeToField.put(specialAttribute, fieldStructure.getName());
                        }
                    }
                }
            }
        }
    }


    public enum SpecialAttribute {
        SUB_FILTER("SubFilter"),
        FILTER("Filter");

        private final String attribut;

        private SpecialAttribute(String attribut) {
            this.attribut = attribut;
        }

        public String getAttribut() {
            return this.attribut;
        }
    }

    /**
     * Returns true if there is no allowed value in any filtered field.
     */
    public boolean dropMessage(IoSession session, IMessageStructure messageStructure, IMessage message) {
        if (!filterValues.isEmpty()) {
            Multimap<SpecialAttribute, String> attributeToField = messageSpecialField.get(messageStructure);
            if (attributeToField != null) {
                if (attributeToField.containsKey(SpecialAttribute.SUB_FILTER)) {
                    Set<Object> subFilters = new HashSet<>();
                    @SuppressWarnings("unchecked") Set<Object> dropSubFilterSet = (Set<Object>) session.getAttribute(DROP_SUB_FILTER);

                    for (String fieldSubFilter : attributeToField.get(SpecialAttribute.SUB_FILTER)) {
                        subFilters.add(message.getField(fieldSubFilter));
                    }

                    if (attributeToField.containsKey(SpecialAttribute.FILTER)) {

                        if (dropSubFilterSet == null) {
                            dropSubFilterSet = new HashSet<Object>();
                            session.setAttribute(DROP_SUB_FILTER, dropSubFilterSet);
                        }

                        for (String fieldFilter : attributeToField.get(SpecialAttribute.FILTER)) {
                            Object filterValue = message.getField(fieldFilter);
                            filterValue = filterValue != null ? filterValue.toString() : null;
                            if (filterValues.contains(filterValue)) {
                                dropSubFilterSet.addAll(subFilters);
                                return false;
                            }
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("Message has been dropped. MsgName={}.{}.{}", message.getName(),
                                    getValues(message, attributeToField.get(SpecialAttribute.FILTER)),
                                    getValues(message, attributeToField.get(SpecialAttribute.SUB_FILTER)));
                        }
                        return true;

                    } else {
                        if (dropSubFilterSet != null && CollectionUtils.containsAny(dropSubFilterSet, subFilters)){
                            return false;
                        }

                    }

                    logger.debug("Message has been dropped. MsgName={}.{}", message.getName(), getValues(message, attributeToField.get(SpecialAttribute.SUB_FILTER)));
                    return true;
                } else if (attributeToField.containsKey(SpecialAttribute.FILTER)) {
                    for (String field : attributeToField.get(SpecialAttribute.FILTER)) {
                        Object filterValue = message.getField(field);
                        filterValue = filterValue != null ? filterValue.toString() : null;
                        if (filterValues.contains(filterValue)) {
                            return false;
                        }
                    }
                    logger.debug("Message has been dropped. MsgName={}.{}", message.getName(), getValues(message, attributeToField.get(SpecialAttribute.FILTER)));
                    return true;
                }
            }

        }
        return false;
    }

    private String getValues(IMessage message, Collection<String> fields){
        StringBuilder values = new StringBuilder();
        for (String field : fields) {
            values.append(field);
            values.append("=");
            Object filterValue = message.getField(field);
            filterValue = filterValue != null ? filterValue.toString() : null;
            values.append(filterValue);
            values.append(".");
        }
        values.deleteCharAt(values.length() - 1);
        return values.toString();
    }

    protected Set<String> parseFilterValues(String value) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                String[] array = value.split(DELIMITER);
                Set<String> result = new HashSet<String>();
                for (String instrumentID : array) {
                    result.add(instrumentID.trim());
                }
                return result;
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
                throw new EPSCommonException("Parse 'Filter values' failed");
            }
        }
        return Collections.emptySet();
    }
}
