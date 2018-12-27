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

import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.exactpro.sf.common.impl.messages.IBaseEnumField;

import junit.framework.Assert;

/**
 * @author nikita.smirnov
 *
 */
public class TestConvertUtil {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Test
    public void testToChar() {
        ConvertUtil convertUtil = new ConvertUtil();

        Assert.assertEquals(Character.valueOf('A'), convertUtil.toChar('A'));
        Assert.assertEquals(Character.valueOf('B'), convertUtil.toChar("B"));
        Assert.assertEquals(Character.valueOf('`'), convertUtil.toChar("0", true));
        Assert.assertEquals(Character.valueOf('1'), convertUtil.toChar(1, true));

        IBaseEnumField baseEnumField = Mockito.mock(IBaseEnumField.class);

        Mockito.when(baseEnumField.getObjectValue()).thenReturn('C');
        Assert.assertEquals(Character.valueOf('C'), convertUtil.toChar(baseEnumField));

        Mockito.when(baseEnumField.getObjectValue()).thenReturn("D");
        Assert.assertEquals(Character.valueOf('D'), convertUtil.toChar(baseEnumField));

        Mockito.when(baseEnumField.getObjectValue()).thenReturn(2);
        Assert.assertEquals(Character.valueOf('2'), convertUtil.toChar(baseEnumField, true));

        try {
            convertUtil.toChar("");
            fail("empty");
        } catch(IllegalArgumentException e1) {
            try {
                convertUtil.toChar("GI");
                fail("GI");
            } catch(IllegalArgumentException e2) {
                try {
                    convertUtil.toChar(4l);
                    fail("4l");
                } catch(IllegalArgumentException e3) {
                    try {
                        convertUtil.toChar(5.5f);
                        fail("5.5f");
                    } catch(IllegalArgumentException e4) {
                        try {
                            convertUtil.toChar(6.6d);
                            fail("6.6d");
                        } catch(IllegalArgumentException e5) {
                            try {
                                convertUtil.toChar(7.7d);
                                fail("7.7d");
                            } catch(IllegalArgumentException e6) {
                                try {
                                    convertUtil.toChar(new BigDecimal(8.8d));
                                    fail("8.8d");
                                } catch(IllegalArgumentException e7) {

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

