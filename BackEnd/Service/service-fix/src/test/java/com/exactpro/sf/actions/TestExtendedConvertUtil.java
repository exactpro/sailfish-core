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

import org.junit.Test;
import org.mockito.Mockito;

import junit.framework.Assert;
import quickfix.Field;

public class TestExtendedConvertUtil {
    
    @Test
    public void testToChar() {
        ExtendedConvertUtil convertUtil = new ExtendedConvertUtil();

        @SuppressWarnings("rawtypes")
        Field field = Mockito.mock(Field.class);

        Mockito.when(field.getObject()).thenReturn('E');
        Assert.assertEquals(Character.valueOf('E'), convertUtil.toChar(field));

        Mockito.when(field.getObject()).thenReturn("F");
        Assert.assertEquals(Character.valueOf('F'), convertUtil.toChar(field));

        Mockito.when(field.getObject()).thenReturn(3);
        Assert.assertEquals(Character.valueOf('3'), convertUtil.toChar(field, true));
    }
}
