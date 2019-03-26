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
package com.exactpro.sf.storage.util;

public class StorageMeasureUtils {

    public static long getSize(String src) {
        int no_chars = (src == null) ? 0 : src.length();
        return (8L * ((((no_chars) << 1) + 45) / 8)); //45bytes is representation of internal string fields
    }

    public static long getSize(String... src) {
        long sum = 0;
        for (String s : src) {
            sum+= getSize(s);
        }
        return sum;
    }

    public static long getSize(byte[] rawMessage) {
        return (long) ((rawMessage == null) ? 16 : (rawMessage.length + 12));
    }
}