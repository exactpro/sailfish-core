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
package com.exactpro.sf.actions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.factory.FixMessageFactory;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl.ActionContextWrapper;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.fix.FIXClient;
import com.exactpro.sf.services.fix.FIXClientSettings;
import com.exactpro.sf.services.fix.FIXSession;
import com.exactpro.sf.services.fix.FixUtil;
import com.exactpro.sf.services.fix.converter.QFJIMessageConverter;
import com.exactpro.sf.services.fix.converter.dirty.FieldConst;
import com.exactpro.sf.util.DateTimeUtility;

import quickfix.FixVersions;

/**
 * Collection of matrix utilities for FIX protocol.
 * @author dmitry.guriev
 *
 */
@MatrixUtils
@ResourceAliases({"FIXMatrixUtil"})
public class FIXMatrixUtil extends AbstractCaller {

	private static Logger logger = LoggerFactory.getLogger(FIXMatrixUtil.class);

    private static final String FIX_DATE_FORMAT = "yyyyMMdd";
    private static final String FIX_DATE_TIME_FORMAT = FIX_DATE_FORMAT + "-HH:mm:ss";
    private static final String FIX_DATE_TIME_FORMAT_MS = FIX_DATE_TIME_FORMAT + ".SSS";
    private static final String FIX_DATE_TIME_FORMAT_NS = FIX_DATE_TIME_FORMAT + ".nnnnnnnnn";
	private static final String FIX_GROUP_PREFIX = "group_";

    private static final DateTimeFormatter FIX_DATE_FORMATTER = DateTimeUtility.createFormatter(FIX_DATE_FORMAT);
    private static final DateTimeFormatter FIX_DATE_TIME_FORMATTER = DateTimeUtility.createFormatter(FIX_DATE_TIME_FORMAT);
    private static final DateTimeFormatter FIX_DATE_TIME_FORMATTER_MS = DateTimeUtility.createFormatter(FIX_DATE_TIME_FORMAT_MS);
    private static final DateTimeFormatter FIX_DATE_TIME_FORMATTER_NS = DateTimeUtility.createFormatter(FIX_DATE_TIME_FORMAT_NS);

    @Description("Transact Time - return time in GMT time zone<br>"
            + DateUtil.MODIFY_HELP
			+ "Example:<br>"
			+ "#{TransactTime(\"Y+2:m-6:D=4:h+1:M-2:s=39\")}<br>"
			+ "will return:<br>"
			+ "<br>"
            + "LocalDateTime (UTC) object with date: 20110904-08:58:39<br>"
	)
	@UtilityMethod
    public LocalDateTime TransactTime(String dateFormat)
	{
        return DateUtil.modifyLocalDateTime(dateFormat);
	}

	@Description("ExpireTime function accept three parameters: Time Shifting Patterns related current time, Date and Time Format Patterns and Date and TimeZone."
			+ DateUtil.FORMAT_HELP
			+ DateUtil.MODIFY_HELP
			+ "<h4>Example:</h4>"
            + "#{ExpireTime(\"Y+2:M-6:D=4:h+1:m-2:s=39\", \"yyyyMMdd-HH:mm:ss.SSS\", \"UTC\")}<br>"
			+ "<br>"
			+ "will return:<br>"
			+ "String: 20110904-08:58:39.123<br>"
			)
	@UtilityMethod
	public final String ExpireTime(String dateFormat, String format, String timeZone)
	{
        return DateUtil.formatDateTime(dateFormat, format, timeZone);
	}

	@Description(" Expire Time - return time in GMT time zone<br>"
			+ DateUtil.MODIFY_HELP
			+ "Example:<br>"
			+ "<br>"
            + "#{ExpireTime(\"Y+2:M-6:D=4:h+1:m-2:s=39\", <DateFormat>)}<br>"
			+ "DateFormat - string parameter, and can be for example: HH:mm:ss-yyyyMMdd"
			+ "default format : yyyyMMdd-HH:mm:ss"
			+ "will return:<br>"
			+ "<br>"
			+ "String: 20110904-08:58:39<br>"
	)
	@UtilityMethod
	public final String ExpireTime(String dateFormat)
	{
        return DateUtil.modifyZonedDateTime(dateFormat).format(FIX_DATE_TIME_FORMATTER);
	}

