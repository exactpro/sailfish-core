/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fake;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration2.HierarchicalConfiguration;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.externalapi.DictionaryProperty;
import com.exactpro.sf.externalapi.DictionaryType;
import com.exactpro.sf.services.AbstractServiceSettings;
import org.apache.commons.configuration2.tree.ImmutableNode;

@XmlRootElement
public class FakeSettings extends AbstractServiceSettings {
    private static final long serialVersionUID = 1387730429929582093L;

    @DictionaryProperty(type = DictionaryType.MAIN)
    private SailfishURI dictionaryName;
	private long idleTimeout = 1000;

	@Override
	public void load(HierarchicalConfiguration<ImmutableNode> config)
	{
		try {
            dictionaryName = SailfishURI.parse(config.getString("dictionaryName"));
        } catch(SailfishURIException e) {
            throw new EPSCommonException(e);
        }

		idleTimeout = config.getLong("idleTimeout");
	}

    @Override
    public SailfishURI getDictionaryName() {
        return dictionaryName;
    }

    @Override
    public void setDictionaryName(SailfishURI dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

	public long getIdleTimeout()
	{
		return idleTimeout;
	}

	public void setIdleTimeout(long idleTimeout)
	{
		this.idleTimeout = idleTimeout;
	}
}
