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
package com.exactpro.sf.common.messages.structures;

import static com.exactpro.sf.comparison.conversion.MultiConverter.convert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;

/**
 * Java interface for attributes
 */
public interface IAttributeStructure {

	/**
	 * Get the attributes name
	 * @return {@link String}
	 */
	String getName();

	/**
	 * Get the attributes value
	 * @return {@link String}
	 */
	String getValue();

	/**
	 * Get the attributes casted value
 	 * @param <T> value casted to <b>type</b>
	 * @return {@link T}
	 */
	<T> T getCastValue();

	/**
	 * Get the attributes type
	 * @return {@link JavaType}
	 */
	JavaType getType();
	
    /**
     * Returns attribute value as the specified type (converting it if required)
     */
    default <T> T getAs(Class<T> type) {
        return convert(getCastValue(), type);
    }

    /**
     * Returns attribute value as byte (converting it if required)
     */
    default byte asByte() {
        return convert(getCastValue(), byte.class);
    }

    /**
     * Returns attribute value as short (converting it if required)
     */
    default short asShort() {
        return convert(getCastValue(), short.class);
    }

    /**
     * Returns attribute value as integer (converting it if required)
     */
    default int asInteger() {
        return convert(getCastValue(), int.class);
    }

    /**
     * Returns attribute value as long (converting it if required)
     */
    default long asLong() {
        return convert(getCastValue(), long.class);
    }

    /**
     * Returns attribute value as float (converting it if required)
     */
    default float asFloat() {
        return convert(getCastValue(), float.class);
    }

    /**
     * Returns attribute value as double (converting it if required)
     */
    default double asDouble() {
        return convert(getCastValue(), double.class);
    }

    /**
     * Returns attribute value as boolean (converting it if required)
     */
    default boolean asBoolean() {
        return convert(getCastValue(), boolean.class);
    }

    /**
     * Returns attribute value as character (converting it if required)
     */
    default char asCharacter() {
        return convert(getCastValue(), char.class);
    }

    /**
     * Returns attribute value as BigDecimal (converting it if required)
     */
    default BigDecimal asBigDecimal() {
        return convert(getCastValue(), BigDecimal.class);
    }

    /**
     * Returns attribute value as LocalDate (converting it if required)
     */
    default LocalDate asDate() {
        return convert(getCastValue(), LocalDate.class);
    }

    /**
     * Returns attribute value as LocalTime (converting it if required)
     */
    default LocalTime asTime() {
        return convert(getCastValue(), LocalTime.class);
    }

    /**
     * Returns attribute value as LocalDateTime (converting it if required)
     */
    default LocalDateTime asDateTime() {
        return convert(getCastValue(), LocalDateTime.class);
    }

    /**
     * Returns attribute value as string (converting it if required)
     */
    default String asString() {
        return convert(getCastValue(), String.class);
    }
}