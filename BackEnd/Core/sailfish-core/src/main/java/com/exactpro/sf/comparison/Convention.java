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
package com.exactpro.sf.comparison;

import java.math.BigDecimal;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.DateTimeUtility;

/**
 * Convention used for change "*" or "#" to appropriate value for filter message
 * to check whether value exists in a message or not exist.
 *
 * @author dmitry.guriev
 *
 */
public class Convention {


    public static final Object CONV_PRESENT_OBJECT = StaticUtil.notNullFilter(0, "Unknown");
	public static final String CONV_PRESENT_STRING = "*";
	public static final byte CONV_PRESENT_BYTE = Byte.MAX_VALUE;
	public static final int CONV_PRESENT_INTEGER = -999;
	public static final long CONV_PRESENT_LONG = -999L;
	public static final double CONV_PRESENT_DOUBLE = -999.0;
	public static final float CONV_PRESENT_FLOAT = -999.0f;
    public static final LocalDate CONV_PRESENT_LOCAL_DATE = DateTimeUtility.MIN_DATE;
    public static final LocalTime CONV_PRESENT_LOCAL_TIME = DateTimeUtility.MIN_TIME;
    public static final LocalDateTime CONV_PRESENT_LOCAL_DATE_TIME = DateTimeUtility.MIN_DATE_TIME;
	public static final BigDecimal CONV_PRESENT_BIG_DECIMAL = new BigDecimal(-999);
    public static final String CONV_PRESENT_DATEONLY_FIX_STRING = "19700101";
	public static final String CONV_PRESENT_TIMEONLY_FIX_STRING = "00:00:00.000";
    public static final String CONV_PRESENT_DATE_FIX_STRING = CONV_PRESENT_DATEONLY_FIX_STRING + '-' + CONV_PRESENT_TIMEONLY_FIX_STRING;
	public static final String CONV_PRESENT_CHAR_FIX_STRING = "\000";
	public static final char CONV_PRESENT_CHAR = '\000';
	public static final short CONV_PRESENT_SHORT = Short.MAX_VALUE;
	public static final String CONV_PRESENT_ENUM = "Present";

	public static final Object CONV_MISSED_OBJECT = StaticUtil.nullFilter(0, "Unknown");
	public static final String CONV_MISSED_STRING = "#";
	public static final byte CONV_MISSED_BYTE = Byte.MIN_VALUE;
	public static final int CONV_MISSED_INTEGER = -998;
	public static final long CONV_MISSED_LONG = -998L;
	public static final double CONV_MISSED_DOUBLE = -998.0;
	public static final float CONV_MISSED_FLOAT = -998.0f;
    public static final LocalDate CONV_MISSED_LOCAL_DATE = DateTimeUtility.MIN_DATE.plusDays(1);
    public static final LocalTime CONV_MISSED_LOCAL_TIME = DateTimeUtility.MIN_TIME.plusSeconds(1);
    public static final LocalDateTime CONV_MISSED_LOCAL_DATE_TIME = LocalDateTime.of(CONV_MISSED_LOCAL_DATE, CONV_MISSED_LOCAL_TIME);
	public static final BigDecimal CONV_MISSED_BIG_DECIMAL = new BigDecimal(-998);
    public static final String CONV_MISSED_DATEONLY_FIX_STRING = "19700102";
	public static final String CONV_MISSED_TIMEONLY_FIX_STRING = "00:00:01.000";
    public static final String CONV_MISSED_DATE_FIX_STRING = CONV_MISSED_DATEONLY_FIX_STRING + '-' + CONV_MISSED_TIMEONLY_FIX_STRING;
	public static final String CONV_MISSED_CHAR_FIX_STRING = "\001";
	public static final char CONV_MISSED_CHAR = '\001';
	public static final short CONV_MISSED_SHORT = Short.MIN_VALUE;
	public static final String CONV_MISSED_ENUM = "Missed";

	private Convention()
	{
		// hide constructor
	}

