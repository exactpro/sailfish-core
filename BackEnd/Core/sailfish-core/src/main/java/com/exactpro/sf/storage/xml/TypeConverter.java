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
package com.exactpro.sf.storage.xml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class TypeConverter {

	public static boolean getBoolean(String value){
		 return value.equals("true")?true:false;
	}

	public static String getString(boolean value){
		return Boolean.toString(value);
	}

	public static Byte getByte(String value){
		return new Byte(value);
	}

	public static String getString(Byte value){
		return Byte.toString(value);
	}

	public static Character getCharacter(String value){
		return value.charAt(0);
	}

	public static String getString(char value){
		return ""+value;
	}

	public static Date getDate(String value){
		DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
		try {
			return formater.parse(value);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getString(Date value){
		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
		return formatter.format(value);
	}

    public static LocalDateTime getLocalDateTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG);
        return LocalDateTime.parse(value, formatter);
    }

    public static LocalDate getLocalDate(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; //'2011-12-03'
        return LocalDate.parse(value, formatter);
    }

    public static LocalTime getLocalTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;
        return LocalTime.parse(value, formatter);
    }

    public static String getString(LocalDateTime value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG);
        return value.format(formatter);
    }

    public static String getString(LocalDate value) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        return value.format(formatter);
    }
    public static String getString(LocalTime value) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;
        return value.format(formatter);
    }

	public static Double getDouble(String value){
		return new Double(value);
	}

	public static String getString(Double value){
		return Double.toString(value);
	}

	public static Float getFloat(String value){
		return new Float(value);
	}

	public static String getString(Float value){
		return Float.toString(value);
	}

	public static Integer getInteger(String value){
		return new Integer(value);
	}

	public static String getString(Integer value){
		return Integer.toString(value);
	}

	public static Long getLong(String value){
		return new Long(value);
	}

	public static String getString(Long value){
		return Long.toString(value);
	}

	public static Short getShort(String value){
		return new Short(value);
	}

	public static String getString(Short value){
		return Short.toString(value);
	}

	public static BigDecimal getBigDecimal(String value){
		return new BigDecimal(value);
	}

	public static String getString(BigDecimal value){
		return value.toPlainString();
	}

}
