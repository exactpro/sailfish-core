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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;

public class TestMessageUtil {
    private MessageUtil messageUtil = new MessageUtil();

    @Test
    public void testFindGroup() {
        try {
            messageUtil.FindGroup(null, null, null);
            Assert.fail("Null list");
        } catch (EPSCommonException e) {
            e.printStackTrace();
        }

        List<IMessage> list = new ArrayList<IMessage>();

        try {
            messageUtil.FindGroup(list, null, null);
            Assert.fail("Null field name");
        } catch (EPSCommonException e) {
            e.printStackTrace();
        }

        String fieldName = "field";

        IMessage msg = new MapMessage("namespace", "name");
        msg.addField("f1", "v1");
        list.add(msg);

        Assert.assertEquals(msg, messageUtil.FindGroup(list, fieldName, null));

        Object value = 1;

        msg = new MapMessage("namespace", "name");
        msg.addField(fieldName, Integer.valueOf(1));
        list.add(msg);

        Assert.assertEquals(msg, messageUtil.FindGroup(list, fieldName, value));
    }

    @Test
    public void testFindField() {
        try {
            messageUtil.FindField(null, null, null, null);
            Assert.fail("Null list");
        } catch (EPSCommonException e) {
            e.printStackTrace();
        }

        List<IMessage> list = new ArrayList<IMessage>();

        try {
            messageUtil.FindField(list, null, null, null);
            Assert.fail("Null field name");
        } catch (EPSCommonException e) {
            e.printStackTrace();
        }

        String fieldName = "field";

        try {
            messageUtil.FindField(list, fieldName, null, null);
            Assert.fail("Null target field name");
        } catch (EPSCommonException e) {
            e.printStackTrace();
        }

        String target = "target";

        IMessage msg = new MapMessage("namespace", "name");
        msg.addField("f1", "v1");
        list.add(msg);

        Assert.assertEquals(null, messageUtil.FindField(list, fieldName, null, target));

        Object value = 1;
        Object targetValue = "H2O";

        msg = new MapMessage("namespace", "name");
        msg.addField(fieldName, Integer.valueOf(1));
        msg.addField(target, targetValue);
        list.add(msg);

        Assert.assertEquals(targetValue, messageUtil.FindField(list, fieldName, value, target));
    }
}