	public static final String getReplacementForPresent(Class<?> type)
	{
		if (type.equals(String.class)) {
			return "\""+CONV_PRESENT_STRING+"\"";
		}
		if (type.equals(Byte.class) || type.equals(byte.class)) {
            return "(byte)" + String.valueOf(CONV_PRESENT_BYTE);
        }
		if (type.equals(Integer.class) || type.equals(int.class)) {
			return String.valueOf(CONV_PRESENT_INTEGER);
		}
		if (type.equals(Long.class) || type.equals(long.class)) {
			return CONV_PRESENT_LONG+"L";
		}
		if (type.equals(Double.class) || type.equals(double.class)) {
			return String.valueOf(CONV_PRESENT_DOUBLE);
		}
		if (type.equals(Float.class) || type.equals(float.class)) {
			return String.valueOf(CONV_PRESENT_FLOAT)+"f";
		}
		if (type.equals(Character.class) || type.equals(char.class)) {
			return "'\\u0000'";
		}
		if (type.equals(Short.class) || type.equals(short.class)) {
			return "Short.MAX_VALUE";
		}
        if (type.equals(LocalDateTime.class)) {
            return "DateTimeUtility.MIN_DATE_TIME";
        }
        if (type.equals(LocalDate.class)) {
            return "DateTimeUtility.MIN_DATE";
        }
        if (type.equals(LocalTime.class)) {
            return "DateTimeUtility.MIN_TIME";
        }
		if (type.equals(BigDecimal.class)) {
			return "new java.math.BigDecimal(-999)";
		}
		throw new EPSCommonException("Present convention for type not defined: "+type);
	}

	public static final String getReplacementForMissed(Class<?> type)
	{
		if (type.equals(String.class)) {
			return "\""+CONV_MISSED_STRING+"\"";
		}
		if (type.equals(Byte.class) || type.equals(byte.class)) {
            return "(byte)" + String.valueOf(CONV_MISSED_BYTE);
        }
		if (type.equals(Integer.class) || type.equals(int.class)) {
			return String.valueOf(CONV_MISSED_INTEGER);
		}
		if (type.equals(Long.class) || type.equals(long.class)) {
			return CONV_MISSED_LONG+"L";
		}
		if (type.equals(Double.class) || type.equals(double.class)) {
			return String.valueOf(CONV_MISSED_DOUBLE);
		}
		if (type.equals(Float.class) || type.equals(float.class)) {
			return String.valueOf(CONV_MISSED_FLOAT)+"f";
		}
		if (type.equals(Character.class) || type.equals(char.class)) {
			return "'\\u0001'";
		}
		if (type.equals(Short.class) || type.equals(short.class)) {
			return "Short.MIN_VALUE";
		}
        if (type.equals(LocalDateTime.class)) {
            return "DateTimeUtility.MIN_DATE_TIME.plusDays(1).plusSeconds(1)";
        }
        if (type.equals(LocalDate.class)) {
            return "DateTimeUtility.MIN_DATE.plusDays(1)";
        }
        if (type.equals(LocalTime.class)) {
            return "DateTimeUtility.MIN_TIME.plusSeconds(1)";
        }
		if (type.equals(BigDecimal.class)) {
			return "new java.math.BigDecimal(-998)";
		}
		throw new EPSCommonException("Missed convention for type not defined: "+type);
	}

	public static final boolean hasConventionForType(Class<?> type)
	{
		if (type.equals(String.class)) {
			return true;
		}
		if (type.equals(Byte.class) || type.equals(byte.class)) {
            return true;
        }
		if (type.equals(Integer.class) || type.equals(int.class)) {
			return true;
		}
		if (type.equals(Long.class) || type.equals(long.class)) {
			return true;
		}
		if (type.equals(Double.class) || type.equals(double.class)) {
			return true;
		}
		if (type.equals(Float.class) || type.equals(float.class)) {
			return true;
		}
		if (type.equals(Character.class) || type.equals(char.class)) {
			return true;
		}
		if (type.equals(Short.class) || type.equals(short.class)) {
			return true;
		}
        if (type.equals(LocalDateTime.class)) {
            return true;
        }
        if (type.equals(LocalDate.class)) {
            return true;
        }
        if (type.equals(LocalTime.class)) {
            return true;
        }
		if (type.equals(BigDecimal.class)) {
			return true;
		}
		return false;
	}

