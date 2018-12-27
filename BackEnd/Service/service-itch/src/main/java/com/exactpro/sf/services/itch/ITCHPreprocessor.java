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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.util.LRUMap;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;


public class ITCHPreprocessor extends DefaultPreprocessor {

    private static final Logger logger = LoggerFactory.getLogger(ITCHPreprocessor.class);

	private final LRUMap<BigDecimal, Long> instruments = new LRUMap<>(10_000);

	@Override
	public void process(IMessage message, IoSession session, IMessageStructure structure) {
        super.process(message, session, structure);
        // RM 25262
		String msgName = message.getName();

		if (msgName.equals("AddOrder") || msgName.equals("AddAttributedOrder")) {
			BigDecimal orderID = (BigDecimal) message.getField("OrderID");
			Long instrumentID = (Long) message.getField("InstrumentID");

			instruments.put(orderID, instrumentID);
		} else if (msgName.equals("OrderModified") || msgName.equals("OrderExecuted") || msgName.equals("OrderExecutedWithPrice_Size")) {
			BigDecimal orderID = (BigDecimal) message.getField("OrderID");
			Long instrumentID = instruments.get(orderID);

			if (instrumentID != null) {
				message.addField("FakeInstrumentID", instrumentID);
			} else {
				logger.error("InstrumentID is null. MsgName: {}, OrderID: {}", msgName, orderID);
			}
		}

	}

}
