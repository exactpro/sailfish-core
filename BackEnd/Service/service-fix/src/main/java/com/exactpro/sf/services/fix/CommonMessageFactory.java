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
package com.exactpro.sf.services.fix;

import quickfix.DataDictionary;
import quickfix.DataDictionary.GroupInfo;
import quickfix.DataDictionaryProvider;
import quickfix.FieldException;
import quickfix.Group;
import quickfix.Message;
import quickfix.Message.Header;
import quickfix.MessageFactory;
import quickfix.MessageUtils;
import quickfix.field.ApplVerID;
import quickfix.field.BeginString;
import quickfix.field.MsgType;

public class CommonMessageFactory implements MessageFactory {

	DataDictionaryProvider dictionaryProvider;
	public CommonMessageFactory(DataDictionaryProvider dictionaryProvider) {
		this.dictionaryProvider = dictionaryProvider;
	}

	@Override
	public Message create(String beginString, String msgType) {
		Message msg = new Message();
		Header hdr = msg.getHeader();
		hdr.setField(new BeginString(beginString));
		hdr.setField(new MsgType(msgType));
		return msg;
	}

	@Override
	public Group create(String beginString, String msgType,
			int correspondingFieldID) {
		DataDictionary dictionary = dictionaryProvider.getSessionDataDictionary(beginString);
		try {
			dictionary.checkMsgType(msgType);
		} catch (FieldException e) {
			ApplVerID applVerId = MessageUtils.toApplVerID(beginString);
			dictionary = dictionaryProvider.getApplicationDataDictionary(applVerId);
		}
		GroupInfo grpInfo = dictionary.getGroup(msgType, correspondingFieldID);
        Group grp = new Group(correspondingFieldID, grpInfo.getDelimiterField());
		return grp;
	}

}
