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
package com.exactpro.sf.services.fast;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.RequiredParam;

public class FASTCodecSettings extends AbstractServiceSettings {

    private static final long serialVersionUID = 3426549826737983629L;

    private int skipInitialByteAmount = 0;
	private boolean streamBlockEncoded = true;
	private boolean resetContextAfterEachUdpPacket = true;
	@RequiredParam
	@Description("Dictionary title")
	private SailfishURI dictionaryName;

	public FASTCodecSettings() {
		super();
	}

	public boolean isStreamBlockEncoded() {
		return this.streamBlockEncoded;
	}

	public void setStreamBlockEncoded(boolean streamBlockEncoded) {
		this.streamBlockEncoded = streamBlockEncoded;
	}

	public void setResetContextAfterEachUdpPacket(boolean resetContextAfterEachUdpPacket) {
		this.resetContextAfterEachUdpPacket = resetContextAfterEachUdpPacket;
	}

    public int getSkipInitialByteAmount() {
        return skipInitialByteAmount;
    }

    public void setSkipInitialByteAmount(int skipInitialByteAmount) {
        this.skipInitialByteAmount = skipInitialByteAmount;
    }

	public boolean isResetContextAfterEachUdpPacket() {
		return resetContextAfterEachUdpPacket;
	}

	@Override
	public void load(HierarchicalConfiguration config) {
	}

    @Override
    public SailfishURI getDictionaryName() {
        return dictionaryName;
    }

    @Override
    public void setDictionaryName(SailfishURI dictionaryName) {
        this.dictionaryName = dictionaryName;
    }
}