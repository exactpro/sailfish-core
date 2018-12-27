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
package com.exactpro.sf.common.messages;

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MsgMetaData implements Cloneable {
    // unique (during SailFish run time). Mapped to 'StoredId' in StoredMessage
    private final long id = MessageUtil.generateId();
    private Date msgTimestamp;
	private String msgNamespace;
	private String msgName;

	private String fromService;
	private String toService;
	private boolean isAdmin;
	private boolean isRejected;
	private String rejectReason;
    private boolean isDirty;

	private byte[] rawMessage;
	private ServiceInfo serviceInfo;
	private SailfishURI dictionaryURI;
    private String protocol;

	@JsonCreator
	public MsgMetaData(@JsonProperty("namespace") String namespace, @JsonProperty("name") String name, @JsonProperty("msgTimestamp") Date msgTimestamp) {
	    this.msgNamespace = namespace;
	    this.msgName = name;
	    this.msgTimestamp = msgTimestamp;
	}

	public MsgMetaData(final String namespace, final String name) {
		this(namespace, name, new Date());
	}

	public long getId() {
		return id;
	}

	public String getFromService() {
		return fromService;
	}

	public void setFromService(String fromService) {
		this.fromService = fromService;
	}

	public String getToService() {
		return toService;
	}

	public void setToService(String toService) {
		this.toService = toService;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

    public boolean isRejected() {
        return isRejected;
    }

    public void setRejected(boolean isRejected) {
        this.isRejected = isRejected;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public Date getMsgTimestamp() {
		return msgTimestamp;
	}

	public String getMsgNamespace() {
		return msgNamespace;
	}

	public String getMsgName() {
		return msgName;
	}

	public byte[] getRawMessage() {
		return this.rawMessage;
	}

	public void setRawMessage(byte[] value) {
		this.rawMessage = value;
	}

	public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public SailfishURI getDictionaryURI() {
        return dictionaryURI;
    }

    public void setDictionaryURI(SailfishURI dictionaryURI) {
        this.dictionaryURI = dictionaryURI;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public MsgMetaData clone() {
	    MsgMetaData metaData = new MsgMetaData(msgNamespace, msgName, msgTimestamp);

        metaData.setAdmin(isAdmin);
        metaData.setRejected(isRejected);
        metaData.setDirty(isDirty);
	    metaData.setFromService(fromService);
	    metaData.setToService(toService);
	    metaData.setServiceInfo(serviceInfo);
        metaData.setDictionaryURI(dictionaryURI);
        metaData.setProtocol(protocol);

	    if(rawMessage != null) {
	        metaData.setRawMessage(Arrays.copyOf(rawMessage, rawMessage.length));
	    }

	    return metaData;
	}

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }

        if(!(obj instanceof MsgMetaData)) {
            return false;
        }

        MsgMetaData that = (MsgMetaData)obj;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(this.msgName, that.msgName);
        builder.append(this.msgNamespace, that.msgNamespace);
        builder.append(this.msgTimestamp, that.msgTimestamp);
        builder.append(this.fromService, that.fromService);
        builder.append(this.toService, that.toService);
        builder.append(this.isAdmin, that.isAdmin);
        builder.append(this.isRejected, that.isRejected);
        builder.append(this.isDirty, that.isDirty);
        builder.append(this.rawMessage, that.rawMessage);
        builder.append(this.serviceInfo, that.serviceInfo);
        builder.append(this.dictionaryURI, that.dictionaryURI);

        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(this.msgName);
        builder.append(this.msgNamespace);
        builder.append(this.msgTimestamp);
        builder.append(this.fromService);
        builder.append(this.toService);
        builder.append(this.isAdmin);
        builder.append(this.isRejected);
        builder.append(this.isDirty);
        builder.append(this.rawMessage);
        builder.append(this.serviceInfo);
        builder.append(this.dictionaryURI);

        return builder.toHashCode();
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
        if (rejectReason != null) {
            setRejected(true);
        }
    }
}
