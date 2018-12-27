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
package com.exactpro.sf.common.util;

import org.apache.commons.lang3.ArrayUtils;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IFieldStructure;

/**
 * Contains useful routines which are called from generating code templates 
 *
 */
public class CodeGenUtils 
{
	public static final String COMPONENTS_SUB_PACKAGE = "components";
	
	public static String getPackage(IFieldStructure fieldStructure, String[] packageName, boolean underscoreAsPackageSeparator) {
	    String[] classPath = getClassPath(fieldStructure, packageName, true, underscoreAsPackageSeparator);
	    classPath = ArrayUtils.remove(classPath, classPath.length - 1);
	    return String.join(".", classPath);
    }
	
	public static String getShortClass(IFieldStructure fieldStructure, boolean underscoreAsPackageSeparator) {
        String className = fieldStructure.getName();
        if (underscoreAsPackageSeparator) {
            String[] nodes = className.split("_");
            className = nodes[nodes.length - 1];
        }
        return className;
    }
	
    public static String getTypeName(IFieldStructure fieldStructure, String[] packageName, boolean underscoreAsPackageSeparator) {
        if (fieldStructure.isComplex() || fieldStructure.isEnum()) {
            String[] classPath = getClassPath(fieldStructure, packageName, false, underscoreAsPackageSeparator);

            return String.join(".", classPath);
        }

        if (!fieldStructure.isCollection()) {
            return convertSimpleFieldTypeToJavaType(fieldStructure.getJavaType());
        } else {
            return convertSimpleFieldTypeToJavaObjectType(fieldStructure.getJavaType());
        }
    }
	
	public static String getEnumValueTypeName(IFieldStructure fieldStructure) {
		if (fieldStructure.isEnum()) {
			JavaType type = fieldStructure.getJavaType();
			return type.value();
		}
		return "dirty_surrogate";
	}
	
	public static String convertSimpleFieldTypeToJavaType(JavaType fldType) {
		
		if (fldType == null) return null;
		
		switch ( fldType )
		{
		case JAVA_LANG_BOOLEAN: 
			return "boolean";
		case JAVA_LANG_BYTE: 
			return "byte";
		case JAVA_LANG_CHARACTER:
			return "char";
        case JAVA_TIME_LOCAL_DATE_TIME:
			return "LocalDateTime";
        case JAVA_TIME_LOCAL_DATE:
            return "LocalDate";
        case JAVA_TIME_LOCAL_TIME:
            return "LocalTime";
		case JAVA_MATH_BIG_DECIMAL:
			return "BigDecimal";
		case JAVA_LANG_DOUBLE:
			return "double";
		case JAVA_LANG_FLOAT:
			return "float";
		case JAVA_LANG_INTEGER:
			return "int";
		case JAVA_LANG_LONG:
			return "long";
		case JAVA_LANG_SHORT:
			return "short";
		case JAVA_LANG_STRING:
			return "String";
		default:
			break;
		}
		
		return null;
	}
	
	
	public static String convertSimpleFieldTypeToJavaObjectType(JavaType fldType) {
		
		if (fldType == null) return null;
		
		switch ( fldType )
		{
		case JAVA_LANG_BOOLEAN: 
			return "Boolean";
		case JAVA_LANG_BYTE: 
			return "Byte";
		case JAVA_LANG_CHARACTER:
			return "Character";
		case JAVA_TIME_LOCAL_DATE_TIME:
			return "LocalDateTime";
        case JAVA_TIME_LOCAL_DATE:
            return "LocalDate";
        case JAVA_TIME_LOCAL_TIME:
            return "LocalTime";
		case JAVA_MATH_BIG_DECIMAL:
			return "BigDecimal";
		case JAVA_LANG_DOUBLE:
			return "Double";
		case JAVA_LANG_FLOAT:
			return "Float";
		case JAVA_LANG_INTEGER:
			return "Integer";
		case JAVA_LANG_LONG:
			return "Long";
		case JAVA_LANG_SHORT:
			return "Short";
		case JAVA_LANG_STRING:
			return "String";
		default:
			break;
		}
		
		return null;
	}
	
	
	public static boolean isPrimitive(JavaType fldType) {
		
		if (fldType == null) return false;
		
		switch ( fldType )
		{
		case JAVA_LANG_BOOLEAN: 
		case JAVA_LANG_BYTE: 
		case JAVA_LANG_CHARACTER:
		case JAVA_LANG_DOUBLE:
		case JAVA_LANG_FLOAT:
		case JAVA_LANG_INTEGER:
		case JAVA_LANG_LONG:
		case JAVA_LANG_SHORT:
			return true;
        case JAVA_TIME_LOCAL_DATE_TIME:
        case JAVA_TIME_LOCAL_DATE:
        case JAVA_TIME_LOCAL_TIME:
        case JAVA_MATH_BIG_DECIMAL:
		case JAVA_LANG_STRING:
			return false;
		default:
			break;
		}
		
		return false;
	}
	
    private static String[] getClassPath(IFieldStructure fieldStructure, String[] packageName, boolean useFieldName, boolean underscoreAsPackageSeparator) {
        String[] result = packageName;
        result = ArrayUtils.add(result, fieldStructure.getNamespace().toLowerCase());
        if (!fieldStructure.isComplex()) {
            result = ArrayUtils.add(result, COMPONENTS_SUB_PACKAGE);
        }
        String fieldName = useFieldName ? fieldStructure.getName() : fieldStructure.getReferenceName();

        if (underscoreAsPackageSeparator) {
            String[] nodes = fieldName.split("_");
            result = ArrayUtils.addAll(result, nodes);
        } else {
            result = ArrayUtils.add(result, fieldName);
        }

        return result;
    }
}