    @Description(" Expire Time - return time in GMT time zone in format " + FIX_DATE_TIME_FORMAT + "<br>"
			+ " Example:<br>"
			+ " For the <b>10 Nov 2015, 13:00:00</b>"
			+ " where input date parameter <b>#{getDate(\"h=16:m=23:s=0\")}</b>"
			+ " this method will return the following:<br>"
			+ " 20151110-16:23:00<br>"
	)
	@UtilityMethod
    public final String ExpireTime(LocalDateTime date)
	{
        return DateTimeUtility.nowZonedDateTime().format(FIX_DATE_TIME_FORMATTER);
	}

	@Description("Calculates checksum of FIX message. "
	        +"Message contains | delimiter.<br>"
	        +"Example: <br> "
	        +"For this message '8=FIX.4.4|9=122|35=D|34=215|49=CLIENT12|52=20100225-19:41:57.316|56=B|1=Marcel|11=13346|21=1|40=2|44=5|54=1|59=0|60=20100225-19:39:52.020|' <br> "
	        +"checksum is 072"
	)
    @UtilityMethod
    public String calculateChecksum(String msg){
        char[] chars = msg.replace('|', '\001').toCharArray();
        int checksum = 0;
        for (char c:chars){
            checksum+=c;
        }

        DecimalFormat formatter = new DecimalFormat("000");

        return formatter.format(checksum & 0xFF);
    }

	@Description("Calculates checksum of FIX message. "
            +"Message contains delimiter specified in delimiter arg.<br>"
            +"Example: <br> "
            +"For this message '8=FIX.4.4|9=122|35=D|34=215|49=CLIENT12|52=20100225-19:41:57.316|56=B|1=Marcel|11=13346|21=1|40=2|44=5|54=1|59=0|60=20100225-19:39:52.020|' <br> "
            +"exec calculateChecksum(msg,'|') return checksum is 072"
    )
    @UtilityMethod
    public String calculateChecksum(String msg, char delimiter) {
        char[] chars = msg.replace(delimiter, '\001').toCharArray();
        int checksum = 0;
        for (char c:chars){
            checksum+=c;
        }

        DecimalFormat formatter = new DecimalFormat("000");

        return formatter.format(checksum & 0xFF);
    }

	@Description(" Expire Time - return time in GMT time zone<br>"
			+ DateUtil.MODIFY_HELP
			+ "Example:<br>"
            + "#{ExpireTime(\"Y+2:M-6:D=4:h+1:m-2:s=39\")}<br>"
			+ "default format : yyyyMMdd-HH:mm:ss.SSS"
			+ "will return:<br>"
			+ "String: 20110904-08:58:39.123<br>"
	)
	@UtilityMethod
	public final String ExpireTimeMS(String dateFormat)
	{
        return DateUtil.modifyZonedDateTime(dateFormat).format(FIX_DATE_TIME_FORMATTER_MS);
	}

	@Description(" Expire Time - return time in GMT time zone<br>"
            + DateUtil.MODIFY_HELP
            + "#{ExpireTime(\"Y+2:M-6:D=4:h+1:m-2:s=39:mc=123456\")}<br>"
            + "default format : yyyyMMdd-HH:mm:ss.000000"
            + "will return:<br>"
            + "String: 20110904-08:58:39.123456<br>")
    @UtilityMethod
    public final String ExpireTimeMC(String dateFormat) {
        String result = DateUtil.modifyZonedDateTime(dateFormat).format(FIX_DATE_TIME_FORMATTER_NS);
        return result.substring(0, FIX_DATE_TIME_FORMAT_NS.length() - 3);
    }

    @Description(" Expire Time - return time in GMT time zone<br>"
            + DateUtil.MODIFY_HELP
            + "Example:<br>"
            + "#{ExpireTime(\"Y+2:M-6:D=4:h+1:m-2:s=39:ns=123456789\")}<br>"
            + "default format : yyyyMMdd-HH:mm:ss.000000000"
            + "will return:<br>"
            + "String: 20110904-08:58:39.123456789<br>"
    )
    @UtilityMethod
    public final String ExpireTimeNS(String dateFormat) {
        String result = DateUtil.modifyZonedDateTime(dateFormat).format(FIX_DATE_TIME_FORMATTER_NS);
        return result.substring(0, FIX_DATE_TIME_FORMAT_NS.length() - 3);
    }

