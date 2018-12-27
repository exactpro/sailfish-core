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

import static com.exactpro.sf.util.FormatHelper.buildFormatString;
import static com.exactpro.sf.util.FormatHelper.formatDouble;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;

@MatrixUtils
@ResourceAliases({"MathUtil"})
public class MathUtil extends AbstractCaller {

    private static final String ROUNDING_MODES = "Rounding Modes:<br/>" +
        "<b>UP</b> - Rounding mode to round away from zero.<br/>" +
            "Always increments the digit prior to a nonzero discarded fraction.<br/>" +
        "<b>DOWN</b> - Rounding mode to round towards zero.<br/>" +
            "Never increments the digit prior to a discarded fraction (i.e., truncates).<br/>" +
        "<b>HALF_UP</b> - Rounding mode to round towards \"nearest neighbor\" unless both neighbors are equidistant,<br/>" +
            " in which case round up. Behaves as for UP if the discarded fraction is &ge; 0.5; otherwise, behaves as for DOWN.<br/>" +
        "<b>HALF_DOWN</b> - Rounding mode to round towards \"nearest neighbor\" unless both neighbors are equidistant,<br/>" +
            " in which case round down. Behaves as for UP if the discarded fraction is &gt; 0.5; otherwise, behaves as for DOWN.<br/>" +
        "<b>CEILING</b> - Rounding mode to round towards positive infinity.<br/>" +
            "If the value is positive, behaves as for UP; if negative, behaves as for DOWN.<br/>" +
        "<b>FLOOR</b> - Rounding mode to round towards negative infinity.<br/>" +
            "If the value is positive, behave as for DOWN; if negative, behave as for UP.<br/>";

    public MathUtil() {}

	@Description("Returns the smallest (closest to negative infinity) double value that is greater than or equal to the argument and is equal to a mathematical integer.<br/>"
            + "Special cases:<br/>"
			+ "If the argument value is already equal to a mathematical integer, then the result is the same as the argument.<br/>"
			+ "If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the argument.<br/>"
			+ "If the argument value is less than zero but greater than -1.0, then the result is negative zero.<br/>"
			+ "Note that the value of Math.ceil(x) is exactly the value of -Math.floor(-x).<br/>"
            + "<b>d</b> - a double value for ceiling.<br/>"
            + "Example:<br/>"
            + "#{ceil(-5.1)} returns -5.0<br/>"
            + "#{ceil(5.1)} returns 6.0")
	@UtilityMethod
	public double ceil(double d) {
		return Math.ceil(d);
	}

    @Description("Returns the smallest (closest to negative infinity) double value that is greater than or equal to the argument and is equal to a mathematical integer.<br/>"
            + "If the argument is not negative, the argument is returned.<br/>"
            + "If the argument is negative, the negation of the argument is returned.<br/>"
            + "Special cases:<br/>"
            + "If the argument is positive zero or negative zero, the result is positive zero.<br/>"
            + "If the argument is infinite, the result is positive infinity.<br/>"
            + "If the argument is NaN, the result is NaN.<br/>"
            + "<b>d</b> - a double value for obtaining the absolute value.<br/>"
            + "Example:<br/>"
            + "#{abs(-5.0)} returns 5.0")
    @UtilityMethod
    public double abs(double d) {
        return Math.abs(d);
	}

    @Description("Returns the absolute value of a float value.<br/>"
            + "If the argument is not negative, the argument is returned.<br/>"
            + "If the argument is negative, the negation of the argument is returned.<br/>"
            + "Special cases:<br/>"
            + "If the argument is positive zero or negative zero, the result is positive zero.<br/>"
            + "If the argument is infinite, the result is positive infinity.<br/>"
            + "If the argument is NaN, the result is NaN.<br/>"
            + "<b>f</b> - a float value for obtaining the absolute value.<br/>"
            + "Example:<br/>"
            + "#{abs(-5.0f)} returns 5.0")
    @UtilityMethod
    public float abs(float f) {
        return Math.abs(f);
    }

    @Description("Returns the absolute value of a long value.<br/> "
            + "If the argument is not negative, the argument is returned.<br/> "
            + "If the argument is negative, the negation of the argument is returned.<br/>"
            + "<b>i</b> - an integer value for obtaining the absolute value.<br/>"
            + "Example:<br/>"
            + "#{abs(-5)} returns 5")
    @UtilityMethod
    public int abs(int i) {
        return Math.abs(i);
    }

