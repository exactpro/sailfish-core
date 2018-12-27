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

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@FacesValidator("sailfish.IterableValidator")
public class IterableValidator implements Validator {

    private Pattern pattern = null;
    private boolean disabled = false;
    
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!disabled && value instanceof Iterable<?>) {
            if (this.pattern == null) {
                throw new ValidatorException(new FacesMessage("Validator problem", "Set regex pattern for validator"));
            }
            Iterable<?> iterable = (Iterable<?>) value;
            List<FacesMessage> facesMessages = StreamSupport.stream(iterable.spliterator(), false)
                .map(Object::toString)
                .filter(item -> !this.pattern.matcher(item).matches())
                .map(item -> new FacesMessage("Incorrect value", "Value '"+ item +"' format mismatch"))
                .collect(Collectors.toList());
            
            if (!facesMessages.isEmpty()) {
                throw new ValidatorException(facesMessages);
            }
        }
    }

    public void setPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }
    
    public String getPattern() {
        return pattern.pattern();
    }
    
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    
    public boolean getDisabled() {
        return disabled;
    }
}
