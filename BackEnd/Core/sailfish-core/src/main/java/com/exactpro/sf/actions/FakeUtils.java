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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.util.DateTimeUtility;

@ResourceAliases({"FakeUtils"})
public class FakeUtils extends AbstractCaller {
    private static final String FIX_DATE_FORMAT = "yyyyMMdd-HH:mm:ss";
    private static final DateTimeFormatter FIX_FORMATTER = DateTimeUtility.createFormatter(FIX_DATE_FORMAT);

    @UtilityMethod
    public final String ClOrdID() {
        return String.valueOf(System.currentTimeMillis());
    }

    @UtilityMethod
    public final String ExpireTime(String dateFormat, String format, String timeZone) {
        return DateUtil.formatDateTime(dateFormat, format, timeZone);
    }

    @UtilityMethod
    public final String ExpireTime(String dateFormat) {
        return DateUtil.modifyZonedDateTime(dateFormat).format(FIX_FORMATTER);
    }

    @UtilityMethod
    public final String ExpireTime(LocalDateTime date) {
        return FIX_FORMATTER.format(date);
    }

    @UtilityMethod
    public final String ExpireDate(String dateFormat) {
        // 2011.09.16 DG: Return String as soon as tag ExpireDate defined as String in QFJ.
        LocalDateTime localDateTime = DateUtil.modifyLocalDateTime(dateFormat);

        int year = localDateTime.getYear();
        int month = localDateTime.getMonth().getValue();
        int day = localDateTime.getDayOfMonth();

        StringBuilder sb = new StringBuilder();

        sb.append(year);

        if(month < 10) {
            sb.append(0);
        }
        sb.append(month);

        if(day < 10) {
            sb.append(0);
        }
        sb.append(day);

        return sb.toString();
    }
}
