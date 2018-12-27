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
package com.exactpro.sf.services.mina;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.RequiredParam;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public abstract class AbstractMINASettings extends AbstractServiceSettings {
    private static final long serialVersionUID = -4850020892139075669L;

    @RequiredParam
    protected SailfishURI dictionaryName;

    @Override
    public SailfishURI getDictionaryName() {
        return dictionaryName;
    }

    @Override
    public void setDictionaryName(SailfishURI dictionaryName) {
        this.dictionaryName = dictionaryName;
    }
}