	@Description("Expire Date - return time in GMT time zone<br>"
			+ " Format date to FIX date format."
			+ DateUtil.MODIFY_HELP
			+ "Example:<br>"
            + "#{ExpireDate(\"Y+2:M-6:D=4\")}<br>"
			+ "will return:<br>"
			+ "20110904<br>"
	)
	@UtilityMethod
    public final String ExpireDate(String dateFormat) {
        return DateUtil.modifyZonedDateTime(dateFormat).format(FIX_DATE_FORMATTER);
	}


	@Description(" Expire Time - return time in UTC time zone<br>"
			+ DateUtil.MODIFY_HELP
			+ "Example:<br>"
			+ "<br>"
            + "#{ExpireTime(\"Y+2:M-6:D=4:h+1:m-2:s=39\", <DateFormat>)}<br>"
			+ "DateFormat - string parameter, and can be for example: HH:mm:ss-yyyyMMdd"
			+ "default format : yyyyMMdd-HH:mm:ss"
			+ "will return:<br>"
			+ "<br>"
			+ "String: 20110904-08:58:39<br>"
	)
	@UtilityMethod
	public final String ExpireTimeUTC(String dateFormat)
	{
        return ExpireTime(dateFormat);
	}

	@Description(" Expire Time - return time in GMT time zone<br>"
			+ DateUtil.MODIFY_HELP
			+ "Example:<br>"
            + "#{ExpireTime(\"Y+2:M-6:D=4:h+1:m-2:s=39:ms=123\")}<br>"
			+ "default format : yyyyMMdd-HH:mm:ss.SSS"
			+ "will return:<br>"
			+ "String: 20110904-08:58:39.123<br>"
	)
	@UtilityMethod
	public final String ExpireTimeMSUTC(String dateFormat)
	{
        return ExpireTimeMS(dateFormat); // BaseExpireTime(dateFormat, FIX_DATE_TIME_FORMAT_MS, DateUtil.GMT_TIME_ZONE.getID(), DateUtil.UTC_TIME_ZONE.getID());
	}

	@Description("Expire Date - return time in GMT time zone<br>"
			+ " Format date to FIX date format."
			+ DateUtil.MODIFY_HELP
			+ "Example:<br>"
            + "#{ExpireDate(\"Y+2:M-6:D=4\")}<br>"
			+ "will return:<br>"
			+ "20110904<br>"
	)
	@UtilityMethod
	public final String ExpireDateUTC (String dateFormat)
	{
        return ExpireDate(dateFormat);
	}

	/**
	 * Generate unique ClOrID for new order.
	 * @return
	 */
	@Description(
			"Generate unique ClOrID for new order.<br>"
			+ "<br>"
			+ "Example:<br>"
			+ "first call will take current time in millis and adds 1, so for "
			+ " current time 1276883000000<br>"
			+ "#{ClOrdID()}<br>"
			+ "will return:<br>"
			+ "1276883000001<br>"
			+" next call will return:<br>"
			+ "1276883000002<br>"
	)
	@UtilityMethod
	public final String ClOrdID()
	{
		return FixUtil.generateClorID();
	}

	/**
	 * Generate unique ClOrID for new order.
	 * @param length
	 * @return
	 */
	@Description(
			"Generate unique ClOrID for new order.<br>"
	)
	@UtilityMethod
	public final String ClOrdID(int length)
	{
		return FixUtil.generateClorIDSpecLng(length);
	}

	@Description("Mark field as excluded.<br>"
	        + "This method excludes auto generated field from message when use in sendDirty action.")
	@UtilityMethod
	public Object ExcludeField() {
        return FieldConst.EXCLUDED_FIELD;
    }

	@Description("Convert Side field from ITCH to FIX format")
	@UtilityMethod
	public char ITCHSideToFIXSide(Short side)
	{
		char fixSide;
		if(side == 66) {
			fixSide = '1';
		} else if(side == 83) {
			fixSide = '2';
		} else {
			fixSide = '3';
		}
		return fixSide;
	}

	@Description("Wraps SeqNum to prepare it for writing to a file")
	@UtilityMethod
	public String WrapSeqNum(Integer seqNum)
	{
		return "\u0000\u0001" + seqNum;
	}

