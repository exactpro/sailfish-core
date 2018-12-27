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

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.common.impl.messages.IBaseEnumField;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;

@MatrixUtils
@ResourceAliases({"ConvertUtil"})
public class ConvertUtil extends AbstractCaller {
    @UtilityMethod
    @Description("Converts a Number value with zero precision to BigDecimal.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toBigDecimal(5.444)} returns 5.444")
    public BigDecimal toBigDecimal(Number value) {
        if(value == null) {
            return null;
        }

        return toBigDecimal(value.toString(), 0);
    }

    @UtilityMethod
    @Description("Converts a Number value with precision to BigDecimal.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "<b>precision</b> - a number of digits.<br/>"
            + "Example:<br/>"
            + "#{toBigDecimal(5.444, 3)} returns 5.44")
    public BigDecimal toBigDecimal(Number value, int presision) {
        if(value == null) {
            return null;
        }

        return toBigDecimal(value.toString(), presision);
    }

    @UtilityMethod
    @Description("Converts a String value to BigDecimal.<br/>"
            + "<b>value</b> - a string value for converting.<br/>"
            + "Example:<br/>"
            + "#{toBigDecimal(\"5.0\")} returns 5.0")
    public BigDecimal toBigDecimal(String value) {
        if(value == null) {
            return null;
        }

        return new BigDecimal(value);
    }

    @UtilityMethod
    @Description("Converts a String value with precision to BigDecimal.<br/>"
            + "<b>value</b> - a string value for converting.<br/>"
            + "<b>precision</b> - a number of digits.<br/>"
            + "Example:<br/>"
            + "#{toBigDecimal(\"5.444\", 3)} returns 5.44")
    public BigDecimal toBigDecimal(String value, int presision) {
        if(value == null) {
            return null;
        }

        MathContext mathContext = new MathContext(presision);
        return new BigDecimal(value, mathContext);
    }

    @UtilityMethod
    @Description("Converts a Number value to Double.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toDouble(5)} returns 5.0")
    public Double toDouble(Number value) {
        if(value == null) {
            return null;
        }

        return value.doubleValue();
    }

    @UtilityMethod
    @Description("Converts a String value to Double.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toDouble(\"5\")} returns 5.0")
    public Double toDouble(String value) {
        if(value == null) {
            return null;
        }

        return Double.valueOf(value);
    }

    @UtilityMethod
    @Description("Converts a Number value to Integer.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toInteger(5.0)} returns 5")
    public Integer toInteger(Number value) {
        if(value == null) {
            return null;
        }

        return value.intValue();
    }

    @UtilityMethod
    @Description("Converts a String value to Integer.<br/>"
            + "<b>value</b> - a string value for converting.<br/>"
            + "Example:<br/>"
            + "#{toInteger(\"5\")} returns 5")
    public Integer toInteger(String value) {
        if(value == null) {
            return null;
        }

        return Integer.valueOf(value);
    }

    @UtilityMethod
    @Description("Converts a Number value to Byte.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toByte(5)} returns 5")
    public Byte toByte(Number value) {
        if(value == null) {
            return null;
        }

        return value.byteValue();
    }

    @UtilityMethod
    @Description("Converts a String value to Byte.<br/>"
            + "<b>value</b> - a string value for converting.<br/>"
            + "Example:<br/>"
            + "#{toByte(\"5\")} returns 5")
    public Byte toByte(String value) {
        if(value == null) {
            return null;
        }

        return Byte.valueOf(value);
    }

    @UtilityMethod
    @Description("Converts a Number value to Short.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toShort(5)} returns 5")
    public Short toShort(Number value) {
        if(value == null) {
            return null;
        }

        return value.shortValue();
    }

    @UtilityMethod
    @Description("Converts a String value to Short.<br/>"
            + "<b>value</b> - a string value for converting.<br/>"
            + "Example:<br/>"
            + "#{toShort(\"5\")} returns 5")
    public Short toShort(String value) {
        if(value == null) {
            return null;
        }

        return Short.valueOf(value);
    }

    @UtilityMethod
    @Description("Converts a Number value to Float.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toFloat(5)} returns 5.0")
    public Float toFloat(Number value) {
        if(value == null) {
            return null;
        }

        return value.floatValue();
    }

    @UtilityMethod
    @Description("Converts a String value to Float.<br/>"
            + "<b>value</b> - a string value for converting.<br/>"
            + "Example:<br/>"
            + "#{toFloat(\"5\")} returns 5.0")
    public Float toFloat(String value) {
        if(value == null) {
            return null;
        }

        return Float.valueOf(value);
    }

