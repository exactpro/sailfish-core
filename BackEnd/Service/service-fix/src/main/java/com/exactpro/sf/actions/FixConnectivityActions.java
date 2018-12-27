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

import static com.exactpro.sf.services.fix.FixUtil.convertToNumber;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IGroupReport;
import com.exactpro.sf.services.IAcceptorService;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.fix.FixMessageHelper;
import com.exactpro.sf.services.tcpip.TCPIPClient;
import com.exactpro.sf.services.tcpip.TCPIPMessageHelper;

@MatrixActions
@ResourceAliases({"FixConnectivityActions"})
public class FixConnectivityActions extends AbstractCaller
{
	private static Logger logger = LoggerFactory.getLogger(FixConnectivityActions.class);

	private static final Character SOH = '\001';
	private static final Character SEP = '|';

    //trailer fields
    private static final String CHECK_SUM_FIELD = "CheckSum";

    private static final String IS_RECEIVE_FIELD = "IsReceive";
    private static final String YES = "y";
    private static final String DIRTY_PREFIX = "Dirty";

    private static final List<String> HEADER_FIELDS = Arrays.asList(
            FixMessageHelper.BEGIN_STRING_FIELD,
            FixMessageHelper.BODY_LENGTH_FIELD,
            FixMessageHelper.MSG_TYPE_FIELD,
            FixMessageHelper.MSG_SEQ_NUM_FIELD,
            FixMessageHelper.SENDER_COMP_ID_FIELD,
            FixMessageHelper.TARGET_COMP_ID_FIELD,
            FixMessageHelper.POSS_DUP_FLAG_FIELD,
            FixMessageHelper.POSS_RESEND_FIELD,
            FixMessageHelper.SENDING_TIME_FIELD,
            FixMessageHelper.ORIG_SENDING_TIME_FIELD,
            FixMessageHelper.APPL_VER_ID_FIELD,
            FixMessageHelper.ON_BEHALF_OF_COMP_ID_FIELD,
            FixMessageHelper.DELIVER_TO_COMP_ID_FIELD);

    private static final List<String> TRAILER_FIELDS = Arrays.asList(CHECK_SUM_FIELD);

    private static final String FIX_MESSAGE_DESCRIPTION =
            "If message doesn't contain header or trailer then a predefined one (by SetHeader/SetTrailer actions) will be added.<br>" +
            "If no header or trailer was defined then it will add an empty one.<br>" +
            "If message doesn't contain MsgSeqNum or DirtyMsgSeqNum then a calculated one will be added.<br>" +
            "If Dirty* fields are specified then they will replace values of according non-dirty fields in header/trailer.<br>";

    private static final String IS_RECEIVE_DESCRIPTION = "If " + IS_RECEIVE_FIELD + " field is set to 'y' or 'Y' then action will work with data for receive actions.<br>";

    private final Map<String, Map<String, Object>> sendHeaders = new HashMap<>();
    private final Map<String, Map<String, Object>> receiveHeaders = new HashMap<>();

    private final Map<String, Map<String, Object>> sendTrailers = new HashMap<>();
    private final Map<String, Map<String, Object>> receiveTrailers = new HashMap<>();

    private final Map<String, Integer> sendSeqNums = new HashMap<>();
    private final Map<String, Integer> receiveSeqNums = new HashMap<>();

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS");

	private ConnectivityActions connectivityActions;

    public FixConnectivityActions() {
        connectivityActions = new ConnectivityActions();
    }

