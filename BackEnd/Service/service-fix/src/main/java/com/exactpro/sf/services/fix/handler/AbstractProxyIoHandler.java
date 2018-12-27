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
package com.exactpro.sf.services.fix.handler;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import com.exactpro.sf.configuration.FieldName;
import com.exactpro.sf.configuration.FieldPosition;
import com.exactpro.sf.configuration.RuleDescription;
import com.exactpro.sf.services.tcpip.IProxyIoHandler;
import com.exactpro.sf.services.tcpip.TCPIPProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import quickfix.*;

/**
 * Base class of {@link org.apache.mina.core.service.IoHandler} classes which handle
 * proxied connections.
 *
 */
public abstract class AbstractProxyIoHandler extends IoHandlerAdapter implements IProxyIoHandler {

	private static Logger logger = LoggerFactory.getLogger(AbstractProxyIoHandler.class);
	private static final Charset CHARSET = Charset.forName("iso8859-1");
	public static final String OTHER_IO_SESSION = AbstractProxyIoHandler.class.getName()+".OtherIoSession";

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		logger.debug("sessionCreated: {}", getClass().getSimpleName());
		session.suspendRead();
		session.suspendWrite();
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		logger.debug("sessionClosed: {}", getClass().getSimpleName());
		if (session.getAttribute( OTHER_IO_SESSION ) != null) {
			IoSession sess = (IoSession) session.getAttribute(OTHER_IO_SESSION);
			sess.setAttribute(OTHER_IO_SESSION, null);
			sess.close(false);
			session.setAttribute(OTHER_IO_SESSION, null);
		}
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception
	{
		logger.debug("messageReceived: {} - {}", getClass().getSimpleName(), message);
		byte[] bytes = ((String)message).getBytes();
		IoBuffer wb = IoBuffer.allocate(bytes.length);
		wb.put(bytes);
		wb.flip();
		((IoSession) session.getAttribute(OTHER_IO_SESSION)).write(wb);
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception
	{
		logger.debug("messageSent: {}", getClass().getSimpleName());
		IoBuffer rb = (IoBuffer) message;
		String msg = rb.getString(CHARSET.newDecoder());
		logger.debug("Message content: {}", msg);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
	{
		logger.error(cause.getMessage(), cause);
	}

	// FIXME: MessageConverter + RulesProcessor??
	protected boolean isNotSendAndRulesProcess(Message mess, String msgType, TCPIPProxy proxyService) throws Exception {
		boolean notSend = false;
		if (proxyService.getSettings().isChangeTags())
		{
			// make settings in service for this feature
			for (RuleDescription rule : proxyService.getRules().getRuleDescription()) {
				logger.debug("Start process for rule with message type: {}", rule.getMsgType());

				if (rule.getMsgType().equals(msgType))
				{
					boolean equals = true;
					for (FieldPosition field : rule.getWhen().getField()) {
						logger.debug("Start process for when rule with field: {}", field.getName());
						int fieldId = getFieldId(field.getName());

						FieldMap map = getField(mess, field.getName());

						if (map == mess) {

							try {

								if (!field.getValue().equals(mess.getString(fieldId)))
								{
									equals = false;
								}

							} catch (FieldNotFound e) {

								if (!field.getValue().equals(mess.getHeader().getString(fieldId)))
								{
									equals = false;
								}

							}

						} else {

							if (!field.getValue().equals(map.getString(fieldId)))
							{
								equals = false;
							}

						}

					}
					logger.debug("rule equals value == {}", equals);

					if (equals)
					{
						if (null != rule.getNotSend()) {
							notSend = true;
						} else {

							if (null != rule.getChange()) {
								for (FieldPosition field : rule.getChange().getField())
								{
									int fieldId = getFieldId(field.getName());

									FieldMap map = getField(mess, field.getName());

									if (map == mess) {

										try {

											if (null != mess.getString(fieldId)) {
												mess.setField(new quickfix.StringField(fieldId, field.getValue()));
											}

										} catch (FieldNotFound e) {

											mess.getHeader().setField(new quickfix.StringField(fieldId, field.getValue()));

										}

									} else {

										map.setField(new quickfix.StringField(fieldId, field.getValue()));

									}

								}
							}

							if (null != rule.getRemove()) {
								for (FieldName field : rule.getRemove().getField())
								{
									int fieldId = getFieldId(field.getName());

									FieldMap map = getField(mess, field.getName());

									if (map == mess) {

										try {

											if (null != mess.getString(fieldId)) {
												mess.removeField(fieldId);
											}

										} catch (FieldNotFound e) {

											mess.getHeader().removeField(fieldId);

										}

									} else {

										map.removeField(fieldId);

									}

								}
							}

						}
					}
				}
			}
		}
		return notSend;
	}

	private int getFieldId(String field) {
		return Integer.parseInt(field.substring(field.lastIndexOf(".")+1));
	}

	private FieldMap getField(FieldMap fmap, String name) throws NumberFormatException, FieldNotFound {

		if (name.indexOf(".") > 0) {

			String rg = name.substring(0, name.indexOf("."));

			int bracePos = rg.indexOf("[");

			FieldMap fm = null;

			if (bracePos > 0) {

				int groupId = Integer.parseInt(name.substring(0, bracePos));
				String exp = name.substring(bracePos+1, name.indexOf("]"));
				int fieldId = Integer.parseInt(exp.substring(0, exp.indexOf("=")));
				String fieldValue = exp.substring(exp.indexOf("=")+1, exp.length());

				List<Group> groups = fmap.getGroups(groupId);

				for (Group group : groups) {
					Iterator<Field<?>> iter = group.iterator();
					while (iter.hasNext()) {
						Field<?> field = iter.next();
						if (fieldId == field.getTag() && fieldValue.equals(field.getObject())) {
							fm = group;
							break;
						}
					}
				}

			} else {

				int groupId = Integer.parseInt(rg);
				List<Group> groups = fmap.getGroups(groupId);

				fm = groups.get(0);

			}

			if (null != fm) {
				return getField(fm, name.substring(name.indexOf(".")+1, name.length()));
			}

			return null;

		} else {
			return fmap;
		}
	}
}