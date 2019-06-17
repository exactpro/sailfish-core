/******************************************************************************
 * Copyright (c) 2009-2018, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactpro.sf.services.fix;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.util.DateTimeUtility;

import quickfix.ConfigError;
import quickfix.DataDictionaryProvider;
import quickfix.DoNotSend;
import quickfix.FieldConvertError;
import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.Group;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.LogUtil;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.StringField;
import quickfix.UnsupportedMessageType;
import quickfix.field.ApplVerID;
import quickfix.field.ExecID;
import quickfix.field.MsgSeqNum;
import quickfix.field.OrdType;
import quickfix.field.OrderID;

public class FIXTESTApplication implements FIXServerApplication {

    private int m_orderID;
    private int m_execID;
    private static final String ALWAYS_FILL_LIMIT_KEY = "AlwaysFillLimitOrders";
    private static final String DEFAULT_MARKET_PRICE_KEY = "DefaultMarketPrice";
    private static final String VALID_ORDER_TYPES_KEY = "ValidOrderTypes";

    private static final String FIELD_GROUP_DELIMITER = "GroupDelimiter";
    private static final String FIELD_HEADER = "header";
    private static final String FIELD_TRAILER = "trailer";
    private static final String MSG_TYPE = "MsgType";

    private boolean alwaysFillLimitOrders;
    private SessionSettings settings;
    private final HashSet<String> validOrderTypes = new HashSet<>();

    private MessageHelper messageHelper;
    private ILoggingConfigurator logConfigurator;
    private ServiceName serviceName;
    private final Map<SessionID, Pair<Integer, Integer>> seqNums = new HashMap<>();
    private IServiceHandler handler;
    private DirtyQFJIMessageConverter converter;
    private IMessageStorage storage;
    private boolean keepMessagesInMemory;
    private ServiceInfo serviceInfo;
    private FixFieldConverter fixFieldConverter;
    private final Map<SessionID, ISession> sessionMap = new HashMap<>();
    private final ISession fixServerSessionsContainer = new FixServerSessionsContainer(this);
    private DataDictionaryProvider dictionaryProvider;

    private final Random random = new Random();

    private static final Logger logger = LoggerFactory.getLogger(FIXTESTApplication.class);

    private MarketDataProvider marketDataProvider = new MarketDataProvider() {

        @Override
        public double getBid(String symbol) {
            return 12.30;
        }

        @Override
        public double getAsk(String symbol) {
            return 12.30;
        }
    };

    interface MarketDataProvider {

        double getBid(String symbol);

        double getAsk(String symbol);
    }

    @Override
    public void init(IServiceContext serviceContext, ApplicationContext applicationContext, ServiceName serviceName) {
        this.logConfigurator = Objects.requireNonNull(serviceContext.getLoggingConfigurator(), "'Logging configurator' parameter");
        this.serviceName = serviceName;
        this.handler = applicationContext.getServiceHandler();
        this.converter = applicationContext.getConverter();
        this.dictionaryProvider = applicationContext.getDictionaryProvider();
        this.storage = serviceContext.getMessageStorage();
        this.serviceInfo = serviceContext.lookupService(serviceName);

        this.messageHelper = applicationContext.getMessageHelper();
        fixFieldConverter = new FixFieldConverter();
        fixFieldConverter.init(messageHelper.getDictionaryStructure(),
                messageHelper.getNamespace());

        if (applicationContext.getServiceSettings() instanceof FIXServerSettings) {
            this.keepMessagesInMemory = ((FIXServerSettings) applicationContext.getServiceSettings()).getKeepMessagesInMemory();
        }

        this.settings = applicationContext.getSessionSettings();
        try {
            alwaysFillLimitOrders = settings.isSetting(ALWAYS_FILL_LIMIT_KEY)
                    && settings.getBool(ALWAYS_FILL_LIMIT_KEY);
            initializeMarketDataProvider(settings);
            initializeValidOrderTypes(settings);
        } catch (ConfigError e) {
            logger.error(e.getMessage(), e);
        } catch (FieldConvertError e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void fillFixHeader(IMessage msg) {
        IMessage header = (IMessage) msg.getField(FIELD_HEADER);
        if (header == null) {
            header = messageHelper.getMessageFactory().createMessage(FIELD_HEADER,
                    messageHelper.getNamespace());
            msg.addField(FIELD_HEADER, header);
        }
        String messageType = getAttributeValue(messageHelper.getDictionaryStructure()
                .getMessages().get(msg.getName()), MessageHelper.FIELD_MESSAGE_TYPE);
        header.addField(MSG_TYPE, messageType);

        msg.addField(FIELD_TRAILER, messageHelper.getMessageFactory()
                .createMessage(FIELD_TRAILER, messageHelper.getNamespace()));

    }

    public void fillMandatoryFields(IMessage message) {

        fillFixHeader(message);

        IMessageStructure msgStructure = messageHelper.getDictionaryStructure()
                .getMessages().get(message.getName());

        for(IFieldStructure field : msgStructure.getFields().values()) {

            // exclude header and trailer
            if (FIELD_HEADER.equals(field.getName()) || FIELD_TRAILER.equals(field.getName())) {
                continue;
            }

            if (!message.isFieldSet(field.getName()) && field.isRequired()) {
                if (field.isSimple()) {
                    message.addField(field.getName(),
                            getRandomObjectByJavaType(field.getJavaType()));
                } else if (field.isCollection()) {
                    ArrayList<Object> l = new ArrayList<>(1);
                    l.add(getRandomObjectByJavaType(field.getJavaType()));
                    message.addField(field.getName(), l);
                } else if (field.isComplex()) {
                    IMessage nested = messageHelper.getMessageFactory()
                            .createMessage(field.getName(), field.getNamespace());
                    fillMandatoryFields(nested);
                    message.addField(field.getName(), nested);
                } else if (field.isEnum()) {
                    message.addField(field.getName(),
                            field.getValues().values().iterator().next().getCastValue());
                }

            }
        }
    }

    public Object getRandomObjectByJavaType(JavaType javaType) {
        switch (javaType) {
        case JAVA_TIME_LOCAL_TIME:
            return DateTimeUtility.nowLocalTime();
        case JAVA_TIME_LOCAL_DATE:
            return DateTimeUtility.nowLocalDate();
        case JAVA_LANG_BOOLEAN:
            return random.nextBoolean();
        case JAVA_LANG_BYTE:
            return random.nextInt() % 127;
        case JAVA_LANG_CHARACTER:
            return (char) (Math.abs(random.nextInt() % 94) + 32);
        case JAVA_LANG_DOUBLE:
            return random.nextDouble();
        case JAVA_LANG_FLOAT:
            return random.nextFloat();
        case JAVA_LANG_INTEGER:
            return random.nextInt();
        case JAVA_LANG_LONG:
            return random.nextLong();
        case JAVA_LANG_SHORT:
            return random.nextInt() % 32766;
        case JAVA_LANG_STRING:
            return (char) (Math.abs(random.nextInt() % 94) + 32);
        case JAVA_MATH_BIG_DECIMAL:
            return BigDecimal.valueOf(random.nextInt());
        case JAVA_TIME_LOCAL_DATE_TIME:
            return DateTimeUtility.nowLocalDateTime();
        default:
            return new Object();
        }
    }

    public void onMessage(SessionID sessionID, IMessage epMessage)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        try {
            validateOrder(epMessage);

            BigDecimal orderQty = (BigDecimal) epMessage.getField("OrderQty");
            BigDecimal price = getPrice(epMessage);

            IMessage answer = messageHelper.getMessageFactory().createMessage("ExecutionReport",
                    messageHelper.getNamespace());

            answer.addField("OrderID", genOrderID());
            answer.addField("ExecID", genExecID());
            answer.addField("ExecType", '2');
            answer.addField("OrdStatus", '0');
            answer.addField("Side", epMessage.getField("Side"));
            answer.addField("LeavesQty", epMessage.getField("OrderQty"));
            answer.addField("CumQty", BigDecimal.ZERO);
            answer.addField("Symbol", epMessage.getField("Symbol"));
            answer.addField("ClOrdID", epMessage.getField("ClOrdID"));

            fillMandatoryFields(answer);
            Message qfjMessage = null;
            try {
                qfjMessage = convertToFix(answer);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
            sendMessage(sessionID, qfjMessage);

            if (isOrderExecutable(epMessage, price)) {
                answer = messageHelper.getMessageFactory().createMessage("ExecutionReport",
                        messageHelper.getNamespace());
                answer.addField("OrderID", genOrderID());
                answer.addField("ExecID", genExecID());
                answer.addField("ExecType", '2');
                answer.addField("OrdStatus", '2');
                answer.addField("Side", epMessage.getField("Side"));
                answer.addField("CumQty", orderQty);
                answer.addField("LeavesQty", BigDecimal.ZERO);
                answer.addField("ClOrdID", epMessage.getField("ClOrdID"));
                answer.addField("Symbol", epMessage.getField("Symbol"));
                answer.addField("OrderQty", orderQty);
                answer.addField("LastPx", price);
                answer.addField("AvgPx", price);

                fillMandatoryFields(answer);
                try {
                    qfjMessage = convertToFix(answer);
                    System.out.println(qfjMessage);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }

                sendMessage(sessionID, qfjMessage);
            }

        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    private boolean isOrderExecutable(IMessage order, BigDecimal price) throws FieldNotFound {

        if (Character.valueOf('2').equals(order.getField("OrdType"))) {
            BigDecimal limitPrice = new BigDecimal((String) order.getField("Price"));
            char side = (char) order.getField("Side");
            BigDecimal thePrice = new BigDecimal("" + price);

            return (Character.valueOf('1').equals(side) && thePrice.compareTo(limitPrice) <= 0)
                    || ((Character.valueOf('2').equals(side) || Character.valueOf('5').equals(side))
                            && thePrice.compareTo(limitPrice) >= 0);
        }
        return true;
    }

    private void validateOrder(IMessage order) throws IncorrectTagValue, FieldNotFound {
        Character ordType = (Character) order.getField("OrdType");
        if (!validOrderTypes.contains(ordType.toString())) {
            logger.error("Order type not in ValidOrderTypes setting");
            throw new IncorrectTagValue(ordType);
        }
        if (Character.valueOf('1').equals(ordType) && marketDataProvider == null) {
            logger.error("DefaultMarketPrice setting not specified for market order");
            throw new IncorrectTagValue(ordType);
        }
    }

    private BigDecimal getPrice(IMessage message) throws FieldNotFound {
        if (Character.valueOf('2').equals(message.getField("OrdType")) && alwaysFillLimitOrders) {
            return (BigDecimal)message.getField("Price");
        } else {
            if (marketDataProvider == null) {
                throw new RuntimeException("No market data provider specified for market order");
            }
            char side = (char) message.getField("Side");
            if (Character.valueOf('1').equals(message.getField("Side"))) {
                return BigDecimal
                        .valueOf(marketDataProvider.getAsk((String)message.getField("Symbol")));
            } else if (Character.valueOf('2').equals(side) || Character.valueOf('5').equals(side)) {
                return BigDecimal
                        .valueOf(marketDataProvider.getBid((String)message.getField("Symbol")));
            } else {
                throw new RuntimeException("Invalid order side: " + side);
            }
        }
    }

    public OrderID genOrderID() {
        return new OrderID(String.valueOf(++m_orderID));
    }

    public ExecID genExecID() {
        return new ExecID(String.valueOf(++m_execID));
    }

    private void initializeMarketDataProvider(SessionSettings settings)
            throws ConfigError, FieldConvertError {
        if (settings.isSetting(DEFAULT_MARKET_PRICE_KEY)) {
            if (marketDataProvider == null) {
                double defaultMarketPrice = settings.getDouble(DEFAULT_MARKET_PRICE_KEY);
                marketDataProvider = new MarketDataProvider() {
                    @Override
                    public double getAsk(String symbol) {
                        return defaultMarketPrice;
                    }

                    @Override
                    public double getBid(String symbol) {
                        return defaultMarketPrice;
                    }
                };
            } else {
                logger.warn("Ignoring {} since provider is already defined.", DEFAULT_MARKET_PRICE_KEY);
            }
        }
    }

    private void initializeValidOrderTypes(SessionSettings settings)
            throws ConfigError, FieldConvertError {
        if (settings.isSetting(VALID_ORDER_TYPES_KEY)) {
            List<String> orderTypes = Arrays
                    .asList(settings.getString(VALID_ORDER_TYPES_KEY).trim().split("\\s*,\\s*"));
            validOrderTypes.addAll(orderTypes);
        } else {
            validOrderTypes.add(String.valueOf(OrdType.LIMIT));
            validOrderTypes.add(String.valueOf(OrdType.MARKET));
            validOrderTypes.add(String.valueOf(OrdType.FOREX_LIMIT));
        }
    }

    private void sendMessage(SessionID sessionID, Message message) {
        try {
            Session session = Session.lookupSession(sessionID);
            if (session == null) {
                throw new SessionNotFound(sessionID.toString());
            }

            DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
            if (dataDictionaryProvider != null) {
                try {
                    dataDictionaryProvider
                            .getApplicationDataDictionary(getApplVerID(session, message))
                            .validate(message, true);
                } catch (Exception e) {
                    LogUtil.logThrowable(sessionID,
                            "Outgoing message failed validation: " + e.getMessage(), e);
                    return;
                }
            }

            session.send(message);
        } catch (SessionNotFound e) {
            logger.error(e.getMessage(), e);
        }
    }

    private ApplVerID getApplVerID(Session session, Message message) {
        String beginString = session.getSessionID().getBeginString();
        return FixVersions.BEGINSTRING_FIXT11.equals(beginString) ? new ApplVerID(ApplVerID.FIX50) : MessageUtils.toApplVerID(beginString);
    }

    @Override
    public void onCreate(SessionID sessionId) {
        logger.info("onCreate: {}", sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        logger.info("onLogon: {}", sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        logger.info("onLogout: {}", sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) throws DoNotSend {
        try {
            if(logger.isDebugEnabled()) {
                logger.debug("toAdmin: {}", FixUtil.toString(message, dictionaryProvider));
            }
            System.out.println("toAdmin: " + FixUtil.toString(message, dictionaryProvider));
            System.out.println("getExpectedSenderNum: "
                    + Session.lookupSession(sessionId).getExpectedSenderNum());
            System.out.println("getExpectedTargetNum: "
                    + Session.lookupSession(sessionId).getExpectedTargetNum());
        } catch (FieldNotFound e) {
            logger.error(e.getMessage(), e);
        }

        try {
            int seqNum = message.getHeader().getInt(MsgSeqNum.FIELD);
            int lastSeqNum = seqNums.computeIfAbsent(sessionId, key -> new Pair<>(0, 0)).getFirst();
            if (lastSeqNum == seqNum) {
                message.getHeader().setInt(MsgSeqNum.FIELD, seqNum + 1);
            }
            seqNums.get(sessionId).setFirst(seqNum);
            System.out.println("seqNum = " + seqNum);
        } catch (FieldNotFound e1) {
            logger.error(e1.getMessage(), e1);
        }
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        try {
            if(logger.isDebugEnabled()) {
                logger.debug("fromAdmin: {}", FixUtil.toString(message, dictionaryProvider));
            }

            System.out.println("fromAdmin: " + FixUtil.toString(message, dictionaryProvider));
            System.out.println("getExpectedSenderNum: "
                    + Session.lookupSession(sessionId).getExpectedSenderNum());
            System.out.println("getExpectedTargetNum: "
                    + Session.lookupSession(sessionId).getExpectedTargetNum());
        } catch (FieldNotFound e) {
            logger.error(e.getMessage(), e);
        }

        try {
            int seqNum = message.getHeader().getInt(MsgSeqNum.FIELD);
            logger.debug("seqNum = {}", seqNum);
        } catch (FieldNotFound e) {
            logger.error(e.getMessage(), e);
        }

        if (keepMessagesInMemory) {
            try {
                IMessage iMessage = converter.convert(message);
                MsgMetaData metaData = iMessage.getMetaData();

                metaData.setAdmin(true);
                metaData.setFromService(sessionId.getTargetCompID());
                metaData.setToService(sessionId.getSenderCompID());
                metaData.setServiceInfo(serviceInfo);

                try {
                    storage.storeMessage(iMessage);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                ISession iSession = getSession(sessionId);

                try {
                    handler.putMessage(iSession, ServiceHandlerRoute.FROM_ADMIN, iMessage);
                } catch (Exception e) {
                    handler.exceptionCaught(iSession, e);
                }
            } catch(MessageConvertException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        try {
            if(logger.isDebugEnabled()) {
                logger.debug("toApp: {}", FixUtil.toString(message, dictionaryProvider));
            }
        } catch (FieldNotFound e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            if(logger.isDebugEnabled()) {
                logger.debug("fromApp: {}", FixUtil.toString(message, dictionaryProvider));
            }
        } catch (FieldNotFound e) {
            logger.error(e.getMessage(), e);
        }

        if (keepMessagesInMemory) {
            try {
                IMessage iMessage = converter.convert(message);
                MsgMetaData metaData = iMessage.getMetaData();

                metaData.setAdmin(false);
                metaData.setFromService(sessionId.getTargetCompID());
                metaData.setToService(sessionId.getSenderCompID());
                metaData.setServiceInfo(serviceInfo);

                try {
                    storage.storeMessage(iMessage);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                ISession iSession = getSession(sessionId);

                try {
                    handler.putMessage(iSession, ServiceHandlerRoute.FROM_APP, iMessage);
                } catch (Exception e) {
                    handler.exceptionCaught(iSession, e);
                }
            } catch(MessageConvertException e) {
                logger.error(e.getMessage(), e);
            }
        }

        IMessage iMessage = null;
        try {
            iMessage = convert(message, sessionId.getTargetCompID(), sessionId.getSenderCompID(),
                    false);
        } catch (MessageConvertException e) {
            logger.error(e.getMessage(), e);
        }

        IDictionaryStructure dictionaryStructure = messageHelper.getDictionaryStructure();

        if (Objects.nonNull(iMessage)) {

            String messageName = iMessage.getName();
            IMessageStructure messageStructure = dictionaryStructure.getMessages().get(messageName);

            if (Objects.nonNull(messageStructure)) {
                String msgType = getAttributeValue(messageStructure, MessageHelper.FIELD_MESSAGE_TYPE);

                switch (msgType) {
                case "D":
                    onMessage(sessionId, iMessage);
                    break;
                default:
                    logger.error("Received unknown message type {} for session {}", msgType, sessionId.getSessionQualifier());
                    break;
                }
            } else {
                logger.warn("Cant find structure for message {} at {} dictionary", messageName, dictionaryStructure.getNamespace());
            }
        }
    }

    @Override
    public void onMessageRejected(Message message, SessionID sessionId, String reason) {
    }

    @Override
    public List<ISession> getSessions() {
        return new ArrayList<>(sessionMap.values());
    }

    @Override
    public void startLogging() {
        if (logConfigurator != null) {
            logConfigurator.createIndividualAppender(
                    getClass().getName() + "@" + Integer.toHexString(hashCode()), serviceName);
        }
    }

    @Override
    public void stopLogging() {
        if (logConfigurator != null) {
            logConfigurator.destroyIndividualAppender(
                    getClass().getName() + "@" + Integer.toHexString(hashCode()), serviceName);
        }
    }

    private ISession getSession(SessionID sessionID) {

        if (!sessionMap.containsKey(sessionID)) {
            FIXSession session = new FIXSession(getClass().getCanonicalName(), sessionID, storage, converter, messageHelper);
            session.setServiceInfo(serviceInfo);
            sessionMap.put(sessionID, session);
        }

        return fixServerSessionsContainer;
    }

    private IMessage convert(Message message, String from, String to, boolean isAdmin)
            throws MessageConvertException {
        IMessage msg = converter.convert(message);
        MsgMetaData meta = msg.getMetaData();
        meta.setFromService(from);
        meta.setToService(to);
        meta.setAdmin(isAdmin);
        return msg;
    }

    private Message convertToFix(IMessage message) throws InterruptedException {

        message = fixFieldConverter.convertFields(message, messageHelper.getMessageFactory(),
                false);

        Message qfjMessage = new Message();
        Object subMessage = message.removeField(FIELD_HEADER);
        if (subMessage instanceof IMessage) {
            fillMessage(qfjMessage.getHeader(), (IMessage) subMessage);
        } else if (subMessage instanceof List) {
            if (((List<?>) subMessage).size() == 1) {
                fillMessage(qfjMessage.getHeader(), (IMessage) ((List<?>) subMessage).get(0));
            } else {
                throw new RuntimeException(
                        "Message can't be conatins more then one " + FIELD_HEADER);
            }
        }
        subMessage = message.removeField(FIELD_TRAILER);
        if (subMessage instanceof IMessage) {
            fillMessage(qfjMessage.getTrailer(), (IMessage) subMessage);
        } else if (subMessage instanceof List) {
            if (((List<?>) subMessage).size() == 1) {
                fillMessage(qfjMessage.getTrailer(), (IMessage) ((List<?>) subMessage).get(0));
            } else {
                throw new RuntimeException(
                        "Message can't be conatins more then one " + FIELD_TRAILER);
            }
        }

        Object field = message.removeField("8");
        if (field != null) {
            qfjMessage.getHeader().setField(new StringField(8, field.toString()));
        }
        field = message.removeField("35");
        if (field != null) {
            qfjMessage.getHeader().setField(new StringField(35, field.toString()));
        }
        field = message.removeField("34");
        if (field != null) {
            qfjMessage.getHeader().setField(new StringField(34, field.toString()));
        }
        field = message.removeField("9");
        if (field != null) {
            qfjMessage.getHeader().setField(new StringField(9, field.toString()));
        }
        field = message.removeField("10");
        if (field != null) {
            qfjMessage.getTrailer().setField(new StringField(10, field.toString()));
        }

        fillMessage(qfjMessage, message);

        return qfjMessage;
    }

    private void fillMessage(FieldMap fieldMap, IMessage inputMessage) {
        Object value = null;
        for (String fieldName : inputMessage.getFieldNames()) {
            value = inputMessage.getField(fieldName);

            if (!FIELD_GROUP_DELIMITER.equals(fieldName)) {
                try {
                    Integer key = Integer.valueOf(fieldName);

                    if (value instanceof List<?>) {
                        for (Object element : (List<?>) value) {
                            if (element instanceof IMessage) {
                                fieldMap.addGroup(fillGroup(fieldMap, key, (IMessage) element));
                            } else {
                                throw new EPSCommonException(
                                        "Incorrect type of sub message " + element);
                            }
                        }
                    } else if (value instanceof IMessage) {
                        fieldMap.addGroup(fillGroup(fieldMap, key, (IMessage) value));
                    } else {
                        fieldMap.setField(new StringField(key, value.toString()));
                    }

                } catch (NumberFormatException e) {
                    throw new EPSCommonException("Unknown field " + fieldName);
                }
            }
        }
    }

    private Group fillGroup(FieldMap fieldMap, Integer groupField, IMessage inputMessage) {
        Object delimiter = inputMessage.removeField(FIELD_GROUP_DELIMITER);
        if (delimiter != null) {
            try {
                int groupDelimiter = Integer.parseInt(delimiter.toString());

                if (inputMessage.isFieldSet(delimiter.toString())) {
                    Group group = new Group(groupField, groupDelimiter);
                    fillMessage(group, inputMessage);
                    return group;
                } else {
                    throw new EPSCommonException("Sub message [" + inputMessage
                            + "] does not contain required field (" + delimiter + ")");
                }
            } catch (NumberFormatException e) {
                throw new EPSCommonException(
                        FIELD_GROUP_DELIMITER + " " + delimiter + " should has integer type");
            }
        } else {
            throw new EPSCommonException("Group [" + inputMessage + "] does not contain "
                    + FIELD_GROUP_DELIMITER + " field");
        }
    }

    @Override
    public void onConnectionProblem(String reason) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSendToAdmin(Message message, SessionID sessionId) {
        storeOutcomeMessage(message, sessionId, ServiceHandlerRoute.TO_ADMIN);
    }

    @Override
    public void onSendToApp(Message message, SessionID sessionId) {
        storeOutcomeMessage(message, sessionId, ServiceHandlerRoute.TO_APP);
    }

    private void storeOutcomeMessage(Message message, SessionID sessionId, ServiceHandlerRoute route) {
        if (keepMessagesInMemory) {
            ISession iSession = getSession(sessionId);
            try {
                IMessage iMessage = converter.convert(message);
                MsgMetaData metaData = iMessage.getMetaData();

                metaData.setAdmin(route.isAdmin());
                metaData.setFromService(sessionId.getSenderCompID());
                metaData.setToService(sessionId.getTargetCompID());
                metaData.setServiceInfo(serviceInfo);

                try {
                    storage.storeMessage(iMessage);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                handler.putMessage(iSession, route, iMessage);
            } catch (Exception e) {
                handler.exceptionCaught(iSession, e);
            }
        }
    }

    @Override
    public ISession getServerSession() {
        return fixServerSessionsContainer;
    }
}