	@Deprecated
	// method for fill and send messages from matrix hashmap
	@ActionMethod
    public HashMap<?,?> SendSomeCountOrders(IActionContext actionContext, HashMap<?,?> inputData) throws Exception
	{
		String serviceName = actionContext.getServiceName();
		actionContext.getLogger().info("[{}] started", serviceName);
		actionContext.getLogger().info("actionContext=[{}]", actionContext);

		int repeatCount = Integer.parseInt(inputData.get( "RepeatCount" ).toString());
		int startMSN = Integer.parseInt(inputData.get( "StartSeqNum" ).toString());
		String nameOrder = inputData.get( "OrderNameStart" ).toString();
		String instrum = inputData.get( "OrderInstrument" ).toString();

		TCPIPClient tcpipClient = getClient(actionContext);

        if(!tcpipClient.isConnected()) {
            tcpipClient.connect();
        }

		for (int i=0;i<repeatCount;i++)
		{
			int tmp = startMSN + i;
			String zOrder = "8=FIXT.1.1!9=10!35=D!34="+ tmp +"!49=" + inputData.get("SenderCompID").toString()+  "!52=10!56=TRADX!11=" + nameOrder + tmp + "!38=1000000!40=2!44=1!54=1!55=[N/A]!59=0!60="+dateFormat.format(new Date())+"!9303=M!48=" + instrum +"!22=101!423=6!";


			FIXPacket fpacket = new FIXPacket( "","" );
			fpacket.fillPacketFromString2( zOrder );

			String messageString = (new  String( fpacket.getInDirtyBytes(null, null), 0, fpacket.RealLen, DirtyFixUtil.charset));
			IMessage message = DefaultMessageFactory.getFactory().createMessage(TCPIPMessageHelper.OUTGOING_MESSAGE_NAME_AND_NAMESPACE, TCPIPMessageHelper.OUTGOING_MESSAGE_NAME_AND_NAMESPACE);

			message.addField("RawMessage", messageString);
			message.getMetaData().setRawMessage(messageString.getBytes());

			tcpipClient.sendMessage( message, 3000 );
			try
			{
				Thread.sleep(10);
			} catch (Exception e )
			{
				throw e;
			}
		}
		return inputData;
	}

