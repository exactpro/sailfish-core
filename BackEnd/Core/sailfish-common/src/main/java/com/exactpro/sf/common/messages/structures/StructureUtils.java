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

import java.math.BigDecimal;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.util.EPSCommonException;

public class StructureUtils {

	public static Object castValueToJavaType(String value, JavaType javaType) throws EPSCommonException {
		
		if (value != null && javaType != null) {
			
	        switch (javaType) {
	        
	            case JAVA_LANG_BOOLEAN:
	            	
	                if ("Y".equalsIgnoreCase(value)) {
	                    return Boolean.TRUE;
	                } else if ("N".equalsIgnoreCase(value)) {
	                    return Boolean.FALSE;
	                }
	                
	                return Boolean.parseBoolean(value);
	                
	            case JAVA_LANG_SHORT:
	                return Short.parseShort(value);
	            case JAVA_LANG_INTEGER:
	                return Integer.parseInt(value);
	            case JAVA_LANG_LONG:
	                return Long.parseLong(value);
	            case JAVA_LANG_BYTE:
	                return Byte.parseByte(value);
	            case JAVA_LANG_FLOAT:
	                return Float.parseFloat(value);
	            case JAVA_LANG_DOUBLE:
	                return Double.parseDouble(value);
	            case JAVA_LANG_STRING:
	                return value;
	            case JAVA_LANG_CHARACTER:
	            	
	                if (value.length() == 1) {
	                    return value.charAt(0);
	                }
                throw new EPSCommonException("Could not parse the value '" + value + "' for " + javaType.name() + " type");
	                
	            case JAVA_MATH_BIG_DECIMAL:
	                return new BigDecimal(value);
	                
                case JAVA_TIME_LOCAL_DATE_TIME:
                case JAVA_TIME_LOCAL_DATE:
                case JAVA_TIME_LOCAL_TIME:

                default:
                    throw new EPSCommonException("Could not parse the value '" + value + "' for " + javaType.name() + " type");
            }
	    }
		
	    return null;
	}
}