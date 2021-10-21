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
package com.exactpro.sf.services.itch;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.primitives.UnsignedLong;

public class ITCHVisitorBase extends DefaultMessageStructureVisitor {

	public static final String MESSAGE_TYPE_ATTRIBUTE = "MessageType";
	public static final String TYPE_ATTRIBUTE = "Type";
    public static final String LENGTH_ATTRIBUTE = "Length";
    public static final String COUNT_ATTRIBUTE = "CountField";
    public static final String ISADMIN = "IsAdmin";
    public static final String IMPILED_DECIMALS_ATTRIBUTE = "ImpliedDecimals";
    public static final String DATE_TIME_FORMAT = "DateTimeFormat";

    public static final UnsignedLong SIZE_DEVIDER = UnsignedLong.valueOf(100_000_000L);
    public static final UnsignedLong SIZE4_DEVIDER = UnsignedLong.valueOf(10_000L);

    public static final BigDecimal BD_SIZE_DEVIDER = new BigDecimal(100_000_000L);
    public static final BigDecimal BD_PRICE_DEVIDER = new BigDecimal(100_000_000L);
    public static final BigDecimal BD_UDT_DEVIDER = new BigDecimal(1_000_000_000L);
    public static final BigDecimal UDT_DEVIDER = new BigDecimal(1_000_000L);
    public static final DateTimeFormatter DATE_AS_INT = DateTimeUtility.createFormatter("yyyyddMM");

    protected static final String charsetName = "ISO-8859-1";
    
	protected static final ThreadLocal<CharsetEncoder> encoder = new ThreadLocal<CharsetEncoder>() { //Charset.forName(charsetName).newEncoder();
		@Override
        protected CharsetEncoder initialValue() {
			return Charset.forName(charsetName).newEncoder();
		}
	};
	public static final ThreadLocal<CharsetDecoder> decoder = new ThreadLocal<CharsetDecoder>() { //Charset.forName(charsetName).newEncoder();
		@Override
        protected CharsetDecoder initialValue() {
			return Charset.forName(charsetName).newDecoder();
		}
	};
	
	public enum ProtocolType {
		
		ALPHA("Alpha"),
		ALPHA_NOTRIM("Alpha_notrim"), // DECODE ONLY
		DATE("Date"),
		DAYS("Days"), // Represented as number of days since 1 January 1970. (Binary field)
		TIME("Time"), // just a string
        DATE_TIME("Date_Time"),
		// 4 or 8 bytes depends on type (Float/Double)
		PRICE("Price"),
		PRICE4("Price4"), // price with 4 implied decimal (price / 10^4)
		SIZE("Size"),
		SIZE4("Size4"),   // size with 4 implied decimal
		
		UDT("UDT"),    // Unix timestamp(in UTC) = (date time per second resolution in unix time format) * 1,000,000,000 + (nanoseconds component)
		STUB("STUB"),  // RM9425
		
		BYTE("Byte"), // FIXME: sometimes it signed, sometimes not... strange
		UINT8("UInt8"),
		UINT16("UInt16"),
		UINT32("UInt32"),
		UINT64("UInt64"),
		INT8("Int8"),
		INT16("Int16"),
		INT32("Int32"),
		INT64("Int64"),
        UINTXX("UIntXX");
        
        private final String type;
        
        ProtocolType(String type) {
            this.type = type;
        }
        
        public static ProtocolType getEnum(String type) {
            for (ProtocolType protocolType : ProtocolType.values()) {
                if (protocolType.type.equals(type)) {
                    return protocolType;
                }
            }
            throw new EPSCommonException("Unknown type = [" + type + "]");
        }
	}

	static boolean encodeString(String str, byte[] array) {
		Arrays.fill(array, (byte) 0x20);
		ByteBuffer buffer = ByteBuffer.wrap(array);
		CharBuffer charBuffer = CharBuffer.wrap(str);

		CoderResult result = encoder.get().encode(charBuffer, buffer, true);

		return !result.isOverflow();
	}

}
