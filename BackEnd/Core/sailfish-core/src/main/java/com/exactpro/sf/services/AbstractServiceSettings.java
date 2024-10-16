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
package com.exactpro.sf.services;

import java.io.Serializable;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.configuration2.HierarchicalConfiguration;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.Ignore;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIAdapter;
import com.exactpro.sf.services.util.ServiceUtil;
import org.apache.commons.configuration2.tree.ImmutableNode;

@SuppressWarnings("serial")
public abstract class AbstractServiceSettings implements IServiceSettings, Serializable {

    @Description("These message types will be stored for checking by matrix actions and presentation in report.\n"
            + "Example: 'MessageA, MessageB, MessageC' or '"+ ServiceUtil.ALIAS_PREFIX + "dataA'. The Enable Invert Saved Message Types option changes the behavior. Listed messages will be excluded")
    protected String storedMessageTypes;

    @Description("This option inverts the behavior of Stored Message Types. If selected, messages listed in Stored Message Types will be excluded.")
    protected boolean invertStoredMessageTypes;

    @Description("How long to wait in milliseconds before attempting to start next service")
    protected long expectedTimeOfStarting = 2000;

    @Description("How long to wait in milliseconds before attempting to start a service")
    protected long waitingTimeBeforeStarting;

    @Description("User comment")
    protected String comment;

    @Description("Network traffic will be recorded for this service's lifetime and for each test script run using it")
	private boolean performDump;

    @Description("Determine will be messages stored or not. " +
            "Note: this setting will not affect mechanism of receiving message from service")
    private boolean persistMessages = true;

    @Description("Determines whether or not service should be managed by auto-start feature")
    private boolean autoStartable = true;

    @Description("Send message timeout in milliseconds used for every sending operation via current service. "
            + "It should be greater than zero, default value is 1000")
    private long sendMessageTimeout = 1000;

    @Ignore
    private boolean evolutionSupportEnabled;

    public String getStoredMessageTypes() {
        return storedMessageTypes;
    }

    public void setStoredMessageTypes(String processedMessageTypes) {
        this.storedMessageTypes = processedMessageTypes;
    }

    @Override
    public boolean isInvertStoredMessageTypes() {
        return invertStoredMessageTypes;
    }

    public void setInvertStoredMessageTypes(boolean invertStoredMessageTypes) {
        this.invertStoredMessageTypes = invertStoredMessageTypes;
    }
    
    public long getExpectedTimeOfStarting() {
        return expectedTimeOfStarting;
    }

    public void setExpectedTimeOfStarting(long expectedTimeOfStarting) {
        this.expectedTimeOfStarting = expectedTimeOfStarting;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public long getWaitingTimeBeforeStarting() {
        return waitingTimeBeforeStarting;
    }

    public void setWaitingTimeBeforeStarting(long expectedTimeBeforeStarting) {
        this.waitingTimeBeforeStarting = expectedTimeBeforeStarting;
    }

    // getter and setter are here because we don't want to annotate setter in every subclass
    public abstract SailfishURI getDictionaryName();

    @XmlJavaTypeAdapter(SailfishURIAdapter.class)
    public abstract void setDictionaryName(SailfishURI dictionaryName);

    public void load(HierarchicalConfiguration<ImmutableNode> cfg) {
    	this.expectedTimeOfStarting = cfg.getLong("expectedTimeOfStarting", 0L);
    	this.waitingTimeBeforeStarting = cfg.getLong("waitingTimeBeforeStarting", 0L);
    	this.comment = cfg.getString("comment", null);
    }

	public boolean isPerformDump() {
		return performDump;
	}

	public void setPerformDump(boolean performDump) {
		this.performDump = performDump;
	}

    @Override
    public boolean isPersistMessages() {
        return persistMessages;
    }

    public void setPersistMessages(boolean persistMessages) {
        this.persistMessages = persistMessages;
    }

    @Override
    public boolean isAutoStartable() {
        return autoStartable;
    }

    public void setAutoStartable(boolean autoStartable) {
        this.autoStartable = autoStartable;
    }

    public long getSendMessageTimeout() {
        return sendMessageTimeout;
    }

    public void setSendMessageTimeout(long sendMessageTimeout) {
        this.sendMessageTimeout = sendMessageTimeout;
    }

    @Override
    public boolean isEvolutionSupportEnabled() {
        return evolutionSupportEnabled;
    }

    public void setEvolutionSupportEnabled(boolean evolutionSupportEnabled) {
        this.evolutionSupportEnabled = evolutionSupportEnabled;
    }
}
