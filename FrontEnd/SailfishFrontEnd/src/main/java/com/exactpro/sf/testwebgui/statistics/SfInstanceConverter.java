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
package com.exactpro.sf.testwebgui.statistics;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;

@ManagedBean(name="sfInstanceConverter")
@ApplicationScoped
@SuppressWarnings("serial")
public class SfInstanceConverter implements Converter, Serializable {
	
	@ManagedProperty(value="#{sfContext}")
	private ISFContext sfContext;
	
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String value) {
		
		if (value == null || !value.matches("\\d+")) {
            return null;
        }        

		SfInstance result = this.sfContext.getStatisticsService().getStorage().getSfInstanceById(Long.valueOf(value));

        if (result == null) {
            throw new ConverterException(new FacesMessage("Unknown Account ID: " + value));
        }

        return result;
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object value) {
		
		if (!(value instanceof SfInstance)) {
            return null;
        }

        return String.valueOf(((SfInstance) value).getId());
	}

	public void setSfContext(ISFContext sfContext) {
		this.sfContext = sfContext;
	}
	
}