    @Description("Returns the absolute value of a int value.<br/> "
            + "If the argument is not negative, the argument is returned.<br/> "
            + "If the argument is negative, the negation of the argument is returned.<br/>"
            + "<b>l</b> - a long value for obtaining the absolute value.<br/>"
            + "Example:<br/>"
            + "#{abs(-555555555l)} returns 555555555")
    @UtilityMethod
    public long abs(long l) {
        return Math.abs(l);
    }

    @Description("Returns the absolute value of a BigDecimal value.<br/>"
            + "If the argument is not negative, the argument is returned.<br/>"
            + "If the argument is negative, the negation of the argument is returned.<br/>"
            + "<b>b</b> - a BigDecimal value for obtaining the absolute value.<br/>"
            + "Example:<br/>"
            + "#{abs(-5.0B)} returns 5.0")
    @UtilityMethod
    public BigDecimal abs(BigDecimal b) {
        return b.abs();
    }


	@Description("Returns the largest (closest to positive infinity) double value that is less than or equal to the argument and is equal to a mathematical integer.<br/>"
            + "Special cases:<br/>"
			+ "If the argument value is already equal to a mathematical integer, then the result is the same as the argument.<br/>"
			+ "If the argument is NaN or infinity or positive zero or negative zero, then the result is the same as the argument.<br/>"
            + "<b>d</b> - a double value for flooring.<br/>"
            + "Example:<br/>"
            + "#{floor(-5.1)} returns -6.0<br/>"
            + "#{floor(5.1)} returns 5.0")
	@UtilityMethod
	public double floor(double d) {
		return Math.floor(d);
	}

	@Description("Returns the floating-point value adjacent to d in the direction of positive infinity.<br/>"
            + "This method is semantically equivalent to nextAfter(d, Double.POSITIVE_INFINITY); however, a nextUp implementation may run faster than its equivalent nextAfter call.<br/>"
			+ "Special Cases:<br/>"
			+ "If the argument is NaN, the result is NaN.<br/>"
			+ "If the argument is positive infinity, the result is positive infinity.<br/>"
			+ "If the argument is zero, the result is Double.MIN_VALUE<br/>"
            + "<b>d</b> - a double value for increase.<br/>"
            + "Example:<br/>"
            + "#{nextUp(5.0)} returns 5.000000000000001")
	@UtilityMethod
	public double nextUp(double d) {
		return Math.nextUp(d);
	}

    @Description("Returns the minimum of the values <br/>" +
            "<b>numbers</b> - values for comparison. Must be of the same type<br/>" +
            "Example:<br/>" +
            "#{min(5, 4, 3)} returns 3")
    @UtilityMethod
    public Object min(Object ... numbers){

        try {
            Arrays.sort(numbers);
        } catch (ClassCastException e) {
            StringBuilder builder = new StringBuilder("Input params have different types:");
            for (Object o:numbers) {
                builder.append(o.getClass().getSimpleName());
                builder.append(", ");
            }
            throw new EPSCommonException(builder.toString());
        }

        return numbers[0];
    }

    @Description("Returns the minimum of the int values<br/>" +
            "<b>numbers</b> - integer values for comparison<br/>" +
            "Example:<br/>" +
            "#{minInt(5, 4, 3)} returns 3")
    @UtilityMethod
    public int minInt(int ... numbers){
        Arrays.sort(numbers);
        return numbers[0];
    }

    @Description("Returns the minimum of the long values<br/>" +
            "<b>numbers</b> - long integer values for comparison<br/>" +
            "Example:<br/>" +
            "#{minLong(55555555555l, 44444444444l, 33333333333l)} returns 33333333333")
    @UtilityMethod
    public long minLong(long ... numbers){
        Arrays.sort(numbers);
        return numbers[0];
    }

    @Description("Returns the minimum of the char values <br/>" +
            "<b>numbers</b> - char values for comparison<br/>" +
            "Example:<br/>" +
            "#{minChar('1', '0', '2')} returns '0'")
    @UtilityMethod
    public char minChar(char ... numbers){
        Arrays.sort(numbers);
        return numbers[0];
    }

    @Description("Returns the minimum of the double values<br/>" +
            "<b>numbers</b> - decimal values for comparison<br/>" +
            "Example:<br/>" +
            "#{minDouble(5.0, 4.0, 3.0)} returns 3.0")
    @UtilityMethod
    public double minDouble(double ... numbers){
        Arrays.sort(numbers);
        return numbers[0];
    }