	@SuppressWarnings("unchecked")
    @Description("Sends dirty FIX message.<br>" + FIX_MESSAGE_DESCRIPTION)
	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
    @CustomColumns({
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.BEGIN_STRING_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.BODY_LENGTH_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.MSG_TYPE_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.MSG_SEQ_NUM_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.SENDER_COMP_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.TARGET_COMP_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.POSS_DUP_FLAG_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.POSS_RESEND_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.SENDING_TIME_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.ORIG_SENDING_TIME_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.APPL_VER_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.ON_BEHALF_OF_COMP_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.DELIVER_TO_COMP_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + CHECK_SUM_FIELD)
    })
    @ActionMethod
    public HashMap<?, ?> SendFixMessage(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {
        applyHeaderAndTrailer((Map<String, Object>)inputData, sendHeaders, sendTrailers, sendSeqNums, actionContext.getServiceName());
	    return connectivityActions.SendMessage(actionContext, inputData);
	}

    @CommonColumns({
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
    @CustomColumns({
        @CustomColumn("RawMessage")
    })
	@ActionMethod
    @SuppressWarnings("deprecation")
    public HashMap<?,?> SendRawMessage(IActionContext actionContext, HashMap<?,?> inputData) throws Exception
	{
		String serviceName = actionContext.getServiceName();
		actionContext.getLogger().info("[{}] started", serviceName);
		actionContext.getLogger().info("actionContext=[{}]", actionContext);

		TCPIPClient tcpipClient = getClient(actionContext);

        if(!tcpipClient.isConnected()) {
            tcpipClient.connect();
        }

		if (!inputData.containsKey("RawMessage"))
			throw new Exception("RawMessage column hasn't been specified in your matrix");

		String messageString = inputData.get("RawMessage").toString();

		logger.debug("RawMessage to be sent: {}", messageString);

		messageString = messageString.replace(SEP, SOH);
		IMessage message = MessageUtil.convertToIMessage(inputData, DefaultMessageFactory.getFactory(), TCPIPMessageHelper.OUTGOING_MESSAGE_NAME_AND_NAMESPACE, TCPIPMessageHelper.OUTGOING_MESSAGE_NAME_AND_NAMESPACE);

		message.getMetaData().setRawMessage(messageString.getBytes());
		tcpipClient.sendMessage(message, 3000);

		return inputData;
	}

	@SuppressWarnings("unchecked")
    @Description("Waits for a dirty FIX message.<br>" + FIX_MESSAGE_DESCRIPTION)
    @CommonColumns({
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @CustomColumns({
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.BEGIN_STRING_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.BODY_LENGTH_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.MSG_TYPE_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.MSG_SEQ_NUM_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.SENDER_COMP_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.TARGET_COMP_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.POSS_DUP_FLAG_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.POSS_RESEND_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.SENDING_TIME_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.ORIG_SENDING_TIME_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.APPL_VER_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.ON_BEHALF_OF_COMP_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + FixMessageHelper.DELIVER_TO_COMP_ID_FIELD),
        @CustomColumn(DIRTY_PREFIX + CHECK_SUM_FIELD)
    })
    @ActionMethod
    public HashMap<?, ?> WaitFixMessage(IActionContext actionContext, HashMap<?, ?> mapFilter) throws Exception {
        applyHeaderAndTrailer((Map<String, Object>)mapFilter, receiveHeaders, receiveTrailers, receiveSeqNums, actionContext.getServiceName());
        return connectivityActions.WaitMessage(actionContext, mapFilter);
	}

	@CommonColumns({
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @CustomColumns({
        @CustomColumn("BeginString"),
        @CustomColumn("MsgType")
    })
	@ActionMethod
    public void CountMessages(IActionContext actionContext, HashMap<?,?> mapFilter) throws Exception
	{
	    IMessage incomingMessage = MessageUtil.convertToIMessage(mapFilter, DefaultMessageFactory.getFactory(), TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE, TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE);
		WaitAction.countMessages(actionContext, incomingMessage, true);
	}

	@CommonColumns({
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @CustomColumns({
        @CustomColumn("MsgType")
    })
	@ActionMethod
    public void CountMessagesWithoutBeginString(IActionContext actionContext, HashMap<?,?> mapFilter) throws Exception
	{
	   CountMessages(actionContext, mapFilter);
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void Connect(IActionContext actionContext) throws Exception {
		String serviceName = actionContext.getServiceName();
		actionContext.getLogger().info("[{}] started", serviceName);
		actionContext.getLogger().info("actionContext=[{}]", actionContext);

		TCPIPClient tcpipClient = getClient(actionContext);

        if(!tcpipClient.isConnected()) {
            tcpipClient.connect();
        }
	}


	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void Disconnect(IActionContext actionContext)
			throws Exception
	{
		String serviceName = actionContext.getServiceName();
		actionContext.getLogger().info("[{}] disconnect.", serviceName);

		TCPIPClient client = getClient(actionContext);
		ISession session = client.getSession();
		if(session != null) {
			session.close();

			long timeout = System.currentTimeMillis() + 1000;
			while (timeout > System.currentTimeMillis() && !session.isClosed()) {
				Thread.sleep(10);
			}
		} else {
			logger.warn("Disconnect finished with error: session is null");
		}
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void CheckConnect (IActionContext actionContext)
	{
		String serviceName = actionContext.getServiceName();
		actionContext.getLogger().info("[{}] disconnect.", serviceName);

		for (ISession session : getAllSessions(getService(actionContext))) {
            if (!session.isClosed()) {
                return;
            }
        }

        throw new EPSCommonException("Service '" + serviceName + "' is not connected.");
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void CheckDisconnect (IActionContext actionContext)
	{
		String serviceName = actionContext.getServiceName();
		actionContext.getLogger().info("[{}] disconnect.", serviceName);

		for (ISession session : getAllSessions(getService(actionContext))) {
            if (false == session.isClosed()) {
                throw new EPSCommonException("Service '" + serviceName + "' is connected.");
            }
        }
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
	@ActionMethod
    public void WaitDisconnect(IActionContext actionContext)
	{
		String serviceName = actionContext.getServiceName();
		Logger logger = actionContext.getLogger();
		logger.info("[{}] waiting for disconnect.", serviceName);

		long sleepTime = actionContext.getTimeout();
		long deadlineTime = System.currentTimeMillis() + sleepTime;

		do
		{   boolean all = true;
		    for (ISession s : getAllSessions(getService(actionContext))) {
                if (!(all &= s.isClosed())) {
                    break;
                }
            }
            if(all) {
                logger.info("[{}] disconnect completed.", serviceName);
                return;
            }
		}
		while(deadlineTime > System.currentTimeMillis());

		throw new EPSCommonException("Service '" + serviceName + "' still connected.");
	}

    @Description("Returns current sequence number for a specified service.<br>" + IS_RECEIVE_DESCRIPTION)
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Reference)
    })
    @CustomColumns({
        @CustomColumn(IS_RECEIVE_FIELD)
    })
    @ActionMethod
    public int GetSeqNum(IActionContext actionContext, HashMap<?, ?> hashMap) {
        boolean isReceive = YES.equalsIgnoreCase(String.valueOf(hashMap.get(IS_RECEIVE_FIELD)));
        Map<String, Integer> seqNums = isReceive ? receiveSeqNums : sendSeqNums;
        Integer value = seqNums.get(actionContext.getServiceName());
        return value == null ? 0 : value;
    }

    @Description("Sets sequence number for a specified service to a value.<br>" + IS_RECEIVE_DESCRIPTION)
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @CustomColumns({
        @CustomColumn(value = FixMessageHelper.MSG_SEQ_NUM_FIELD, required = true),
        @CustomColumn(IS_RECEIVE_FIELD)
    })
    @ActionMethod
    public void SetSeqNum(IActionContext actionContext, HashMap<?, ?> hashMap) {
        boolean isReceive = YES.equalsIgnoreCase(String.valueOf(hashMap.get(IS_RECEIVE_FIELD)));
        Object seqNumObject = hashMap.get(FixMessageHelper.MSG_SEQ_NUM_FIELD);

        int seqNum = convertToNumber(FixMessageHelper.MSG_SEQ_NUM_FIELD, seqNumObject).intValue();

        Map<String, Integer> seqNums = isReceive ? receiveSeqNums : sendSeqNums;
        seqNums.put(actionContext.getServiceName(), seqNum);
    }

    @Description("Adds value to a sequence number of a specified service.<br>" + IS_RECEIVE_DESCRIPTION)
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @CustomColumns({
        @CustomColumn(value = FixMessageHelper.MSG_SEQ_NUM_FIELD, required = true),
        @CustomColumn(IS_RECEIVE_FIELD)
    })
    @ActionMethod
    public void AddSeqNum(IActionContext actionContext, HashMap<?, ?> hashMap) {
        boolean isReceive = YES.equalsIgnoreCase(String.valueOf(hashMap.get(IS_RECEIVE_FIELD)));
        Object seqNumObject = hashMap.get(FixMessageHelper.MSG_SEQ_NUM_FIELD);

        int seqNum = convertToNumber(FixMessageHelper.MSG_SEQ_NUM_FIELD, seqNumObject).intValue();

        Map<String, Integer> seqNums = isReceive ? receiveSeqNums : sendSeqNums;
        seqNums.put(actionContext.getServiceName(), GetSeqNum(actionContext, hashMap) + seqNum);
    }

    @Description("Sets message header for a specified service.<br>" + IS_RECEIVE_DESCRIPTION)
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @CustomColumns({
        @CustomColumn(IS_RECEIVE_FIELD),
        @CustomColumn(FixMessageHelper.BEGIN_STRING_FIELD),
        @CustomColumn(FixMessageHelper.BODY_LENGTH_FIELD),
        @CustomColumn(FixMessageHelper.MSG_TYPE_FIELD),
        @CustomColumn(FixMessageHelper.MSG_SEQ_NUM_FIELD),
        @CustomColumn(FixMessageHelper.SENDER_COMP_ID_FIELD),
        @CustomColumn(FixMessageHelper.TARGET_COMP_ID_FIELD),
        @CustomColumn(FixMessageHelper.POSS_DUP_FLAG_FIELD),
        @CustomColumn(FixMessageHelper.POSS_RESEND_FIELD),
        @CustomColumn(FixMessageHelper.SENDING_TIME_FIELD),
        @CustomColumn(FixMessageHelper.ORIG_SENDING_TIME_FIELD),
        @CustomColumn(FixMessageHelper.APPL_VER_ID_FIELD),
        @CustomColumn(FixMessageHelper.ON_BEHALF_OF_COMP_ID_FIELD),
        @CustomColumn(FixMessageHelper.DELIVER_TO_COMP_ID_FIELD)
    })
    @ActionMethod
    public void SetHeader(IActionContext actionContext, HashMap<?, ?> hashMap) {
        Map<String, Object> header = extractMap(HEADER_FIELDS, hashMap, null);
        boolean isReceive = YES.equalsIgnoreCase(String.valueOf(hashMap.get(IS_RECEIVE_FIELD)));
        Map<String, Map<String, Object>> targetMap = isReceive ? receiveHeaders : sendHeaders;
        targetMap.put(actionContext.getServiceName(), header);
    }

    @Description("Sets message trailer for a specified service.<br>" + IS_RECEIVE_DESCRIPTION)
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @CustomColumns({
        @CustomColumn(IS_RECEIVE_FIELD),
        @CustomColumn(CHECK_SUM_FIELD)
    })
    @ActionMethod
    public void SetTrailer(IActionContext actionContext, HashMap<?, ?> hashMap) {
        Map<String, Object> trailer = extractMap(TRAILER_FIELDS, hashMap, null);
        boolean isReceive = YES.equalsIgnoreCase(String.valueOf(hashMap.get(IS_RECEIVE_FIELD)));
        Map<String, Map<String, Object>> targetMap = isReceive ? receiveTrailers : sendTrailers;
        targetMap.put(actionContext.getServiceName(), trailer);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(String fieldName, Map<String, Object> sourceMap, Map<String, Map<String, Object>> defaultMaps, String serviceName) {
        Object fieldObject = sourceMap.get(fieldName);

        if(fieldObject == null) {
            Map<String, Object> map = defaultMaps.get(serviceName);

            if(map == null) {
                map = new HashMap<>();
            } else {
                map = new HashMap<>(map);
            }

            sourceMap.put(fieldName, fieldObject = map);
        } else if(!(fieldObject instanceof Map<?, ?>)) {
            throw new EPSCommonException(fieldName + " is not a map");
        }

        return (Map<String, Object>)fieldObject;
    }

    private Map<String, Object> extractMap(List<String> fieldNames, Map<?, ?> sourceMap, String prefix) {
        Map<String, Object> resultMap = new HashMap<>();

        for(String fieldName : fieldNames) {
            Object fieldValue = sourceMap.remove(prefix != null ? prefix.concat(fieldName) : fieldName);

            if(fieldValue != null) {
                resultMap.put(fieldName, fieldValue);
            }
        }

        return resultMap;
    }

    private void applyHeaderAndTrailer(Map<String, Object> message, Map<String, Map<String, Object>> headers, Map<String, Map<String, Object>> trailers, Map<String, Integer> seqNums, String serviceName) {
        Map<String, Object> header = getMap(FixMessageHelper.HEADER, message, headers, serviceName);
        Map<String, Object> trailer = getMap(FixMessageHelper.TRAILER, message, trailers, serviceName);

        if(!header.containsKey(FixMessageHelper.MSG_SEQ_NUM_FIELD) && !message.containsKey(DIRTY_PREFIX + FixMessageHelper.MSG_SEQ_NUM_FIELD)) {
            Integer seqNum = ObjectUtils.defaultIfNull(seqNums.get(serviceName), 0);
            header.put(FixMessageHelper.MSG_SEQ_NUM_FIELD, Integer.toString(seqNum));
            seqNums.put(serviceName, ++seqNum);
        }

        header.putAll(extractMap(HEADER_FIELDS, message, DIRTY_PREFIX));
        trailer.putAll(extractMap(TRAILER_FIELDS, message, DIRTY_PREFIX));
    }

	private static TCPIPClient getClient(IActionContext actionContext) {
		return ActionUtil.getService(actionContext, TCPIPClient.class);
	}

	private static IService getService(IActionContext actionContext) {
		return ActionUtil.getService(actionContext, IService.class);
	}

	private static List<ISession> getAllSessions(IService service) {
        List<ISession> tmp = new ArrayList<>();
        if (service instanceof IAcceptorService) {
            tmp.addAll(((IAcceptorService) service).getSessions());
        }
        if (service instanceof IInitiatorService) {
            tmp.add(((IInitiatorService) service).getSession());
        }

        return tmp;
	}

	/* wait for message with tags */
	@ActionMethod
    public quickfix.Message UNI_WaitMessageWithTags(IActionContext actionContext, HashMap<?,?> messageFilter) throws Exception
	{
		ArrayList<String> masWaitingTags = new ArrayList<>();

		Boolean checkedRight = false;

		String groupHeader = null;
		String groupDelimiter = null;
		ArrayList<String> groupTags = new ArrayList<>();

		if ( messageFilter.containsKey( "WaitingTags" ) )
		{
			String[] waitingTags = ((String) messageFilter.get( "WaitingTags" )).split(";");

			for (int i = 0; i < waitingTags.length; i++ )
			{
				masWaitingTags.add( waitingTags[ i ] + "|" );
			}
		}

		if ( messageFilter.containsKey( "WaitingGroup" ) )
		{
			String[] waitingTags = ((String) messageFilter.get( "WaitingGroup" )).split(";");

			groupHeader = waitingTags[ 0 ];
			groupDelimiter = waitingTags[ 1 ];

			for (int i = 2; i < waitingTags.length; i++ )
			{
				groupTags.add( waitingTags[ i ] + "|");
			}
		}

		long waitTime = actionContext.getTimeout();

		long endTime = System.currentTimeMillis() + waitTime;

		int fromId = 0;//session.messagelist.size();
		int index = fromId;
		//String mainHTMLPrint = new String( "<table width='90%' border='0' cellspacing='0' cellpadding='0'" );
		do {
			List<String> list = new ArrayList<>();//session.messagelist;
			if (list != null) {
				for (; index < list.size(); index++)
				{
					Object message;
					message = list.get(index);

					String printString = "<font color='black'><table width='90%' border='0' cellspacing='0' cellpadding='0'";

					String htmlMsg = new String( message.toString().getBytes(), "UTF8" );

					String tmpMsg = new String();
					int countChars = 0;
					//htmlMsg.replaceAll( "" + htmlMsg.charAt(10),"|");

					for (int j = 0; j < htmlMsg.length(); j++)
					{
						countChars++;
						if ( htmlMsg.charAt(j) == 1 )
						{
							tmpMsg += "|";
							if ( countChars > 140 )
							{
								countChars = 0;
								tmpMsg += "<br>";
							}

						} else
							tmpMsg += htmlMsg.charAt(j);
					}

					htmlMsg = tmpMsg;
					IActionReport report = actionContext.getReport();
                    report.createMessage(StatusType.NA, MessageLevel.INFO,
                            "Recieved message" + printString + "<tr><td></td><td>" + tmpMsg + "</td><tr></table></font>");

					// compare message
					// **** WaitingGroupTags ****
					if ( groupTags.size() > 0 )
					{
						// remove part before start of Group
						htmlMsg = htmlMsg.replaceAll( "<br>", "" );

						if (( htmlMsg.indexOf( groupHeader + "=" )) <= 0 ) continue;

						htmlMsg = htmlMsg.substring( htmlMsg.indexOf( groupHeader + "="), htmlMsg.length() );
						htmlMsg = htmlMsg.substring( htmlMsg.indexOf( "|")+1, htmlMsg.length() );

						if ( htmlMsg.indexOf( groupDelimiter ) != 0 )
						{
                            try (IGroupReport groupReport = report.createActionGroup("Wait for message", tmpMsg)) {
                                groupReport.createMessage(StatusType.FAILED, MessageLevel.INFO, "Have not needed group. in:<br> " + tmpMsg);
                                groupReport.createVerification(StatusType.FAILED, "Wait for message", tmpMsg, "");
                            }
							continue;
						}
						htmlMsg = htmlMsg.substring( htmlMsg.indexOf( "=")+1, htmlMsg.length() );

						// fill array with repeating groups
						ArrayList<String> msgGroups = new ArrayList<>();

						String[] groupInTags = htmlMsg.split( groupDelimiter + "=" );

						for (int i = 0; i < groupInTags.length; i++ )
						{
							msgGroups.add( groupDelimiter + "=" + groupInTags[ i ] );
						}

						//find tags in group

						for ( int i = 0; i < msgGroups.size(); i++ )
						{
							int passedCountForGroup = 0;
							for ( int j = 0; j < groupTags.size(); j++ )
							{
								String firstpart = groupTags.get( j ).split("=")[0] + "=";
								int pozition = msgGroups.get( i ).indexOf( firstpart );
								if ( pozition < 0 )
								{ continue; }

								if ( msgGroups.get( i ).contains( groupTags.get( j ) ) )
								{
									passedCountForGroup++;
									String tempStr = groupTags.get( j ).substring(0, groupTags.get( j ).length() - 1 );
									msgGroups.set( i, msgGroups.get( i ).replaceFirst( tempStr, "<font color='green'><strong>" + tempStr + "</strong></font>" ) );
								} else if ( msgGroups.get( i ).contains( firstpart ) )
								{
									String tempStr = msgGroups.get( i ).substring( pozition, msgGroups.get( i ).indexOf( "|", pozition ) );
									msgGroups.set( i, msgGroups.get( i ).replaceFirst( tempStr, "<font color='red'><strong>" + tempStr + "</strong></font>" ) );
								}
							}
							if ( passedCountForGroup == groupTags.size() )
							{
                                try (IGroupReport groupReport = report
                                        .createActionGroup("GROUP: " + passedCountForGroup + " passed from " + groupTags.size(), "#" + i)) {
                                    printString = "<table><tr><td></td><td>group:</td><td>" + msgGroups.get(i).toString() + "</td><tr></table>";
                                    groupReport.createMessage(StatusType.PASSED, MessageLevel.INFO, printString);
                                    groupReport.createVerification(StatusType.PASSED,
                                            "GROUP: " + passedCountForGroup + " passed from " + groupTags.size(), "#" + i, "", null, null);
                                }
								checkedRight = true;
							} else
							{
                                try (IGroupReport groupReport = report
                                        .createActionGroup("GROUP: " + passedCountForGroup + " passed from " + groupTags.size(), "#" + i)) {
                                    printString = "<table><tr><td></td><td>group:</td><td width='80%'>" + msgGroups.get(i).toString()
                                            + "</td><tr></table>";
                                    groupReport.createMessage(StatusType.FAILED, MessageLevel.INFO, printString);
                                    groupReport.createVerification(StatusType.FAILED,
                                            "GROUP: " + passedCountForGroup + " passed from " + groupTags.size(),
                                            "#" + i, "", null, null);
                                }
							}
						}
						if (!checkedRight)
						{
							throw new Exception( "No right message" );
						}
					}
					// **** WaitingTags ****
					if ( masWaitingTags.size() > 0 )
					{
						int passedCount = 0;
						for ( int i = 0; i < masWaitingTags.size(); i++ )
						{
							String firstpart = masWaitingTags.get( i ).split("=")[0] + "=";
							int pozition = htmlMsg.indexOf( firstpart );
							if ( htmlMsg.contains( masWaitingTags.get( i ) ) )
							{
								passedCount++;
								String tempStr = masWaitingTags.get( i ).substring(0, masWaitingTags.get( i ).length() - 1 );
								htmlMsg = htmlMsg.replaceFirst( tempStr, "<font color='green'><strong>" + tempStr + "</strong></font>" );
							} else if ( htmlMsg.contains( firstpart ) )
							{
								String tempStr = htmlMsg.substring( pozition, htmlMsg.indexOf( "|", pozition ) );
								htmlMsg = htmlMsg.replaceFirst( tempStr, "<font color='red'><strong>" + tempStr + "</strong></font>" );
							}
						}
						if ( passedCount == masWaitingTags.size() )
						{
                            try (IGroupReport groupReport = report.createActionGroup("Wait for message", "")) {
                                printString += "<tr><td></td><td>Found in message:</td><td>" + htmlMsg + "</td><tr></table>";
                                groupReport.createMessage(StatusType.PASSED, MessageLevel.INFO, printString);
                                groupReport.createVerification(StatusType.PASSED, "Wait for message", "", "", null, null);
                            }

							return null;
						}
                        try (IGroupReport groupReport = report.createActionGroup("Wait for message", "")) {
                            printString += "<tr><td></td><td>Found in message:</td><td width='80%'>" + htmlMsg + "</td><tr></table>";
                            groupReport.createMessage(StatusType.FAILED, MessageLevel.INFO, printString);
                            report.createVerification(StatusType.FAILED, "Wait for message", "", "", null, null);
                        }

					} else if (checkedRight)
					{
						return null;
					}
				}
			}
			// do not wait if timeout reached or when timeout == 0
			if (endTime <= System.currentTimeMillis()) {
				return null; //PartialList
			}
			Thread.sleep(1);
		} while (endTime > System.currentTimeMillis());

		throw new Exception( "Time is over" );
	}
}
