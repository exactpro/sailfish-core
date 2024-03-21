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
package com.exactpro.sf.services.itch;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.codecs.ICodecSettings;

public class ITCHCodecSettings implements ICodecSettings {

	private byte[] chunkDelimiter;

	private int msgLength;

	private String filterValues;

	private SailfishURI dictionaryURI;

    private boolean preprocessingEnabled = true;
    private boolean evolutionSupportEnabled;
    private boolean trimLeftPaddingEnabled = true;

    /**
     * Wraps result messages to {@link ITCHMessageHelper#MESSAGELIST_NAME} message.
     * Adds them as a list to the {@link ITCHMessageHelper#SUBMESSAGES_FIELD_NAME} field in this message.
     */
    private boolean wrapMessages = true;

    /**
     * The decoded messages with have the format that is supported by th2
     */
    private boolean evolutionaryOutput = false;

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

	@Override
	public String getFilterValues() {
		return filterValues;
	}

	public void setFilterValues(String filterValues) {
		this.filterValues = filterValues;
	}

	@Override
	public SailfishURI getDictionaryURI() {
		return dictionaryURI;
	}

	public void setDictionaryURI(SailfishURI dictionaryURI) {
		this.dictionaryURI = dictionaryURI;
	}

    public boolean isPreprocessingEnabled() {
        return preprocessingEnabled;
    }

    public void setPreprocessingEnabled(boolean preprocessingEnabled) {
        this.preprocessingEnabled = preprocessingEnabled;
    }

    @Override
    public boolean isEvolutionSupportEnabled() {
        return evolutionSupportEnabled;
    }

    public void setEvolutionSupportEnabled(boolean evolutionSupportEnabled) {
        this.evolutionSupportEnabled = evolutionSupportEnabled;
    }

    public boolean isTrimLeftPaddingEnabled() {
        return trimLeftPaddingEnabled;
    }

    public void setTrimLeftPaddingEnabled(boolean trimLeftPaddingEnabled) {
        this.trimLeftPaddingEnabled = trimLeftPaddingEnabled;
    }

    public boolean isWrapMessages() {
        return wrapMessages;
    }

    public void setWrapMessages(boolean wrapMessages) {
        this.wrapMessages = wrapMessages;
    }

    public boolean isEvolutionaryOutput() {
        return evolutionaryOutput;
    }

    public void setEvolutionaryOutput(boolean evolutionaryOutput) {
        this.evolutionaryOutput = evolutionaryOutput;
    }
}
