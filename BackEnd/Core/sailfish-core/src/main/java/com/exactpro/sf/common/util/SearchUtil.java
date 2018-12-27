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

/**
 * This class contain collection of utilities to search substrings in strings.
 *
 * @author dmitry.guriev
 *
 */
public class SearchUtil {

	private SearchUtil()
	{
		// hide constructor
	}

	public static int indexOf(String src, String s1, int n)
	{
		return indexOf(src, s1, n, 0);
	}

	public static int indexOf(String src, String s1, int n, int fromIndex)
	{
		for (int i=0; i<n; i++)
		{
			fromIndex = src.indexOf(s1, fromIndex);
			if (fromIndex == -1)
				return -1;
			if (i+1 != n)
				fromIndex++;
		}
		return fromIndex;
	}

	public static String substring(String src, String s1, int n1)
	{
		int beginIndex = indexOf(src, s1, n1);
		if (beginIndex == -1)
			return null;
		beginIndex += s1.length();
		return src.substring(beginIndex);
	}

	public static String substring(String src, String s1)
	{
		return substring(src, s1, 1);
	}

	public static String substring(String src, String s1, int n1, String s2, int n2)
	{
		int beginIndex = indexOf(src, s1, n1, 0);
		if (beginIndex == -1)
			return null;
		beginIndex += s1.length();
		int endIndex = indexOf(src, s2, n2, beginIndex);
		if (endIndex == -1)
			return null;
		return src.substring(beginIndex, endIndex);
	}

	public static String substring(String src, String s1, String s2)
	{
		return substring(src, s1, 1, s2, 1);
	}

	public static int lastIndexOf(String src, String s1, int n, int fromIndex)
	{
		for (int i=0; i<n; i++)
		{
			fromIndex = src.lastIndexOf(s1, fromIndex);
			if (fromIndex == -1)
				return -1;
			if (i+1 != n)
				fromIndex--;
		}
		return fromIndex;
	}

	public static String searchFBFF(String src, String s1, String s2, String s3, String s4)
	{
		int index = src.indexOf(s1);
		if (index == -1)
			return null;
		index = lastIndexOf(src, s2, 1, index);
		if (index == -1)
			return null;
		index += s2.length();
		index = src.indexOf(s3, index);
		if (index == -1)
			return null;
		index += s3.length();
		int index2 = src.indexOf(s4, index);
		if (index2 == -1)
			return null;
		return src.substring(index, index2);
	}

	public static String searchFBF(String src, String s1, String s2, String s3)
	{
		int index = src.indexOf(s1);
		if (index == -1)
			return null;
		index = lastIndexOf(src, s2, 1, index);
		if (index == -1)
			return null;
		index += s2.length();
		int index2 = src.indexOf(s3, index);
		if (index2 == -1)
			return null;
		return src.substring(index, index2);
	}

	public static String searchFF(String src, String s1, String s2)
	{
		int index = src.indexOf(s1);
		if (index == -1)
			return null;
		index += s1.length();
		int index2 = src.indexOf(s2, index);
		if (index2 == -1)
			return null;
		return src.substring(index, index2);
	}

	public static String searchF(String src, String s1)
	{
		int index = src.indexOf(s1);
		if (index == -1)
			return null;
		index += s1.length();
		return src.substring(index);
	}
}
