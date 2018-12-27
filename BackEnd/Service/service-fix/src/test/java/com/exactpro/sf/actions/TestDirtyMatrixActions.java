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

import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import junit.framework.Assert;
import quickfix.Message;

/*FIXME: Use FixDataDictionaryProvider instead quickfixj
 * Implement enum conversion by DataDictionary instead of by class
*/
@Ignore
public class TestDirtyMatrixActions {

//    @Test
//    public void test1() throws Exception {
//        String expireTime = DirtyMatrixActions.ExpireTime("Y+2:m-6:D=4:h+1:M-2:s=39", null);
//        GregorianCalendar cal = new GregorianCalendar();
//        cal.setTime(new Date(GMTTime.currentTimeMillis()));
//        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 2);
//        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 6);
//        cal.set(Calendar.DAY_OF_MONTH, 4);
//        cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) + 1);
//        cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE) - 2);
//        cal.set(Calendar.SECOND, 39);
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
//
//        System.out.println(expireTime);
//
//        Assert.assertEquals(sdf.format(cal.getTime()), expireTime);
//
//    }

    @Test
    public void testConvertEnumValues() throws InterruptedException {
        HashMap<String, Object> inputData = new HashMap<>();
        inputData.put("ExecType", "NEW");

        Logger logger = Mockito.mock(Logger.class);

        Message msg = DirtyFixUtil.createMessage(logger, inputData, "FIXT.1.1", "8", null);
        Assert.assertEquals("8=FIXT.1.19=1135=8150=010=042", msg.toString());
    }
}
