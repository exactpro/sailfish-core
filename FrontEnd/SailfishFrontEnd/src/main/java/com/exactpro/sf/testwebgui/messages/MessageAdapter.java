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
package com.exactpro.sf.testwebgui.messages;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.storage.MessageRow;

public class MessageAdapter implements Serializable {

	private static final long serialVersionUID = 5771172503053380219L;

	private Long id;
	private String timestamp;
	private String name;
	private String from;
	private String to;
	private String json;
	private String humanReadable;
	private String rawMessage;
	private String namespace;
	private String fields;
    private String printableMessage;
    private String rejectReason;

	public MessageAdapter() {
		id = 0L;
		timestamp = "Time";
		name = "Name";
		from = "From";
		to = "to";
		humanReadable = "Content";
		json = "{}"; 
		rawMessage = Arrays.toString(json.getBytes());

	}

	public MessageAdapter(MessageRow args) {
		id = Long.parseLong(args.getID());
		name = StringUtils.defaultString(args.getMsgName(), " ");
		namespace = StringUtils.defaultString(args.getMsgNamespace(), " ");
		timestamp = args.getTimestamp();
		from = StringUtils.defaultString(args.getFrom(), " ");
		to = StringUtils.defaultString(args.getTo(), " ");
		humanReadable = StringUtils.defaultString(args.getContent(), " ");
		json = StringUtils.defaultString(args.getJson(), humanReadable);//FIXME: This code used for backward compatibility, refactore after a few months
		rawMessage = args.getRawMessage();
		printableMessage = args.getPrintableMessage();
		fields = MessageParser.parseFields(json);
		rejectReason = StringUtils.defaultString(args.getRejectReason(), "");
	}

	public MessageAdapter(Long id, String ts, String n, String f, String t, String c) {
		this.id= id;
		this.timestamp = ts;
		this.name = n;
		this.from = f;
		this.to = t;
		this.json = c;
		this.rawMessage = Arrays.toString(json.getBytes());
	}

	public String getRawMessage() {
		return rawMessage;
	}

	public void setRawMessage(String rawMessage) {
		this.rawMessage = rawMessage;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String content) {
		this.json = content;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getFields() {
		return fields;
	}

	public void setFields(String fields) {
		this.fields = fields;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public String getHumanReadable() {
        return humanReadable;
    }

    public void setHumanReadable(String humanReadable) {
        this.humanReadable = humanReadable;
    }

    public String getPrintableMessage() {
        return printableMessage == null ? rawMessage : printableMessage;
    }

    public void setPrintableMessage(String printableMessage) {
        this.printableMessage = printableMessage;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
