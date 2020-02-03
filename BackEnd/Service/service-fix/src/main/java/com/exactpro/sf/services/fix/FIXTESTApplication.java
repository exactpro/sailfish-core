/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
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
import quickfix.FieldNotFound;
import quickfix.FixVersions;
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
import quickfix.UnsupportedMessageType;
import quickfix.field.ApplVerID;
import quickfix.field.MsgSeqNum;
import quickfix.field.OrdType;

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

    private final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

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

    private void fillFixHeader(IMessage msg, IMessageStructure msgStructure) {
        IMessage header = (IMessage) msg.getField(FIELD_HEADER);
        if (header == null) {
            header = messageHelper.getMessageFactory().createMessage(FIELD_HEADER,
                    messageHelper.getNamespace());
            msg.addField(FIELD_HEADER, header);
        }
        String messageType = getAttributeValue(msgStructure, MessageHelper.FIELD_MESSAGE_TYPE);
        header.addField(MSG_TYPE, messageType);

        msg.addField(FIELD_TRAILER, messageHelper.getMessageFactory()
                .createMessage(FIELD_TRAILER, messageHelper.getNamespace()));

    }

    public void fillMandatoryFields(IMessage message, IMessageStructure msgStructure) {
        if (msgStructure.getFields().containsKey(FixMessageHelper.HEADER)) {
            fillFixHeader(message, msgStructure);
        }

        msgStructure.getFields().forEach((name, field) -> {
            // exclude header and trailer
            if (FIELD_HEADER.equals(name) || FIELD_TRAILER.equals(name)) {
                return;
            }

            if (!message.isFieldSet(name) && field.isRequired()) {
                Object value = null;

                if (field.isComplex()) {
                    IMessage nested = messageHelper.getMessageFactory().createMessage(field.getReferenceName(), field.getNamespace());
                    fillMandatoryFields(nested, (IMessageStructure)field);
                    value = nested;
                } else if (field.isEnum()) {
                    value = field.getValues().values().iterator().next().getCastValue();
                } else {
                    value = getRandomObjectByJavaType(field.getJavaType());
                }

                message.addField(name, field.isCollection() ? Collections.singletonList(value) : value);
            }
        });
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
            answer.addField("Text", epMessage.getField("Text"));
            answer.addField("ClOrdID", epMessage.getField("ClOrdID"));

            IMessageStructure msgStructure = messageHelper.getDictionaryStructure().getMessages().get(answer.getName());
            fillMandatoryFields(answer, msgStructure);
            Message qfjMessage = converter.convert(answer, true);
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
                answer.addField("Text", epMessage.getField("Text"));
                answer.addField("OrderQty", orderQty);
                answer.addField("LastPx", price);
                answer.addField("AvgPx", price);

                fillMandatoryFields(answer, msgStructure);
                qfjMessage = converter.convert(answer, true);

                sendMessage(sessionID, qfjMessage);
            }

        } catch (RuntimeException | MessageConvertException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    private boolean isOrderExecutable(IMessage order, BigDecimal price) {
        if (Objects.equals(order.getField("OrdType"), '2')) {
            String orderPrice = order.getField("Price");
            Character orderSide = order.getField("Side");

            if (orderPrice == null || orderSide == null) {
                return false;
            }

            BigDecimal limitPrice = new BigDecimal(orderPrice);

            return (orderSide == '1' && price.compareTo(limitPrice) <= 0)
                    || (orderSide == '2' || orderSide == '5') && price.compareTo(limitPrice) >= 0;
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

    public String genOrderID() {
        return String.valueOf(++m_orderID);
    }

    public String genExecID() {
        return String.valueOf(++m_execID);
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
            logConfigurator.registerLogger(this, serviceName);
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
