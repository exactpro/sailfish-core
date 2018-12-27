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

import java.util.Arrays;

/**
 * Collection of validations for Java language.
 * @author dmitry.guriev
 *
 */
public class JavaValidator {

	private static final String keywords[] = { "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
			"class", "const", "continue", "default", "do", "double", "else", "extends", "false", "final", "finally",
            "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
			"new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
			"super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void",
			"volatile", "while" };
	
	private JavaValidator()
	{
		// hide constructor
	}

	/**
	 * Return error string or null if function name is valid.
	 * @param typeName tested function name
	 */
	public static String validateTypeName(String typeName)
	{
		// don't conflict with @Test annotation
		if("Test".equals(typeName)) {
			return "Test is already defined";
		}
		
		if ("".equals(typeName)) {
			return "Invalid empty Type name";
		}

		if (typeName.contains(" ")) {
			return "Type '"+typeName+"' contain white space";
		}

		if (typeName.contains("\t")) {
			return "Type '"+typeName+"' contain tab character";
		}
		
		if (Arrays.binarySearch(keywords, typeName) >= 0) {
			return "Type '"+typeName+"' is reserved java word";
		}

		byte[] bytes = typeName.getBytes();
		for (int i=0; i<bytes.length; i++)
		{
			byte b = bytes[i];
			boolean valid = ( (b ==  '_')
					|| (b >= '0' && b <= '9')
					|| (b >= 'a' && b <= 'z')
					|| (b >= 'A' && b <= 'Z'));
			if (false == valid) {
				return "Type '"+typeName+"' contain invalid character";
			}
		}
		Character.isJavaIdentifierStart(bytes[0]);

		if (bytes[0] == '_') {
			return "Type '"+typeName+"' start with invalid character '_'";
		}

		if (bytes[0] >= '0' && bytes[0] <= '9') {
			return "Type '"+typeName+"' start with a digit";
		}

		return null;
	}

	/**
	 * Return error string or null if function name is valid.
	 * @param function tested function name
	 */
	public static String validateFunctionName(String function)
	{
		if ("".equals(function)) {
			return "Invalid empty function name";
		}

		if (function.contains(" ")) {
			return "Function '"+function+"' contain white space";
		}

		if (function.contains("\t")) {
			return "Function '"+function+"' contain tab character";
		}
		
		if (Arrays.binarySearch(keywords, function) >= 0) {
			return "Function '"+function+"' is reserved java word";
		}

		byte[] bytes = function.getBytes();
		for (int i=0; i<bytes.length; i++)
		{
			byte b = bytes[i];
			boolean valid = ( (b ==  '_')
					|| (b >= '0' && b <= '9')
					|| (b >= 'a' && b <= 'z')
					|| (b >= 'A' && b <= 'Z'));
			if (false == valid) {
				return "Function '"+function+"' contain invalid character";
			}
		}

		if (bytes[0] == '_') {
			return "Function '"+function+"' start with invalid character '_'";
		}

		if (bytes[0] >= '0' && bytes[0] <= '9') {
			return "Function '"+function+"' start with a digit";
		}

		return null;
	}

	/**
	 * Return error string or null if variable name is valid.
	 * @param variable tested variable name
	 */
	public static String validateVariableName(String variable)
	{
		if ("".equals(variable)) {
			return "Invalid empty variable name";
		}

		if (variable.contains(" ")) {
			return "Variable '"+variable+"' contain white space";
		}

		if (variable.contains("\t")) {
			return "Variable '"+variable+"' contain tab character";
		}

		if (Arrays.binarySearch(keywords, variable) >= 0) {
			return "Variable '"+variable+"' is reserved java word";
		}

		byte[] bytes = variable.getBytes();
		for (int i=0; i<bytes.length; i++)
		{
			byte b = bytes[i];
			boolean valid = ( (b ==  '_')
					|| (b >= '0' && b <= '9')
					|| (b >= 'a' && b <= 'z')
					|| (b >= 'A' && b <= 'Z'));
			if (false == valid) {
				return "Variable '"+variable+"' contain invalid character";
			}
		}

		if (bytes[0] == '_') {
			return "Variable '"+variable+"' start with invalid character '_'";
		}

		if (bytes[0] >= '0' && bytes[0] <= '9') {
			return "Variable '"+variable+"' start with a digit";
		}

		return null;
	}

	/**
	 * Return error string or null if package name is valid.
	 * @param packageName tested package name
	 */
	public static String validatePackageName(String packageName) {
		if (packageName == null || packageName.equals("")) {
			return null;
		}

		if (packageName.contains(" ")) {
			return "Invalid package name. Package name '"+packageName+"' contain white space";
		}

		if (packageName.contains("\t")) {
			return "Invalid package name. Package name '"+packageName+"' contain tab character";
		}

		if (packageName.contains("..")) {
			return "Invalid package name. Package name '"+packageName+"' is not valid java indentifier";
		}

		String[] parts = packageName.split("\\.");
		for (String part: parts) {
			if (Arrays.binarySearch(keywords, part) >= 0) {
				return "Invalid package name. Package name '"+packageName+"' contains reserved java word '"+part+"'";
			}
		}

		byte[] bytes = packageName.getBytes();
		for (int i=0; i<bytes.length; i++)
		{
			byte b = bytes[i];
			boolean valid = ( (b ==  '_')
					|| (b ==  '.')
					|| (b ==  '$')
					|| (b >= '0' && b <= '9')
					|| (b >= 'a' && b <= 'z')
					|| (b >= 'A' && b <= 'Z'));
			if (false == valid) {
				return "Invalid package name. Package name '"+packageName+"' is not valid java indentifier";
			}
		}

		String[] chanks = packageName.split("\\.");

		for (String chank : chanks)
		{
			bytes = chank.getBytes();
			if (bytes[0] >= '0' && bytes[0] <= '9') {
				return "Invalid package name. Package name '"+packageName+"' is not valid java indentifier";
			}
		}

		return null;

	}
}