	@Description("Unwraps SeqNum read from a file")
	@UtilityMethod
	public Integer UnwrapSeqNum(String seqNum)
	{
		return Integer.valueOf(seqNum.substring(2));
	}

    @UtilityMethod
    @Description("Return the MsgSeqNum from the Text field<br>" + "<b>Usage:</b> #{extractMsgSeqNum(\"text\")}<br>")
    public int extractMsgSeqNum(String text) {
	    try {
            return extractSeqNum(text);
        } catch (NumberFormatException e) {
	        throw new EPSCommonException("Expecting MsgSeqNum in [" + text + "] is missing or not a valid Integer value", e);
        }
    }

    public static int extractSeqNum(String text) {
        String value = StringUtils.substringBetween(text, "expecting ", " but received");
        value = value == null ? StringUtils.substringBetween(text, "Expected: ", ">") : value;
        return Integer.parseInt(value);
    }

	protected static Map<String, List<MetaContainer>> convert(Map<String, List<MetaContainer>> children, IMessage message) {
		if (children != null && message != null) {
			Map<String, List<MetaContainer>> result = new HashMap<>();

			Set<String> findedChildren = new HashSet<>();
			Object fieldValue = null;
			List<?> listValue = null;
			MetaContainer child = null;

			for (Entry<String, List<MetaContainer>> entry : children.entrySet()) {
				String extractedFieldName = entry.getKey().replaceAll(FIX_GROUP_PREFIX, "");

				if (message.getFieldNames().contains(extractedFieldName)) {
					findedChildren.add(entry.getKey());

					List<MetaContainer> listResult = new ArrayList<>();
					result.put(extractedFieldName, listResult);

					fieldValue = message.getField(extractedFieldName);
					if (fieldValue instanceof List && entry.getValue().size() <= ((List<?>) fieldValue).size()) {
						listValue = (List<?>) fieldValue;
						for (int i = 0; i < entry.getValue().size(); i++) {
							if (listValue.get(i) instanceof IMessage) {
								child = entry.getValue().get(i).clone();
								listResult.add(child);

								Map<String, List<MetaContainer>> childResult = convert(
										new HashMap<>(entry.getValue().get(i).getChildren()),
										(IMessage) listValue.get(i));
								if (childResult != null) {
									for (Entry<String, List<MetaContainer>> childEntry : childResult.entrySet()) {
										for (int j = 0; j < childEntry.getValue().size(); j++) {
											child.add(childEntry.getKey(), childEntry.getValue().get(j));
										}
									}
								}

							} else {
								throw new RuntimeException(
										"Incorrect type of group element. Column: " + extractedFieldName);
							}
						}
					} else {
						throw new RuntimeException("Incorrect type of group or size. Column: " + extractedFieldName);
					}
				}
			}

			if (!findedChildren.isEmpty()) {
				for (String fieldName : findedChildren) {
					children.remove(fieldName);
				}
				findedChildren.clear();
			}

			if (!children.isEmpty()) {

				int numChildren = children.size();

				for (String fieldName : message.getFieldNames()) {// Component
																	// has been
																	// added
																	// after
																	// conversions
					fieldValue = message.getField(fieldName);
					if (fieldValue instanceof IMessage) {
						Map<String, List<MetaContainer>> childResult = convert(children, (IMessage) fieldValue);
						if (childResult != null) {
							child = new MetaContainer();

							List<MetaContainer> listResult = new ArrayList<>();
							result.put(fieldName, listResult);
							listResult.add(child);

							for (Entry<String, List<MetaContainer>> childEntry : childResult.entrySet()) {
								for (int j = 0; j < childEntry.getValue().size(); j++) {
									child.add(childEntry.getKey(), childEntry.getValue().get(j));
								}
							}
						}

					}
				}

				if (children.size() != numChildren && !children.isEmpty()) {
					throw new RuntimeException("Groups " + children.keySet() + " are not presented");
				}
			}

			if (!result.isEmpty()) {
				return result;
			}
		}

		return null;
	}

