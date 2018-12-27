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
package com.exactpro.sf.aml;

import static com.exactpro.sf.aml.AMLLangConst.BEGIN_FUNCTION;
import static com.exactpro.sf.aml.AMLLangConst.END_FUNCTION;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class AMLLangUtil {

//	/**
//	 * Value starts with this string should interpreted as java code
//	 * and will be used without transformation.
//	 * Before insertion it should be checked via compilation.
//	 */
//	public static final String TAG_INTERPRET_AS_JAVA = "java:";
//	/**
//	 * Value starts with this string should interpreted as java code
//	 * and will no checked by compilation.
//	 */
//	public static final String TAG_NO_COMPILE = "!com:";

    public static final char SEPARATOR = ',';

	private AMLLangUtil() {
		// hide constructor
	}

	public static boolean containsReferences(String value) {
	    return findReferences(value).length > 0;
	}

    public static List<String> findExpressions(String value) {
        List<String> expressions = new ArrayList<>();
        Deque<Integer> stack = new ArrayDeque<>();

        for(int i = 0; i < value.length(); i++) {
            if(value.startsWith(AMLLangConst.BEGIN_FUNCTION, i)) {
                stack.push(i);
            } else if(value.startsWith(AMLLangConst.BEGIN_REFERENCE, i)) {
                stack.push(i);
            } else if(value.startsWith(AMLLangConst.BEGIN_STATIC, i)) {
                stack.push(i);
            } else if(value.charAt(i) == '}' && !stack.isEmpty()) {
                expressions.add(value.substring(stack.pop(), i + 1));
            }
        }

        return expressions;
    }

	public static String[] findReferences(String value) {
	    List<String> references = new ArrayList<>();
        List<String> expressions = findExpressions(value);

        for(String expression : expressions) {
            if(expression.startsWith(AMLLangConst.BEGIN_REFERENCE)) {
                String ref = expression.substring(AMLLangConst.BEGIN_REFERENCE.length(), expression.length() - 1).replaceAll("\\[.*?\\]", "");
                String[] refParts = ref.split("[:.]");
                Boolean isValid = refParts.length > 0;

                for(String part : refParts) {
                    if(part.trim().isEmpty()) {
                        isValid = false;
                        break;
                    }
                }

                if(isValid) {
                    references.add(ref);
                }
	        }
	    }

	    return references.toArray(new String[references.size()]);
	}

    public static String[] getReferences(String value) throws AMLException {
        if(value.length() < 2 || !isCollection(value)) {
            throw new AMLException("Invalid reference format: " + value);
        }

        return getValues(value);
    }

    public static String[] getValues(String value) {
        String body = value.substring(1, value.length() - 1).trim();
        if (body.isEmpty()) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return body.split("\\s*,\\s*");
    }

    public static String getReference(String value) {
        return StringUtils.substringBetween(value, AMLLangConst.BEGIN_REFERENCE, AMLLangConst.END_REFERENCE);
    }

    public static String getStaticVariableName(String value) {
        return StringUtils.substringBetween(value, AMLLangConst.BEGIN_STATIC, AMLLangConst.END_STATIC);
    }

    public static boolean isSubmessage(String value) {
        return value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']';
    }

    public static boolean isCollection(String value) {
        return value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']';
    }

    public static boolean isArray(String value) {
        return value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}';
    }

    public static boolean isString(String value) {
        return value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"';
    }

    public static boolean isReference(String value) {
        return value.startsWith(AMLLangConst.BEGIN_REFERENCE) && value.endsWith(AMLLangConst.END_REFERENCE);
    }

    public static boolean isStaticVariableReference(String value) {
        return value.startsWith(AMLLangConst.BEGIN_STATIC) && value.endsWith(AMLLangConst.END_STATIC);
    }

    public static boolean isFunction(String value) {
        return value.startsWith(BEGIN_FUNCTION) && value.endsWith(END_FUNCTION);
    }

    public static boolean isExecutable(String value, boolean skipOptional) {
        //FIXME: validate value?
        if (AMLLangConst.OPTIONAL.equalsIgnoreCase(value)) {
            return !skipOptional;
        }
        return !AMLLangConst.NO.equalsIgnoreCase(value);
    }
}