    @UtilityMethod
    @Description("Converts a Number value to Long.<br/>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toLong(5)} returns 5")
    public Long toLong(Number value) {
        if(value == null) {
            return null;
        }

        return value.longValue();
    }

    @UtilityMethod
    @Description("Converts a String value to Long.<br/>"
            + "<b>value</b> - a string value for converting.<br/>"
            + "Example:<br/>"
            + "#{toLong(\"5\")} returns 5")
    public Long toLong(String value) {
        if(value == null) {
            return null;
        }

        return Long.valueOf(value);
    }

    @UtilityMethod
    @Description("Converts an Object value to String. Returns null if the value is null.<br/>"
            + "<b>value</b> - a string value for converting.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{toString(value)}")
    public String toString(Object value) {
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    @UtilityMethod
    @Description("Converts a Number value to String. Returns null if the value is null.<br/>"
            + "<b>Number Format Pattern Syntax</b>"
            + "<table>"
            + "<tr><td>    0   </td><td>   A digit - always displayed, even if the number has fewer digits (then 0 is displayed)     </td></tr>"
            + "<tr><td>    #   </td><td>   A digit, leading zeroes are omitted.    </td></tr>"
            + "<tr><td>    0   </td><td>   Marks decimal separator </td></tr>"
            + "<tr><td>    ,   </td><td>   Marks a grouping separator (e.g. a thousands separator)   </td></tr>"
            + "<tr><td>    E   </td><td>   Marks the separation of the mantissa and the exponent for exponential formats.   </td></tr>"
            + "<tr><td>    ;   </td><td>   Separates formats   </td></tr>"
            + "<tr><td>    -   </td><td>   Marks the negative number prefix    </td></tr>"
            + "<tr><td>    %   </td><td>   Multiplies by 100 and shows the number as percentage   </td></tr>"
            + "<tr><td>    ?   </td><td>   Multiplies by 1000 and shows the number as per mille    </td></tr>"
            + "<tr><td>    ¤   </td><td>   Currency sign - replaced by the currency sign for the Locale. Also makes formatting use the monetary decimal separator instead of the official decimal separator. ¤¤ makes formatting use international monetary symbols. </td></tr>"
            + "<tr><td>    X   </td><td>   Marks a character to be used in the number prefix or suffix </td></tr>"
            + "<tr><td>    '   </td><td>   Marks a quote around special characters in the prefix or the suffix of the formatted number.   </td></tr>"
            + "</table>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "<b>pattern</b> - a format of the resulting string.<br/>"
            + "Examples: <table border=1>"
            + "<tr><td>Pattern</td><td>Number</td><td>Formatted String</td></tr>"
            + "<tr><td>###.###</td><td>123.456</td><td>123.456</td></tr>"
            + "<tr><td>###.#</td><td>123.456</td><td>123.5</td></tr>"
            + "<tr><td>###,###.##</td><td>123456.789</td><td>123,456.79</td></tr>"
            + "<tr><td>000.###</td><td>9.95</td><td>009.95</td></tr>"
            + "<tr><td>##0.###</td><td>0.95</td><td>0.95</td></tr>"
            + "</table>")
    public String toString(Number value, String pattern) {
        return toString(value, pattern, null, null);
    }

    @UtilityMethod
    @Description("Converts a Number value to String. Returns null if the value is null.<br/>"
            + "<b>Number Format Pattern Syntax</b>"
            + "<table>"
            + "<tr><td>    0   </td><td>   A digit - always displayed, even if the number has fewer digits (then 0 is displayed)     </td></tr>"
            + "<tr><td>    #   </td><td>   A digit, leading zeroes are omitted.    </td></tr>"
            + "<tr><td>    0   </td><td>   Marks decimal separator </td></tr>"
            + "<tr><td>    ,   </td><td>   Marks a grouping separator (e.g. a thousands separator)   </td></tr>"
            + "<tr><td>    E   </td><td>   Marks the separation of the mantissa and the exponent for exponential formats.   </td></tr>"
            + "<tr><td>    ;   </td><td>   Separates formats   </td></tr>"
            + "<tr><td>    -   </td><td>   Marks the negative number prefix    </td></tr>"
            + "<tr><td>    %   </td><td>   Multiplies by 100 and shows the number as percentage   </td></tr>"
            + "<tr><td>    ?   </td><td>   Multiplies by 1000 and shows the number as per mille    </td></tr>"
            + "<tr><td>    ¤   </td><td>   Currency sign - replaced by the currency sign for the Locale. Also makes formatting use the monetary decimal separator instead of the official decimal separator. ¤¤ makes formatting use international monetary symbols. </td></tr>"
            + "<tr><td>    X   </td><td>   Marks a character to be used in the number prefix or suffix </td></tr>"
            + "<tr><td>    '   </td><td>   Marks a quote around special characters in the prefix or the suffix of the formatted number.   </td></tr>"
            + "</table>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "<b>pattern</b> - a format of the resulting string.<br/>"
            + "<b>decimalSeparator - a character used for the decimal sign.</b>"
            + "Examples:"
            + "<table border=1>"
            + "<tr><td>Pattern</td><td>Number</td><td>Formatted String</td></tr>"
            + "<tr><td>###.###</td><td>123.456</td><td>123.456</td></tr>"
            + "<tr><td>###.#</td><td>123.456</td><td>123.5</td></tr>"
            + "<tr><td>###,###.##</td><td>123456.789</td><td>123,456.79</td></tr>"
            + "<tr><td>000.###</td><td>9.95</td><td>009.95</td></tr>"
            + "<tr><td>##0.###</td><td>0.95</td><td>0.95</td></tr>"
            + "</table>")
    public String toString(Number value, String pattern, Character decimalSeparator) {
        return toString(value, pattern, decimalSeparator, null);
    }

    @UtilityMethod
    @Description("Converts a Number value to String. Returns null if the value is null.<br/>"
            + "<b>Number Format Pattern Syntax</b>"
            + "<table>"
            + "<tr><td>    0   </td><td>   A digit - always displayed, even if the number has fewer digits (then 0 is displayed)     </td></tr>"
            + "<tr><td>    #   </td><td>   A digit, leading zeroes are omitted.    </td></tr>"
            + "<tr><td>    0   </td><td>   Marks decimal separator </td></tr>"
            + "<tr><td>    ,   </td><td>   Marks a grouping separator (e.g. a thousands separator)   </td></tr>"
            + "<tr><td>    E   </td><td>   Marks the separation of the mantissa and the exponent for exponential formats.   </td></tr>"
            + "<tr><td>    ;   </td><td>   Separates formats   </td></tr>"
            + "<tr><td>    -   </td><td>   Marks the negative number prefix    </td></tr>"
            + "<tr><td>    %   </td><td>   Multiplies by 100 and shows the number as percentage   </td></tr>"
            + "<tr><td>    ?   </td><td>   Multiplies by 1000 and shows the number as per mille    </td></tr>"
            + "<tr><td>    ¤   </td><td>   Currency sign - replaced by the currency sign for the Locale. Also makes formatting use the monetary decimal separator instead of the official decimal separator. ¤¤ makes formatting use international monetary symbols. </td></tr>"
            + "<tr><td>    X   </td><td>   Marks a character to be used in the number prefix or suffix </td></tr>"
            + "<tr><td>    '   </td><td>   Marks a quote around special characters in the prefix or the suffix of the formatted number.   </td></tr>"
            + "</table>"
            + "<b>value</b> - a number value for converting.<br/>"
            + "<b>pattern</b> - a format of the resulting string.<br/>"
            + "<b>decimalSeparator - a character used for the decimal sign.</b>"
            + "<b>groupingSeparator - a character used for the thousands separator.</b><br/>"
            + "Examples:"
            + "<table border=1>"
            + "<tr><td>Pattern</td><td>Number</td><td>Formatted String</td></tr>"
            + "<tr><td>###.###</td><td>123.456</td><td>123.456</td></tr>"
            + "<tr><td>###.#</td><td>123.456</td><td>123.5</td></tr>"
            + "<tr><td>###,###.##</td><td>123456.789</td><td>123,456.79</td></tr>"
            + "<tr><td>000.###</td><td>9.95</td><td>009.95</td></tr>"
            + "<tr><td>##0.###</td><td>0.95</td><td>0.95</td></tr>"
            + "</table>")
    public String toString(Number value, String pattern, Character decimalSeparator, Character groupingSeparator) {
        if (value != null) {
            DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
            if (decimalSeparator != null) {
                formatSymbols.setDecimalSeparator(decimalSeparator);
            }
            if (groupingSeparator != null) {
                formatSymbols.setGroupingSeparator(groupingSeparator);
            }
            DecimalFormat format = new DecimalFormat(pattern, formatSymbols);
            return format.format(value);
        }
        return null;
    }

    @UtilityMethod
    @Description("Converts the incoming parameter of a <b>String (size 1)</b>, <b>Character</b>, <b>Byte</b>, <b>Short</b>, <b>Integer</b> value type<br/>"
            + "or <b>FIX enum</b>, <b>Base enum</b> (using for non-FIX message) based on the same types.<br/>"
            + "Throws IllegalArgumentException in runtime"
            + "<b>value</b> - a number value for converting.<br/>"
            + "Example:<br/>"
            + "#{toChar(\"A\")} returns 'A'")
    public Character toChar(Object value) {
        return toChar(value, Boolean.FALSE);
    }

    @UtilityMethod
    @Description("Converts the incoming parameter of a <b>String (size 1)</b>, <b>Character</b>, <b>Byte</b>, <b>Short</b>, <b>Integer</b> value type<br/>"
            + "or <b>FIX enum</b>, <b>Base enum</b> (using for non-FIX message) based on the same types.<br/>"
            + "<b>isDigit<b> - adds 48 / '0' code.<br/>"
            + "Example:<br/>"
            + "\"#{toChar(1, true)} returns '1'<br/>"
            + "Throws IllegalArgumentException in runtime.")
    public Character toChar(Object value, Boolean isDigit) {
        if (value == null) {
            return null;
        }

        value = extractValue(value);

        char charValue = '\u0000';

        if (value instanceof Character) {
            charValue = (Character)value;
        } else if (value instanceof String) {
            String stringValue = (String)value;
            if (stringValue.length() != 1) {
                throw new IllegalArgumentException("String length is not one, value [" + value + "]");
            }

            charValue = stringValue.charAt(0);
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            charValue = (char) ((Number)value).intValue();
        } else {
            throw new IllegalArgumentException("No action for value type [" + value.getClass().getCanonicalName() + "]");
        }

        if (Boolean.TRUE.equals(isDigit)) {
            return (char) (charValue + '0');
        }
        return charValue;
    }

    @UtilityMethod
    @Description("Converts a <b>String (size 1)</b>, <b>Character</b>, <b>Byte</b>, <b>Short</b>, <b>Integer</b> types with single char in the value<br/>"
            + "to code point at the given char.<br/>"
            + "<b>value</b> - a value for converting.<br/>"
            + "Example:<br/>"
            + "#{toCodePoint('A')} returns 65.<br/>"
            + "Throws IllegalArgumentException in runtime.")
    public Integer toCodePoint(Object value) {
        if(value == null) {
            return null;
        }

        Character charValue = toChar(value);
        return Character.codePointAt(new char[] { charValue }, 0);
    }

    @UtilityMethod
    @Description("Converts an array of objects to map.<br/>"
            + "<b>values</b> - a values for converting(must be even).<br/>"
            + "Example:<br/>"
            + "#{toMap(\"Text\", \"example!\")} returns {Test=example}")
    public Map<Object, Object> toMap(Object... values) {
        if(values == null) {
            return null;
        }

        if(values.length % 2 > 0) {
            throw new IllegalArgumentException("Amount of values must be even");
        }

        Map<Object, Object> map = new HashMap<>();

        for(int i = 0; i < values.length; i += 2) {
            map.put(values[i], values[i + 1]);
        }

        return map;
    }

    @UtilityMethod
    @Description("Converts an array of objects to list.<br/>"
            + "<b>values</b> - a values for converting(must be even).<br/>"
            + "Example:<br/>"
            + "#{toList(\"Text\", \"example!\")} returns [Test, example]")
    public List<Object> toList(Object... values) {
        if(values == null) {
            return null;
        }

        return Arrays.asList(values);
    }
    
    protected Object extractValue(Object value) {
        if (value instanceof IBaseEnumField) {
            value = ((IBaseEnumField)value).getObjectValue();
        }
        return value;
    }

    @UtilityMethod
    @Description("Creates regex to match the string representation of the provided object.<br/>"
            + "<b>value</b> - a value for converting.<br/>"
            + "Example:<br/>"
            + "#{toRegex(\"Text example!\")} returns \\QText example!\\E")
    public String toRegex(Object value) {
        String stringValue = toString(value);

        if(stringValue == null) {
            return null;
        }

        return Pattern.quote(stringValue);
    }
}
