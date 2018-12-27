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
package com.exactpro.sf.services.netty;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.ServiceException;

@XmlRootElement
public abstract class NettyClientSettings extends AbstractServiceSettings {
    private static final long serialVersionUID = -8964085667583828258L;

    @Description("Idle timeout (minutes)")
	protected int idleTimeout;

	@Description("Dictionary name")
	protected SailfishURI dictionaryName;

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	@Override
	public SailfishURI getDictionaryName() {
		return dictionaryName;
	}

	@Override
	public void setDictionaryName(SailfishURI dictionaryName) {
		this.dictionaryName = dictionaryName;
	}

	@Override
	public void load(HierarchicalConfiguration cfg) {
		super.load(cfg);
		this.idleTimeout = cfg.getInt("idleTimeout");

		try {
            this.dictionaryName = SailfishURI.parse(cfg.getString("dictionaryName"));
        } catch(SailfishURIException e) {
            throw new ServiceException(e);
        }
	}

}