	protected static MetaContainer convert(MetaContainer sourceNode, IMessage message) {
		if (sourceNode != null && message != null) {
			Map<String, List<MetaContainer>> children = sourceNode.getChildren();
			if (children.isEmpty()) {
				return sourceNode.clone();
			} else {
				Map<String, List<MetaContainer>> childResult = convert(new HashMap<>(children), message);

				if (childResult != null) {
					MetaContainer result = sourceNode.clone();
					for (Entry<String, List<MetaContainer>> childEntry : childResult.entrySet()) {
						for (int i = 0; i < childEntry.getValue().size(); i++) {
							result.add(childEntry.getKey(), childEntry.getValue().get(i));
						}
					}
					return result;
				}
			}
		}
		throw new EPSCommonException("No one groups are presented in converted message");
	}

	/*
	 * Common send method
	 */
	static quickfix.Message send(IActionContext actionContext,
			quickfix.Message message) throws Exception {
		String serviceName = actionContext.getServiceName();
		IInitiatorService service = FIXMatrixUtil.getClient(actionContext);

		boolean performance = false;
		if (service instanceof FIXClient) {
			performance = ((FIXClient)service).isPerformance();
		}

        Logger logger = actionContext.getLogger();

        if(!performance && logger.isInfoEnabled()) {
            logger.info("[{}] {}", serviceName, FixUtil.toString(message, FixUtil.getDictionary(message)));
        }

		if (message.getHeader().isSetField(quickfix.field.MsgSeqNum.FIELD)) {
			int seqnum = message.getHeader().getInt(
					quickfix.field.MsgSeqNum.FIELD);
			((FIXSession) (service.getSession())).addExpectedSenderNum(seqnum);
		}

		Thread.sleep(actionContext.getTimeout());

		service.getSession().send(message);

        if(!performance && logger.isInfoEnabled()) {
            actionContext.getLogger().info("[{}] finished successfully\n", serviceName);
        }

		return message;
	}

	static quickfix.Message receive(IActionContext actionContext, quickfix.Message messageFilter) throws Exception
	{
		boolean isApp = messageFilter.isApp();
		FIXClient service = FIXMatrixUtil.getClient(actionContext);
		IServiceSettings serviceSettings = service.getSettings();
		if(serviceSettings instanceof FIXClientSettings) {
			FIXClientSettings fixClientSettings = (FIXClientSettings) serviceSettings;
			boolean isFIXT = FixVersions.BEGINSTRING_FIXT11.equals(fixClientSettings.getBeginString());
			QFJIMessageConverter converter = service.getConverter();
			IMessage newMessageFilter = converter.convert(messageFilter, null, Boolean.TRUE);//convert to AML3 format
			ActionContextWrapper actionContextWrapper = new ActionContextWrapper(actionContext);
			actionContextWrapper.setMetaContainer(convert(actionContext.getMetaContainer(), newMessageFilter));
			actionContextWrapper.setUncheckedFields(FixMessageFactory.UNCHECKED_FIELDS);
            logger.debug("Meta container has been converted to [{}]", actionContextWrapper.getMetaContainer());

            IMessage iMessage = WaitAction.waitForMessage(actionContextWrapper, newMessageFilter, isApp);

			return converter.convert(iMessage, isFIXT, messageFilter.getClass());//convert AML3 to AML2 format
		} else{
			throw new IllegalStateException("This method must use Fix only");
		}
	}

	static void countMessages(IActionContext actionContext, quickfix.Message message) throws Exception {
		boolean isApp = message.isApp();
		FIXClient service = FIXMatrixUtil.getClient(actionContext);
		QFJIMessageConverter converter = service.getConverter();
		IMessage newMessageFilter = converter.convert(message, null, Boolean.TRUE);// convert to AML3 format
		ActionContextWrapper actionContextWrapper = new ActionContextWrapper(actionContext);
		actionContextWrapper.setMetaContainer(convert(actionContext.getMetaContainer(), newMessageFilter));
		actionContextWrapper.setUncheckedFields(FixMessageFactory.UNCHECKED_FIELDS);
		logger.debug("Meta container has been converted to [{}]", actionContextWrapper.getMetaContainer());

		WaitAction.countMessages(actionContextWrapper, newMessageFilter, isApp);
	}

	static FIXClient getClient(IActionContext actionContext) {
		return ActionUtil.getService(actionContext, FIXClient.class);
	}
}