    @Description("Returns the minimum of the BigDecimal values <br/>" +
            "<b>numbers</b> - BigDecimal values for comparison<br/>" +
            "Example:<br/>" +
            "#{minBigDecimal(5.0B, 4.0B, 3.0B)} returns 3.0")
    @UtilityMethod
    public BigDecimal minBigDecimal(BigDecimal... numbers){
        Arrays.sort(numbers);
        return numbers[0];
    }

    @Description("Rounds <b>d</b> with specified <b>precision</b><br/>"
            + "<b>d</b> - double value for rounding<br/>"
            + "<b>precision</b> - the number of digits after the decimal separator<br/>"
            + "Example:<br/>"
            + "#{round(5.4564638, 4)} returns 5.4565")
    @UtilityMethod
    public double round(double d, int precision) {
        String formatString = buildFormatString(precision);
        return formatDouble(formatString, d);
    }

    @Description("Rounds <b>d</b> with specified <b>precision</b><br/>"
            + "<b>d</b> - BigDecimal value for rounding<br/>"
            + "<b>precision</b> - the number of digits after the decimal separator<br/>"
            + "Example:<br/>"
            + "#{round(5.4564638B, 4)} returns 5.4565")
    @UtilityMethod
    public BigDecimal round(BigDecimal d, int precision) {
        return d.setScale(precision, RoundingMode.HALF_UP);
    }

    @Description("Rounds <b>d</b> with specified <b>precision</b><br/>"
            + "<b>d</b> - double value for rounding<br/>"
            + "<b>precision</b> - the number of digits after the decimal separator<br/>"
            + ROUNDING_MODES
            + "Example:<br/>"
            + "#{round(5.4564638, 4, \"HALF_UP\")} returns 5.4565")
    @UtilityMethod
    public Double round(Double d, int precision, String roundingMode) {
        BigDecimal bd = BigDecimal.valueOf(d);
        bd = bd.setScale(precision, RoundingMode.valueOf(roundingMode.toUpperCase()));
        return bd.doubleValue();
    }

    @Description("Rounds <b>d</b> with specified <b>precision</b><br/>"
            + "<b>d</b> - BigDecimal value for rounding<br/>"
            + "<b>precision</b> - the number of digits after the decimal separator<br/>"
            + ROUNDING_MODES
            + "Example:<br/>"
            + "#{round(5.4564638, 4, \"HALF_UP\")} returns 5.4565")
    @UtilityMethod
    public BigDecimal round(BigDecimal d, int precision, String roundingMode) {
        return d.setScale(precision, RoundingMode.valueOf(roundingMode.toUpperCase()));
    }
    
    @Description("Returns a <b>rounded</b> by <b>precision</b> and <b>direction</b> number<br/>" +
            "<b>value</b> - rounding value.<br/>" +
            "<b>direction = 0</b> - the value will be rounded down.<br/>" +
            "<b>direction = 1</b> - the value will be rounded up.<br/>" +
            "<b>direction = 2</b> - arithmetic rounding<br/>" +
            "<b>precision</b> - symbols of the value double are set to zero, starting from the position precision. Must be positive.<br/>" +
            "Example:<br/>" +
            "#{round_zero(12345,1,0)} returns 12340<br/>" +
            "#{round_zero(12345,1,1)} returns 12350<br/>" +
            "#{round_zero(12345,1,2)} returns 12350")
    @UtilityMethod
    public double roundZero(double value, int precision, int direction){

        switch (direction){
        case 0:return roundDown((long)value, (int)Math.pow(10, precision));
        case 1:return roundUp((long)value, (int)Math.pow(10, precision));
        case 2:return round((long)value, (int)Math.pow(10, precision));
        default:return round((long)value, (int)Math.pow(10, precision));
        }
    }

    @Description("Rounds the double to an integer value using round away from zero mode<br/>"
            + "<b>value</b> - rounding value.<br/>"
            + "Example:<br/>"
            + "#{roundUp(-1.001)} returns -2")
    @UtilityMethod
    public int roundUp(double value) {
        return round(value, 0, RoundingMode.UP.toString()).intValue();
    }

    private static long round(long num, int multiplier) {
        return ((int) (((num / (double) multiplier) - num / multiplier) * 10) >= 5 ? roundUp(num, multiplier)
                : roundDown(num, multiplier));
    }

    private static long roundUp(long num, int multiplier) {
        return ((num / multiplier) * multiplier + multiplier);
    }

    private static long roundDown(long num, int multiplier) {
        return ((num / multiplier) * multiplier);
    }
}
