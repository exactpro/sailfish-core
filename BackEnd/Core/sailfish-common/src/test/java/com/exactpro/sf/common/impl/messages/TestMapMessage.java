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
package com.exactpro.sf.common.impl.messages;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Assert;
import org.junit.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.util.DateTimeUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @author nikita.smirnov
 *
 */

public class TestMapMessage {
    
    @Test
    public void testMapMessageSerializationWithJson() throws IOException {
        MapMessage message = new MapMessage("namespace", "name");
        message.addField("boolean", true);
        message.addField("boolean_array", Arrays.asList(new Boolean[] { true, false }));
        message.addField("byte", (byte) 1);
        message.addField("byte_array", Arrays.asList(new Byte[] { 2, 3, 4 }));
        message.addField("short", (short) 5);
        message.addField("short_array", Arrays.asList(new Short[] { 6, 7, 8 }));
        message.addField("int", 9);
        message.addField("int_array", Arrays.asList(new Integer[] { 10, 11, 12 }));
        message.addField("long", 13l);
        message.addField("long_array", Arrays.asList(new Long[] { 14l, 15l, 16l }));
        message.addField("float", 17.1f);
        message.addField("float_array", Arrays.asList(new Float[] { 18.1f, 19.1f, 20.1f }));
        message.addField("double", 21.1d);
        message.addField("double_array", Arrays.asList(new Double[] { 22.1d, 23.1d, 24.1d }));
        message.addField("bigdecimal", new BigDecimal("25.1"));
        message.addField("bigdecimal_array", Arrays.asList(new BigDecimal[] { new BigDecimal("26.1"), new BigDecimal("27.1"), new BigDecimal("28.1") }));
        message.addField("char", 'a');
        message.addField("char_array", Arrays.asList(new Character[] { 'b', 'c', 'd' }));
        message.addField("string", "ef");
        message.addField("string_array", Arrays.asList(new String[] { "gh", "jk", "lm" }));
        message.addField("localdatetime", DateTimeUtility.nowLocalDateTime());
        message.addField("localdatetime_array", Arrays.asList(new LocalDateTime[] { DateTimeUtility.nowLocalDateTime(), DateTimeUtility.nowLocalDateTime(), DateTimeUtility.nowLocalDateTime() }));
        message.addField("localdate", DateTimeUtility.nowLocalDate());
        message.addField("localdate_array", Arrays.asList(new LocalDate[] { DateTimeUtility.nowLocalDate(), DateTimeUtility.nowLocalDate(), DateTimeUtility.nowLocalDate() }));
        message.addField("localtime", DateTimeUtility.nowLocalTime());
        message.addField("localtime_array", Arrays.asList(new LocalTime[] { DateTimeUtility.nowLocalTime(), DateTimeUtility.nowLocalTime(), DateTimeUtility.nowLocalTime() }));
        
        MsgMetaData metaData = message.getMetaData();
        metaData.setDictionaryURI(SailfishURI.unsafeParse("plugin.dictionary"));
        metaData.setFromService("fromService");
        metaData.setToService("toService");
        metaData.setRawMessage(new byte[] { 1, 2, 3, 4, 5 });
        metaData.setServiceInfo(new ServiceInfo("id", new ServiceName("env", "serviceName")));
        
        MapMessage subMessage = message.cloneMessage();
        message.addField("message", subMessage);
        message.addField("message_array", Arrays.asList(new MapMessage[] { subMessage, subMessage }));
        
        ObjectMapper objectMapper = new ObjectMapper().enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
                .registerModule(new JavaTimeModule());
        ObjectReader reader = objectMapper.reader(IMessage.class);
        ObjectWriter writer = objectMapper.writer();
        
        String json = writer.writeValueAsString(message);
        
        IMessage message2 = reader.readValue(json);
        
        Assert.assertEquals(message, message2);
    }
}
