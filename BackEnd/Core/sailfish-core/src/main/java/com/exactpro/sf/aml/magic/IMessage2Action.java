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
package com.exactpro.sf.aml.magic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.common.messages.IMessage;

public class IMessage2Action {

	public static List<AMLElement> convert(final IMessage message) {
		// dumb implementation:
		List<AMLElement> result = convertImessageFields(message);

		AMLElement action = result.get(result.size() - 1);

		action.setValue(Column.Action, "receive");

		action.setValue(Column.ServiceName, message.getMetaData().getToService());

		action.setValue(Column.Dictionary, message.getMetaData().getMsgNamespace());

		action.setValue(Column.MessageType, message.getName());

		return result;
	}

	private static List<AMLElement> convertImessageFields(final IMessage message) {
		List<AMLElement> result = new ArrayList<>();

		AMLElement action = new AMLElement();

		action.setValue(Column.Dictionary, message.getMetaData().getMsgNamespace());
		action.setValue(Column.MessageType, message.getName());

		for (String fieldName : message.getFieldNames()) {
			Object field = message.getField(fieldName);
			if (field instanceof IMessage) {
				result.addAll(convertImessageFields((IMessage) field));
			} else if (field instanceof Map<?, ?>) {
				result.addAll(convertImessageFields((IMessage) field));
			} else if  (field instanceof Collection<?>) {
				for (Object obj : (Collection<?>) field) {
					if (obj instanceof IMessage) {
						result.addAll(convertImessageFields((IMessage) obj));
					}
					else {
						action.setValue(fieldName, obj.toString());
					}
				}
			} else {
				action.setValue(fieldName, field.toString());
			}
		}

		result.add(action);
		return result;
	}

}
