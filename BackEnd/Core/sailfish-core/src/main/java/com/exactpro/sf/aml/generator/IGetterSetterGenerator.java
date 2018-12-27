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
package com.exactpro.sf.aml.generator;

import com.exactpro.sf.aml.AMLException;

/**
 *
 * @author dmitry.guriev
 *
 */
public interface IGetterSetterGenerator {

	/**
	 * Find getter method in <code>type</code> class for parameter with
	 * name <code>parameterName</code> and return code for extract value.
	 * <br>
	 * Example 1:<br>
	 * <pre>
	 * type = java.lang.HashMap
	 * parameterName = "login"
	 * </pre>
	 * method should return:
	 * <pre>
	 * get("login");
	 * </pre>
	 * Example 2:<br>
	 * <pre>
     * type = com.exactpro.quickfix.fix44.NewOrderSingle
	 * parameterName = "Symbol"
	 * </pre>
	 * method should return:
	 * <pre>
	 * getSymbol()
	 * </pre>
	 * or
	 * <pre>
     * get(new com.exactpro.quickfix.field.Symbol())
	 * </pre>
	 *
	 * @param type class where getter declared
	 * @param parameterName parameter name i.e. column in matrix.
	 * @param source for getting.
	 * @return generated code for extracting value
	 */
	public String getGetter(Class<?> type, String parameterName, String source) throws AMLException;

	///**
	// * Find getter method in <code>type</code> class for parameter with
	// * name <code>parameterName</code>;
	// * @param type message type
	// * @param parameterName name of the parameter getter method looked for
	// * @return getter method or {@code null} if method not found
	// * @throws AMLException
	// */
	//public Method getGetterMethod(Class<?> type, String parameterName) throws AMLException;

	/**
	 * Find setter method in <code>type</code> class for parameter with
	 * name <code>parameterName</code> and return code for set value.
	 * <br>
	 * Example 1:<br>
	 * <pre>
	 * type = java.lang.Map<?, ?>;
	 * parameterName = "login"
	 * value = "Jumbo"
	 * </pre>
	 * method should return:
	 * <pre>
	 * set("login", "Jumbo")
	 * </pre>
	 * Example 2:<br>
	 * <pre>
     * type = com.exactpro.quickfix.fix44.NewOrderSingle
	 * parameterName = "Symbol"
	 * value = "SUN"
	 * </pre>
	 * method should return:
	 * <pre>
	 * setSymbol("SUN")
	 * </pre>
	 * or
	 * <pre>
     * set(new com.exactpro.quickfix.field.Symbol("SUN"))
	 * </pre>
	 * @param type class where setter declared
	 * @param parameterName parameter name i.e. column name from matrix.
	 * @param value parameter value, i.e. matrix cell
	 * @param isReference <code>true</code> if value defined as reference or
	 * <code>false</code> otherwise
	 * @return generated code for setting value
	 */
	public String getSetter(Class<?> type, String parameterName, String value, boolean isReference) throws AMLException;

	///**
	// * Find setter method in <code>type</code> class for parameter with
	// * name <code>parameterName</code> with enum parameter type;
	// * @param type message type
	// * @param parameterName name of the parameter setter method looked for
	// * @return setter method with enum parameter type or {@code null} if method not found
	// */
	//public Method getSetterMethodEnum(Class<?> type, String parameterName);

	///**
	// * Find setter method in <code>type</code> class for parameter with
	// * name <code>parameterName</code> with primitive parameter type;
	// * @param type message type
	// * @param parameterName name of the parameter setter method looked for
	// * @return setter method with primitive parameter type or {@code null} if method not found
	// */
	//public Method getSetterMethodPrimitive(Class<?> type, String parameterName);

	/**
	 * Generate code for adding submessage to parent message.
	 * @param type class where setter declared
	 * @param parameterName name of the parameter i.e. column name in matrix
	 * @param value reference to submessage
	 * @param paramClass parameter class (optional)
	 * @return generated code for adding submessage or {@code null} if submessage not exists
	 */
	public String addSubmessage(Class<?> type, String parameterName, String value, Class<?> paramClass) throws AMLException;

	/**
	 * Find submessage class with name <code>childClassName</code>
	 * in parent class <code>type</code>.
	 * @param type parent class
	 * @param childClassName submessage class name
	 * @param paramClass parameter class (optional)
	 * @return submessage class or {@code null} if submessage not exists
	 */
	public Class<?> getSubmessageClass (Class<?> type, String childClassName, Class<?> paramClass) throws AMLException;

	// /**
	// * Find setter method in message class.
	// * Is it argument class find public static final constant or enum and extract value.
	// * @param type message type
	// * @param parameterName parameter name
	// * @param value field value
	// * @return extracted value of constant or enum or return null
	// * @throws AMLException
	// */
	//public Object getConstantValue(Class<?> type, String parameterName, String value) throws AMLException;

	/**
	 * For some message we should call a method to get object
	 * for converting to tree using SimpleTreeEntity.
	 * @return
	 */
	public String getMethodForExtractingTreeEntity() throws AMLException;
}
