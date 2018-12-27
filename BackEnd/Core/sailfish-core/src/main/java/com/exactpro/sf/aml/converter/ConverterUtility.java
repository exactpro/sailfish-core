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
package com.exactpro.sf.aml.converter;

import java.util.Objects;

public class ConverterUtility {

    /**
     * Create message in format 'line:messageName:column message' 
     */
    public static String formatError(Long line, String messageName, String column, String messageFormat, Object ... args) {
        Objects.requireNonNull(messageFormat, "Message require non null");
        
        StringBuilder builder = new StringBuilder();
        if (line != null) {
            builder.append(line).append(':');
        }
        if (messageName != null) {
            builder.append(messageName).append(':');
        }
        if (column != null) {
            builder.append(column).append(' ');
        }
        builder.append(String.format(messageFormat, args));
        return builder.toString();
    }
    
    /**
     * Create message in format 'messageName:column message' 
     */
    public static String formatError(String messageName, String column, String messageFormat, Object ... args) {
        return formatError(null, Objects.requireNonNull(messageName, "Message name require non null"), Objects.requireNonNull(column, "Column require non null"), messageFormat, args);
    }
    
    /**
     * Create message in format 'column message' 
     */
    public static String formatError(String column, String messageFormat, Object ... args) {
        return formatError(null, null, Objects.requireNonNull(column, "Column require non null"), messageFormat, args);
    }
    
    /**
     * Create message in format 'line:message' 
     */
    public static String formatError(long line, String format, Object ... args) {
        return formatError(line, null, null, format, args);
    }
}
