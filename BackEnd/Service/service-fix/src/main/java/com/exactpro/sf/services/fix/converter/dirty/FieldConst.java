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
package com.exactpro.sf.services.fix.converter.dirty;

import quickfix.field.BeginString;
import quickfix.field.BodyLength;
import quickfix.field.CheckSum;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.SendingTime;
import quickfix.field.TargetCompID;

public class FieldConst {
    public static final String FIELD_ORDER = "FieldOrder";
    public static final String GROUP_COUNTERS = "GroupCounters";
    /**
     * @deprecated Use FIELD_ORDER instead this
     */
    @Deprecated
    public static final String FIELD_GROUP_DELIMITER = "GroupDelimiter";
    
    public static final Object EXCLUDED_FIELD = new Object() {
        public String toString() {
            return "excluded field";
        };
    };  

    public static final String HEADER = "header";
    public static final String TRAILER = "trailer";

    public static final String BEGIN_STRING = String.valueOf(BeginString.FIELD);
    public static final String BODY_LENGTH = String.valueOf(BodyLength.FIELD);
    public static final String CHECKSUM = String.valueOf(CheckSum.FIELD);
    public static final String MSG_SEQ_NUM = String.valueOf(MsgSeqNum.FIELD);
    public static final String MSG_TYPE = String.valueOf(MsgType.FIELD);
    public static final String SENDER_COMP_ID = String.valueOf(SenderCompID.FIELD);
    public static final String TARGET_COMP_ID = String.valueOf(TargetCompID.FIELD);
    public static final String SENDING_TIME = String.valueOf(SendingTime.FIELD);
}