	public static final boolean isConventionedValuePresent(Object value)
	{
		Class<? extends Object> type = value.getClass();
		if (type.equals(String.class)) {
			return value.equals(CONV_PRESENT_STRING)
			|| value.equals(Convention.CONV_PRESENT_DATE_FIX_STRING)
			|| value.equals(Convention.CONV_PRESENT_DATEONLY_FIX_STRING)
			|| value.equals(Convention.CONV_PRESENT_TIMEONLY_FIX_STRING)
			|| value.equals(Convention.CONV_PRESENT_CHAR_FIX_STRING);
		} else if (type.equals(Integer.class)) {
			return value.equals(CONV_PRESENT_INTEGER);
		} else if (type.equals(Long.class)) {
			return value.equals(CONV_PRESENT_LONG);
		} else if (type.equals(Double.class)) {
			return value.equals(CONV_PRESENT_DOUBLE);
		} else if (type.equals(Float.class)) {
			return value.equals(CONV_PRESENT_FLOAT);
		} else if (type.equals(Character.class)) {
			return value.equals(CONV_PRESENT_CHAR);
		} else if (type.equals(Short.class)) {
			return value.equals(CONV_PRESENT_SHORT);
        } else if (type.equals(LocalDateTime.class)) {
            return value.equals(CONV_PRESENT_LOCAL_DATE_TIME);
        } else if (type.equals(LocalDate.class)) {
            return value.equals(CONV_PRESENT_LOCAL_DATE);
        } else if (type.equals(LocalTime.class)) {
            return value.equals(CONV_PRESENT_LOCAL_TIME);
        } else if (type.equals(BigDecimal.class)) {
			return ((BigDecimal) value).compareTo(CONV_PRESENT_BIG_DECIMAL) == 0;
		} else if (type.equals(Byte.class)){
			return value.equals(CONV_PRESENT_BYTE);
		} else if (value instanceof Enum) {
			return (value.toString().equals(CONV_PRESENT_ENUM));
		}
		return false;
	}

	public static final boolean isConventionedValueMissed(Object value)
	{
		Class<? extends Object> type = value.getClass();
		if (type.equals(String.class)) {
			return value.equals(CONV_MISSED_STRING)
			|| value.equals(Convention.CONV_MISSED_DATE_FIX_STRING)
                    || value.equals(Convention.CONV_MISSED_DATEONLY_FIX_STRING)
			|| value.equals(Convention.CONV_MISSED_TIMEONLY_FIX_STRING)
			|| value.equals(Convention.CONV_MISSED_CHAR_FIX_STRING);
		} else if (type.equals(Integer.class)) {
			return value.equals(CONV_MISSED_INTEGER);
		} else if (type.equals(Long.class)) {
			return value.equals(CONV_MISSED_LONG);
		} else if (type.equals(Double.class)) {
			return value.equals(CONV_MISSED_DOUBLE);
		} else if (type.equals(Character.class)) {
			return value.equals(CONV_MISSED_CHAR);
		} else if (type.equals(Short.class)) {
			return value.equals(CONV_MISSED_SHORT);
        } else if (type.equals(LocalDateTime.class)) {
            return value.equals(CONV_MISSED_LOCAL_DATE_TIME);
        } else if (type.equals(LocalDate.class)) {
            return value.equals(CONV_MISSED_LOCAL_DATE);
        } else if (type.equals(LocalTime.class)) {
            return value.equals(CONV_MISSED_LOCAL_TIME);
        } else if (type.equals(BigDecimal.class)) {
			return ((BigDecimal) value).compareTo(CONV_MISSED_BIG_DECIMAL) == 0;
		} else if (type.equals(Byte.class)){
			return value.equals(CONV_MISSED_BYTE);
		} else if (value instanceof Enum) {
			return (value.toString().equals(CONV_MISSED_ENUM));
		}
		return false;
	}

    /**
     * @deprecated This method for backward compatibility with aml 2. Please use {@link #isConventionedValueMissed(Object)}
     */
	@Deprecated
    public static final boolean isConventionedValueMissedOrNestedMissed(Object value) {
        if (value instanceof IMessage) {
            IMessage message = (IMessage) value;
            for (String fieldName : message.getFieldNames()) {
                if (!isConventionedValueMissedOrNestedMissed(message.getField(fieldName))) {
                    return false;
                }
            }
            return true;
        } else if (value instanceof List<?>) {
            for (Object nested : (List<?>) value) {
                if (!isConventionedValueMissedOrNestedMissed(nested)) {
                    return false;
                }
            }
            return true;
        }
        return isConventionedValueMissed(value);
    }
}
