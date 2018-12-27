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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.suri.SailfishURI;
import org.apache.commons.configuration.HierarchicalConfiguration;

public class ITCHCodecSettings implements ICommonSettings {

	private byte[] chunkDelimiter;

	private int msgLength;

	private String filterValues;

	private SailfishURI dictionaryURI;

    public ITCHCodecSettings(int msgLength) {
		this.msgLength = msgLength;
	}

    public ITCHCodecSettings() {
		this.msgLength = 0;
	}

	@Override
	public void load(HierarchicalConfiguration config) {
		// TODO Auto-generated method stub

	}

	public int getMsgLength() {
		return msgLength;
	}

	public void setMsgLength(int msgLength) {
		this.msgLength = msgLength;
	}

	public byte[] getChunkDelimiter() {
		return chunkDelimiter;
	}

	public void setChunkDelimiter(byte[] chunkDelimiter) {
		this.chunkDelimiter = chunkDelimiter;
	}

	public String getFilterValues() {
		return filterValues;
	}

	public void setFilterValues(String filterValues) {
		this.filterValues = filterValues;
	}

	public SailfishURI getDictionaryURI() {
		return dictionaryURI;
	}

	public void setDictionaryURI(SailfishURI dictionaryURI) {
		this.dictionaryURI = dictionaryURI;
	}

}
