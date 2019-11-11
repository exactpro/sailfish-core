/*******************************************************************************
 *   Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.services.fast.filter;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openfast.Message;
import org.openfast.template.TemplateRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.CoreVersion;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.fast.FASTMessageHelper;
import com.exactpro.sf.services.fast.FastTemplateLoader;
import com.exactpro.sf.services.fast.converter.ConverterException;
import com.exactpro.sf.services.fast.converter.IMessageToFastConverter;
import com.exactpro.sf.util.FASTServicePluginTest;

@RunWith(Parameterized.class)
public class TestFastFilter extends FASTServicePluginTest {
    private static final Logger logger = LoggerFactory.getLogger(TestFastFilter.class);
    private static final String TEMPLATE_TITLE = "FAST";
    private static final String CORE_ALIAS = new CoreVersion().getAlias();
    private static final SailfishURI DICTIONARY_URI = SailfishURI.unsafeParse(CORE_ALIAS + ':' + TEMPLATE_TITLE);

    private final String filterString;
    private final boolean resultFilter;

    public TestFastFilter(String filterString, boolean resultFilter) {
        this.filterString = filterString;
        this.resultFilter = resultFilter;
    }

    @Parameters(name = "Test #{index}:check(filterString: {0}, expected result: {1})")
    public static Iterable<Object[]> dataForTest() {
        return Arrays.asList(new Object[][] {
                { "MsgType = 0; MsgType = 1", true },
                { "MsgType = 0; ApplReqID = 0", true },
                { "MsgType = 0; ApplReqID = 1", false },
                { "MsgType = 1; ApplReqID = 0", false },
                { "MsgType = ![1]; ApplReqID = 0", true },
                { "MsgType = ![1]; ApplReqID = ![1]", true },
                { "", true },
                { null, true },
                { "ApplReqID = 0", true },
                { "ApplReqID = 4", false },
                { "ApplReqID = [0]", true },
                { "ApplReqID = [4]", false },
                { "ApplReqID = ![1]", true },
                { "ApplReqID = ![0]", false },
                { "RefApplID = [TMP]", true },
                { "RefApplID = [NOTMP]", false },
                { "RefApplID = [TMP]; RefApplID = [NOTMP]", true },
                { "RefApplID = [TMP]; ApplReqID = 0", true },
                { "RefApplID = [TMP]; ApplReqID = 1", false },
                { "RefApplID = ![TMP]", false },
                { "RefApplID = ![NOTMP]", true },
                { "MsgType = 0; MsgType = 1; ApplReqID = 0", true },
                { "MsgType = 0; MsgType = 1; ApplReqID = 1", false },
                { "MsgType1 = 0", true },
                { "MsgType1 = ![0]", true },
                { "MsgType1 = 0; ApplReqID = [0]", true }
        });
    }

    @Test
    public void testFilterFastMessage() {
        IMessageFactory msgFactory = DefaultMessageFactory.getFactory();

        String pathName = SAILFISH_DICTIONARY_PATH + TEMPLATE_TITLE + ".xml";
        IDictionaryStructure dictionary = serviceContext.getDictionaryManager().createMessageDictionary(pathName);
        IDataManager dataManager = Objects.requireNonNull(serviceContext.getDataManager(), "'Data manager' parameter");
        String fastTemplate = Objects.requireNonNull(getAttributeValue(dictionary, FASTMessageHelper.TEMPLATE_ATTRIBYTE), "'Template attribute' parameter");

        FastTemplateLoader templateLoader = new FastTemplateLoader();
        TemplateRegistry registry = templateLoader.loadFastTemplates(dataManager, DICTIONARY_URI.getPluginAlias(), fastTemplate);

        IMessageToFastConverter converter = new IMessageToFastConverter(dictionary, registry);

        IMessage message = msgFactory.createMessage("ApplicationMessageRequest", "fast");
        message.addField("MsgType", "0");
        message.addField("SendingTime", "20160210-07:10:06.193");
        message.addField("ApplReqID", "0");
        message.addField("ApplReqType", 0L);
        message.addField("NoApplIDs", 1L);

        List<IMessage> list = new ArrayList<>();

        IMessage subMessage = msgFactory.createMessage("ApplicationMessageRequest_IndicesRequestEntries", "fast");
        subMessage.addField("RefApplID", "TMP");
        subMessage.addField("Reserved1", 6L);
        subMessage.addField("ApplBegSeqNum", 4328L);
        subMessage.addField("ApplEndSeqNum", 4333L);
        list.add(subMessage);

        subMessage = msgFactory.createMessage("ApplicationMessageRequest_IndicesRequestEntries", "fast");
        subMessage.addField("RefApplID", "TMP");
        subMessage.addField("Reserved1", 7L);
        subMessage.addField("ApplBegSeqNum", 4329L);
        subMessage.addField("ApplEndSeqNum", 4334L);
        list.add(subMessage);

        message.addField("IndicesRequestEntries", list);

        Assert.assertEquals(resultFilter, isMessageAcceptable(message, new SimpleMessageFilter(filterString), converter));

    }

    private static boolean isMessageAcceptable(IMessage message, SimpleMessageFilter simpleMessageFilter, IMessageToFastConverter converter) {
        Message fastMsg = null;
        try {
            fastMsg = converter.convert(message);
        } catch (ConverterException e) {
            logger.error("Error convert message", e);
        }

        return simpleMessageFilter.isMessageAcceptable(fastMsg);
    }
}