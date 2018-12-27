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
import java.text.DecimalFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.util.LocalDateTimeConst;

/**
 *
 * @author dmitry.guriev
 *
 */
public class Formatter {

    public static String XML_10_PATTERN = "[^" + "\u0009\r\n" + "\u0020-\uD7FF"
            + "\uE000-\uFFFD" + "\ud800\udc00-\udbff\udfff" + "]";

	private Formatter() {
		// hide constructor
	}

    private final static ThreadLocal<DecimalFormat> df = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("#.####################");
        }
    };

    public static String format(Object o, boolean isExpected)
	{
		if (o == null) {
			return "null";
		}
		if (isExpected)
		{
			if (o instanceof String) {
				// DG: dirty hack for FIX protocol for DateTime fields comparison
				if (o.equals(Convention.CONV_PRESENT_DATE_FIX_STRING)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_DATE_FIX_STRING)) return Convention.CONV_MISSED_STRING;
				if (o.equals(Convention.CONV_PRESENT_CHAR_FIX_STRING)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_CHAR_FIX_STRING)) return Convention.CONV_MISSED_STRING;
				String s = (String)o;
				if (s.startsWith(" ")
						|| s.endsWith(" ")
						|| s.startsWith("\t")
						|| s.endsWith("\t")) {
					return "["+s+"]";
				}
				return s;
			}
			if (o instanceof Integer) {
				if (o.equals(Convention.CONV_PRESENT_INTEGER)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_INTEGER)) return Convention.CONV_MISSED_STRING;
				return o.toString();
			}
			if (o instanceof Long) {
				if (o.equals(Convention.CONV_PRESENT_LONG)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_LONG)) return Convention.CONV_MISSED_STRING;
				return o.toString();
			}
			if (o instanceof Double) {
				if (o.equals(Convention.CONV_PRESENT_DOUBLE)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_DOUBLE)) return Convention.CONV_MISSED_STRING;
				Double d = (Double)o;
				return (d.equals(Double.NaN)) ? "NaN" : df.get().format(d);
			}
			if (o instanceof Float) {
				if (o.equals(Convention.CONV_PRESENT_FLOAT)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_FLOAT)) return Convention.CONV_MISSED_STRING;
				Float d = (Float)o;
				return (d.equals(Float.NaN)) ? "NaN" : df.get().format(d);
			}
            if (o instanceof LocalDateTime) {
                if (o.equals(Convention.CONV_PRESENT_LOCAL_DATE_TIME)) return Convention.CONV_PRESENT_STRING;
                if (o.equals(Convention.CONV_MISSED_LOCAL_DATE_TIME)) return Convention.CONV_MISSED_STRING;
                return o.toString();
            }
            if (o instanceof LocalDate) {
                if (o.equals(Convention.CONV_PRESENT_LOCAL_DATE)) return Convention.CONV_PRESENT_STRING;
                if (o.equals(Convention.CONV_MISSED_LOCAL_DATE)) return Convention.CONV_MISSED_STRING;
                return o.toString();
            }
            if (o instanceof LocalTime) {
                if (o.equals(Convention.CONV_PRESENT_LOCAL_TIME)) return Convention.CONV_PRESENT_STRING;
                if (o.equals(Convention.CONV_MISSED_LOCAL_TIME)) return Convention.CONV_MISSED_STRING;
                return o.toString();
            }
			if (o instanceof Character) {
				if (o.equals(Convention.CONV_PRESENT_CHAR)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_CHAR)) return Convention.CONV_MISSED_STRING;
				return o.toString();
			}
		}
		if (o instanceof Double) {
			Double d = (Double)o;
			return (d.equals(Double.NaN)) ? "NaN" : df.get().format(d);
		}
		if (o instanceof Float) {
			Float f = (Float)o;
			return (f.equals(Float.NaN)) ? "NaN" : df.get().format(f);
		}
		if (o instanceof BigDecimal) {
			BigDecimal bd = (BigDecimal)o;
			if (bd.compareTo(Convention.CONV_PRESENT_BIG_DECIMAL) == 0) {
				return Convention.CONV_PRESENT_STRING;
			}
			if (bd.compareTo(Convention.CONV_MISSED_BIG_DECIMAL) == 0) {
				return Convention.CONV_MISSED_STRING;
			}

			return bd.toPlainString();
		}
		if (o instanceof String) {
			String s = (String)o;
			if (s.startsWith(" ")
					|| s.endsWith(" ")
					|| s.startsWith("\t")
					|| s.endsWith("\t")) {
				return "["+s+"]";
			}
		}
		return o.toString();
	}

	public static String formatForHtml(Object o, boolean isExpected) {
		if (o == null) {
			return "null";
		}
		if (isExpected)
		{
			if (o instanceof IFilter) {
				IFilter fltr = (IFilter) o;
				return fltr.getCondition();
			}
			if (o instanceof String) {
				// DG: dirty hack for FIX protocol for DateTime fields comparison
				if (o.equals(Convention.CONV_PRESENT_DATE_FIX_STRING)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_DATE_FIX_STRING)) return Convention.CONV_MISSED_STRING;
				if (o.equals(Convention.CONV_PRESENT_CHAR_FIX_STRING)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_CHAR_FIX_STRING)) return Convention.CONV_MISSED_STRING;
				String s = (String)o;
				if (s.startsWith(" ")
						|| s.endsWith(" ")
						|| s.startsWith("\t")
						|| s.endsWith("\t")) {
					s = "["+s+"]";
				}
				if (s.indexOf(Convention.CONV_MISSED_CHAR_FIX_STRING) != -1) {
					s = s.replace(Convention.CONV_MISSED_CHAR_FIX_STRING, Convention.CONV_MISSED_STRING);
				}
				if (s.indexOf(Convention.CONV_MISSED_CHAR) != -1) {
					s = s.replace(Convention.CONV_MISSED_CHAR, Convention.CONV_MISSED_STRING.charAt(0));
				}
				return s.replaceAll(XML_10_PATTERN, Convention.CONV_MISSED_STRING);
			}
			if (o instanceof Integer) {
				if (o.equals(Convention.CONV_PRESENT_INTEGER)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_INTEGER)) return Convention.CONV_MISSED_STRING;
				return o.toString();
			}
			if (o instanceof Long) {
				if (o.equals(Convention.CONV_PRESENT_LONG)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_LONG)) return Convention.CONV_MISSED_STRING;
				return o.toString();
			}
			if (o instanceof Double) {
				if (o.equals(Convention.CONV_PRESENT_DOUBLE)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_DOUBLE)) return Convention.CONV_MISSED_STRING;
				return df.get().format(o);
			}
			if (o instanceof Float) {
				if (o.equals(Convention.CONV_PRESENT_FLOAT)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_FLOAT)) return Convention.CONV_MISSED_STRING;
				return df.get().format(o);
			}
			if (o instanceof Short) {
				if (o.equals(Convention.CONV_PRESENT_SHORT)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_SHORT)) return Convention.CONV_MISSED_STRING;
				return o.toString();
			}
			if (o instanceof LocalDateTime) {
				if (o.equals(Convention.CONV_PRESENT_LOCAL_DATE_TIME)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_LOCAL_DATE_TIME)) return Convention.CONV_MISSED_STRING;
                return ((LocalDateTime) o).format(LocalDateTimeConst.DATE_TIME_FORMATTER);
			}
            if (o instanceof LocalDate) {
                if (o.equals(Convention.CONV_PRESENT_LOCAL_DATE)) return Convention.CONV_PRESENT_STRING;
                if (o.equals(Convention.CONV_MISSED_LOCAL_DATE)) return Convention.CONV_MISSED_STRING;
                return ((LocalDate) o).format(LocalDateTimeConst.DATE_FORMATTER);
            }
            if (o instanceof LocalTime) {
                if (o.equals(Convention.CONV_PRESENT_LOCAL_TIME)) return Convention.CONV_PRESENT_STRING;
                if (o.equals(Convention.CONV_MISSED_LOCAL_TIME)) return Convention.CONV_MISSED_STRING;
                return ((LocalTime) o).format(LocalDateTimeConst.TIME_FORMATTER);
            }
			if (o instanceof Character) {
				if (o.equals(Convention.CONV_PRESENT_CHAR)) return Convention.CONV_PRESENT_STRING;
				if (o.equals(Convention.CONV_MISSED_CHAR)) return Convention.CONV_MISSED_STRING;
				return o.toString();
			}
			if (o instanceof BigDecimal) {
				BigDecimal bd = (BigDecimal)o;
				if (bd.compareTo(Convention.CONV_PRESENT_BIG_DECIMAL) == 0) {
					return Convention.CONV_PRESENT_STRING;
				}
				if (bd.compareTo(Convention.CONV_MISSED_BIG_DECIMAL) == 0) {
					return Convention.CONV_MISSED_STRING;
				}
			}
		}

		if (o instanceof IFilter) {
			IFilter fltr = (IFilter) o;
			return fltr.getCondition();
		}

		if (o instanceof Double) {
			return df.get().format(o);
		}
		if (o instanceof Float) {
			return df.get().format(o);
		}

		String s;
		if(o instanceof BigDecimal) {
			s = ((BigDecimal) o).toPlainString();
		} else
			s = o.toString();

		if (s.startsWith(" ")
				|| s.endsWith(" ")
				|| s.startsWith("\t")
				|| s.endsWith("\t")) {
			return "["+s+"]";
		}

		return s;
	}

    public static String formatExpected(ComparisonResult result) {
        Object expected = result.getExpected();

        if(expected instanceof IFilter) {
            return ((IFilter)expected).getCondition(result.getActual());
        }

        return formatForHtml(expected, true);
    }
}
