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
package com.exactpro.sf.testwebgui.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.apache.commons.lang3.StringUtils;

@FacesConverter("sailfish.IterableConverter")
public class IterableConverter implements Converter {

    private String separator;
    private CollectionType type;
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (separator == null) {
            throw new ConverterException(new FacesMessage("Converter problem", "Set separator for converter"));
        }
        if (type == null) {
            throw new ConverterException(new FacesMessage("Converter problem", "Set type for converter"));
        }
        return StringUtils.isBlank(value) ? null :
                Arrays.stream(value.split(this.separator))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(this.type.collector());
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value instanceof Iterable<?>) {
            Iterable<?> iterable = (Iterable<?>) value;
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
        }
        return "";
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
    
    public String getType() {
        return type.name();
    }
    
    public void setType(String type) {
        this.type = Enum.valueOf(CollectionType.class, type);
    }
    
    public static enum CollectionType {
        SET {
            @Override
            public <T> Collector<T, ?, Set<T>> collector() {
                return Collectors.toSet();
            }
        },
        LIST {
            @Override
            public <T> Collector<T, ?, List<T>> collector() {
                return Collectors.toList();
            }
        };

        public abstract <T> Collector<T, ?, ? extends Iterable<T>> collector();
    }
}
