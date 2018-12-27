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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import java.time.LocalDateTime;

import com.exactpro.sf.util.DateTimeUtility;

public class TestFakeUtils {

    private final FakeUtils instance = new FakeUtils();

    @Test
    public void testExpireTime() {
        
        LocalDateTime localDateTime = DateTimeUtility.nowLocalDateTime();

        System.out.println(localDateTime);

        System.out.println(instance.ExpireTime("Y+1:M+1:D+1:h+1:m+1:s+1:ms+1", "yyyyMMdd-HH:mm:ss.SSS", "UTC"));

        System.out.println(instance.ExpireTime("Y+1:M+1:D+1:h+1:m+1:s+1:ms+1"));

        assertEquals(instance.ExpireTime(LocalDateTime.of(2005, 6, 7, 8, 9, 10, 11_000_000)), "20050607-08:09:10");
    }

    @Test
    public void testExpireDate() {
        LocalDateTime localDateTime = DateTimeUtility.nowLocalDateTime();

        System.out.println(localDateTime);

        System.out.println(instance.ExpireDate("Y+1:M+1:D+1:h+1:m+1:s+1:ms+1"));
    }

    // ExpireTime(String, String, String)
    // ExpireTime1(String, String, String)
    //
    // ExpireTime(String)
    // ExpireTime1(String)
    //
    // ExpireTime(Date)
    // ExpireTime(LocalDateTime)
    //
    // ExpireDate(String)
    // ExpireDate1(String)
}
