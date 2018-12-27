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

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.services.fix.FixUtil;

/**
 * These utility methods were extracted from {@link FixConnectivityActions} class
 */
@MatrixUtils
@ResourceAliases("FixConnectivityUtil")
public class FixConnectivityUtil extends AbstractCaller {
    private final DateUtil dateUtil = new DateUtil();

    private int messageSeqNumber = 1;

    /**
     * Generate unique ClOrID for new order.
     * @return
     */
    @Description("Generate unique ClOrID for new order.<br>"
            + "Example:<br>"
            + "first call will take current time in millis and adds 1, so for "
            + " current time 1276883000000<br>"
            + "#{ClOrdID()}<br>"
            + "will return:<br>"
            + "1276883000001<br>"
            +" next call will return:<br>"
            + "1276883000002<br>")
    @UtilityMethod
    public final String ClOrdID()
    {
        return FixUtil.generateClorID();
    }

    @Description("Formats current date time in UTC time zone modified by pattern into string using <code>yyyyMMdd-HH:mm:ss</code> format pattern" + DateUtil.MODIFY_HELP + "Usage: #{Date(modifyPattern)}")
    @UtilityMethod
    public final String Date(String modifyPattern) {
        return dateUtil.formatDateTime("yyyyMMdd-HH:mm:ss", modifyPattern);
    }

    // find other resolution
    // needed functions

    @Description("Add to current MsgSeqNum specified integer value.<br>"
            + "Example:<br>"
            + "#{addMsgSeqNum(10)}<br>"
            + "adds 10 to current MsgSeqNum")
    @UtilityMethod
    public final int addMsgSeqNum(int corr)
    {
        messageSeqNumber = messageSeqNumber + corr;
        return ( messageSeqNumber );
    }

    @Description("Set current MsgSeqNum to specified integer value.<br>"
            + "Example:<br>"
            + "#{setMsgSeqNum(10)}<br>"
            + "set current MsgSeqNum to 10")
    @UtilityMethod
    public final int setMsgSeqNum(int corr)
    {
        messageSeqNumber =  corr;
        return ( messageSeqNumber );
    }

    @Description("Formats current date time in UTC time zone modified by pattern into string using <code>yyyyMMdd-HH:mm:ss.SSS</code> format pattern" + DateUtil.MODIFY_HELP + "Usage: #{DateMS(modifyPattern)}")
    @UtilityMethod
    public final String DateMS(String modifyPattern) {
        return dateUtil.formatDateTime("yyyyMMdd-HH:mm:ss.SSS", modifyPattern);
    }
}
