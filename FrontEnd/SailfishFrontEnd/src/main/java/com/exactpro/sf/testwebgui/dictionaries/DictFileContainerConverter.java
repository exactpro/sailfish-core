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
package com.exactpro.sf.testwebgui.dictionaries;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.servlet.http.HttpSession;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.testwebgui.BeanUtil;

@FacesConverter("dictFileContainerConverter")
public class DictFileContainerConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {

        DictionaryEditorModel model = BeanUtil.getSessionModelsMapper().getDictionaryEditorModel(
                (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false));

        if (model == null) {
            return null;
        }

        try {
            return model.getFileContainerByURI(SailfishURI.parse(arg2));
        } catch(SailfishURIException e) {
            return null;
        }
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {

		if (arg2 instanceof String) return (String)arg2;

		return ((DictFileContainer) arg2).getURI().toString();
	}
}