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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

	public static final String EOL = System.getProperty("line.separator");

	private StringUtil()
	{
		// hide constructor
	}

	private static final int shift = 'a' - 'A';

	private static final String DEFAULT_ENCODING = "UTF-8";

    private static final Pattern FORBIDDEN_FILENAME_ELEMENTS = Pattern.compile("<|>|:|\"|\\/|\\\\|\\||\\?|\\*|[\\x00-\\x1F]|(\\.|\\s)$|^(CON|PRN|AUX|NUL|COM\\d|LPT\\d)$");

    public static void main(String[] args) {
        validateFileName("str/dgfg");
    }

	/**
	 * Capitalize word. Search in input string first character "a-z,A-Z" and
	 * let it be in upper case. Other word characters will be in lower case.
	 * @param s string to be capitalized
	 * @return capitalized string
	 */
	public static String capitalize(String s)
	{
		StringBuilder sb = new StringBuilder(s.length());

		boolean isFirst = true;

		for (byte b : s.getBytes())
		{
			if ((b >= 'a' && b <= 'z'))
			{
				if (isFirst) {
					b -= shift;
					isFirst = false;
				}
			} else if ((b >= 'A' && b <= 'Z'))
			{
				if (isFirst) {
					isFirst = false;
				} else {
					b += shift;
				}
			}

			sb.append((char)b);
		}
		return sb.toString();
	}

	/**
	 * Capitalize all strings in array. In each word find first character
	 * "a-z,A-Z" and convert it to upper case.
	 * Other characters will be converted to lower case.
	 * @param s array of strings to be capitalized
	 * @return array of capitalized strings
	 */
	public static String[] capitalize(String[] s) {
		String[] ss = new String[s.length];

		for (int i=0; i<s.length; i++) {
			ss[i] = capitalize(s[i]);
		}
		return ss;
	}

	/**
	 * Join array of strings using a string delimiter.<br>
	 * <br>
	 * Example:
	 * <pre>
	 * join("_", new String[] {"Hello", "world", ""})
	 * will return "Hello_world_"
	 * </pre>
	 * @param delim string delimiter
	 * @param arr array of strings to be joined
	 * @return joined string
	 */
	public static String join(String delim, String[] arr)
	{
		StringBuilder sb = new StringBuilder();

		for (int i=0; i<arr.length; i++) {
			sb.append(arr[i]);
			if (i < arr.length-1) {
				sb.append(delim);
			}
		}

		return sb.toString();
	}

	/**
	 * Join array of strings using a string delimiter.<br>
	 * <br>
	 * Example:
	 * <pre>
	 * join("_", new String[] {"Hello", "world", ""})
	 * will return "Hello_world_"
	 * </pre>
	 * @param delim string delimiter
	 * @param arr array of strings to be joined
	 * @return joined string
	 */
	public static String join(String delim, String[] arr, int begin)
	{
		StringBuilder sb = new StringBuilder();

		for (int i=begin; i<arr.length; i++) {
			sb.append(arr[i]);
			if (i < arr.length-1) {
				sb.append(delim);
			}
		}

		return sb.toString();
	}

	/**
	 * Return "s" if quantity not equals 1 or empty string otherwise.
	 * @param quantity
	 * @return
	 */
	public static String getSSuffix(int quantity) {
		if (quantity != 1) return "s";
		return "";
	}

	/**
	 * Parse string: '1-3,5 , 7-' to set of integers.
	 * @param s string representation of range.
	 * @param size maximum size limit.
	 * @return set of integers
	 */
	public static Set<Integer> parseRange(String s, int size)
	{
		Set<Integer> list = new HashSet<>();
		if (s == null || s.trim().equals("")) {
			for (int i=0; i<size; i++) {
				list.add(i);
			}
			return list;
		}

		String[] ranges = s.split(",");
		for (int i=0; i<ranges.length; i++)
		{
			String range = ranges[i].trim();
			if (range.endsWith("-")) {
				if (i < ranges.length-1) {
					throw new IllegalArgumentException("Range can ends with '-' only in last enumeration. ");
				}
				String first = range.substring(0,range.length() - 1).trim();
				int index;
				try {
					index = Integer.parseInt(first);
				} catch (Exception e) {
					throw new IllegalArgumentException("Enumeration '"+range+"' did not contain a number as first index. ");
				}

                if(index >= size) {
                    throw new IllegalArgumentException("Range start index is greater than testCase amount. ");
                }
                if(index <= 0) {
                    throw new IllegalArgumentException("Range start index must be positive number. ");
                }

				for (; index <= size; index++) {
					list.add(index);
				}
			} else if (range.contains("-")) {
				String[] numbers = range.split("-");
				if (numbers.length != 2) {
					throw new IllegalArgumentException("Invalid enumeration: '"+range+"'. ");
				}
				int index1;
				int index2;
				try {
					index1 = Integer.parseInt(numbers[0]);
				} catch (Exception e) {
					throw new IllegalArgumentException("Enumeration '"+range+"' did not contain a number as first index. ");
				}

                if(index1 >= size){
                    throw new IllegalArgumentException("Range start index is greater than testCase amount. ");
                }
                if(index1 <= 0){
                    throw new IllegalArgumentException("Range start index must be positive number. ");
                }

				try {
					index2 = Integer.parseInt(numbers[1]);
				} catch (Exception e) {
					throw new IllegalArgumentException("Enumeration '"+range+"' did not contain a number as last index. ");
				}

                if(index2 >= size) {
                    throw new IllegalArgumentException("Range end index is greater than testCase amount. ");
                }
                if(index2 <= 0) {
                    throw new IllegalArgumentException("Range end index must be positive number. ");
                }

                if(index1 > index2) {
                    throw new IllegalArgumentException("Range end index must be greater than start index. ");
                }

				for (; index1 <= index2; index1++) {
					list.add(index1);
				}
			} else {
				int index1;
				try {
					index1 = Integer.parseInt(range);
				} catch (Exception e) {
					throw new IllegalArgumentException("Enumeration '"+range+"' is not a number. ");
				}

                if((index1 <= 0) || (index1 >= size)) {
                    throw new IllegalArgumentException("Test case number " + index1 + " does not exist. ");
                }

				list.add(index1);
			}
		}

		return list;
	}

	/**
	 * Compare 2 objects without NullPointerException.
	 * @param o1
	 * @param o2
	 * @return {@code true} if objects are equals or both are {@code null}
	 */
	public static boolean equals(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return true;
		}
		if (o1 != null) {
			return o1.equals(o2);
		}
		return false;
	}

	/**
	 * Split string to array.
	 * @param s source string
	 * @param delim delimiter
	 * @return
	 */
	public static String[] split(final String s, final String delim)
	{
		if (s == null) {
			throw new NullPointerException("String is null");
		}
		if (delim == null) {
			throw new NullPointerException("Delimiter is null");
		}
		if (delim.equals("")) {
			List<String> arr = new ArrayList<>();
			for (int i=0; i<s.length(); i++) {
				arr.add(String.valueOf(s.charAt(i)));
			}
			return arr.toArray(new String[arr.size()]);
		}

		List<String> list = new ArrayList<>();

		int beginIndex = 0;
		int endIndex = s.indexOf(delim);
		while (endIndex != -1) {
			list.add(s.substring(beginIndex, endIndex));
			beginIndex = endIndex+delim.length();
			endIndex = s.indexOf(delim, beginIndex);
		}

		list.add(s.substring(beginIndex, s.length()));

		return list.toArray(new String[list.size()]);
	}

	public static String toJavaString(String s)
	{
		if (s == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
        for(char b : s.toCharArray()) {
			switch (b) {
			case '"': sb.append("\\\""); break;
			case '\\': sb.append("\\\\"); break;
			case '\n': sb.append("\\n"); break;
			case '\r': sb.append("\\r"); break;
			case '\t': sb.append("\\t"); break;
			default:
                sb.append(b);
			}
		}
		return sb.toString();
	}

	private static HashMap<Character, String> trans = null;
	private static int cur = 192;

	private static void next(String s) {
		trans.put( (char)cur, s );
		cur++;
	}

	private static void initHTMLtransformer()
	{
		trans = new HashMap<>();
		trans.put('<', "&lt;");
		trans.put('>', "&gt;");
		trans.put('&', "&amp;");
		trans.put('\'', "&#39;");
		trans.put('\256', "&reg;");
		trans.put('\251', "&copy;");
		trans.put('\200', "&euro;");
		//trans.put(' ', "&nbsp;"); // 2011.11.25 DG: do not replace spaces!!!

		cur = 192;
		next("&Agrave;"); //192
		next("&Aacute;");
		next("&Acirc;");
		next("&Atilde;"); //195
		next("&Auml;");
		next("&Aring;");
		next("&AElig;");
		next("&Ccedil;");
		next("&Egrave;"); //200
		next("&Eacute;");
		next("&Ecirc;");
		next("&Euml;");
		next("&Igrave;");
		next("&Iacute;"); //205
		next("&Icirc;");
		next("&Iuml;");
		next("&ETH;");
		next("&Ntilde;");
		next("&Ograve;"); //210
		next("&Oacute;");
		next("&Ocirc;");
		next("&Otilde;");
		next("&Ouml;");
		next("&times;");  //215
		next("&Oslash;");
		next("&Ugrave;");
		next("&Uacute;");
		next("&Ucirc;");
		next("&Uuml;");   //220
		next("&Yacute;");
		next("&THORN;");
		next("&szlig;");  //223

		next("&agrave;"); //224
		next("&aacute;"); //225
		next("&acirc;");
		next("&atilde;");
		next("&auml;");
		next("&aring;");
		next("&aelig;");  //230
		next("&ccedil;");
		next("&egrave;");
		next("&eacute;");
		next("&ecirc;");
		next("&euml;");   //235
		next("&igrave;");
		next("&iacute;");
		next("&icirc;");
		next("&iuml;");
		next("&eth;");    //240
		next("&ntilde;");
		next("&ograve;");
		next("&oacute;");
		next("&ocirc;");
		next("&otilde;"); //245
		next("&ouml;");
		next("&divide;");
		next("&oslash;");
		next("&ugrave;");
		next("&uacute;"); //250
		next("&ucirc;");
		next("&uuml;");
		next("&yacute;");
		next("&thorn;");
		next("&yuml;");  //255

	}

	public static final String escapeHTML(String s){
	   StringBuffer sb = new StringBuffer();
	   int n = s.length();
	   if (trans == null)
		   initHTMLtransformer();


	   for (int i = 0; i < n; i++) {
	      char c = s.charAt(i);
	      String app = trans.get(c);
	      if (app != null)
	    	  sb.append(app);
	      else
	    	  sb.append(c);
	   }
	   return sb.toString();
	}

	public static String escapeURL(String url) {
	    String result = url.replace("\\", "/").replace("%5C", "/"); // remove windows separator

        try {
            return URLEncoder.encode(result, DEFAULT_ENCODING).replaceAll("%2F", "/");
        } catch(UnsupportedEncodingException e) {
            throw new EPSCommonException("Failed to escape URL", e);
        }
    }

    public static boolean containsAll(String word, String ...keywords) {
        for (String current : keywords) {
            if (!word.contains(current)) {
                return false;
            }
        }
        return true;
    }

	/**
	 * Replaces a set of characters with replacement
	 * @param string an input string
	 * @param chars a string of characters to be replaced
	 * @param replacement a replacement char
	 * @return
	 */
	public static final String replaceChars(String string, String chars, char replacement) {
		for (int i = 0; i < chars.length(); i++) {
			string = string.replace(chars.charAt(i), replacement);
		}
		return string;
	}

	/**
	 * Chooses the maximum string between two strings
	 * @param s1 the first string
	 * @param s2 the second string
	 * @return the maximum string
	 */
	public static final String maximumString(String s1, String s2) {
		if (s1.compareTo(s2) > 0) return s1;
		else return s2;
	}

	public static final String enclose(String input) {
        return enclose(input, '"');
    }

	public static final String enclose(String input, char enclosure) {
	    return enclosure + input + enclosure;
	}

	public static boolean isStripped(CharSequence cs) {
	    if(cs == null || cs.length() == 0) {
	        return true;
	    }

	    return !Character.isWhitespace(cs.charAt(0)) && !Character.isWhitespace(cs.charAt(cs.length() - 1));
	}

    public static String validateFileName(String fileName) {
        Matcher matcher = FORBIDDEN_FILENAME_ELEMENTS.matcher(fileName);

        if(matcher.find()) {
            throw new EPSCommonException(String.format("File name '%s' contains forbidden element: '%s'", fileName.replace("%", "%%"), matcher.group()));
        }

        return fileName;
    }
}
