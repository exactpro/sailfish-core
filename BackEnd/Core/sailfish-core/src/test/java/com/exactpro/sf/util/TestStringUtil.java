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
package com.exactpro.sf.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.util.StringUtil;

public class TestStringUtil extends EPSTestCase {

	@Test
	public void testCapitalize()
	{
		String s = "capitalize";
		Assert.assertEquals("Capitalize", StringUtil.capitalize(s));
		s = "Capitalize";
		Assert.assertEquals("Capitalize", StringUtil.capitalize(s));
		s = "CAPITALIZE";
		Assert.assertEquals("Capitalize", StringUtil.capitalize(s));
		s = "cAPITALIZE";
		Assert.assertEquals("Capitalize", StringUtil.capitalize(s));
		s = "_capitalize";
		Assert.assertEquals("_Capitalize", StringUtil.capitalize(s));
		s = "_Capitalize";
		Assert.assertEquals("_Capitalize", StringUtil.capitalize(s));
		s = "_CAPITALIZE";
		Assert.assertEquals("_Capitalize", StringUtil.capitalize(s));
	}

	@Test
	public void testJoin()
	{
		String[] s = "hello world".split(" ");
		Assert.assertEquals("hello_world", StringUtil.join("_", s));
		s = "hello  world".split(" ");
		Assert.assertEquals("hello__world", StringUtil.join("_", s));
		s = " hello  world ".split(" ");
		Assert.assertEquals("_hello__world", StringUtil.join("_", s));


		Assert.assertEquals("HelloWorld", StringUtil.join("", StringUtil.capitalize("hello world".split(" "))));
	}

	@Test
	public void testParseRange()
	{
		Set<Integer> actual = StringUtil.parseRange("1-3, 5, 7-", 10);
		Set<Integer> expected = new HashSet<Integer>();
		expected.add(1);
		expected.add(2);
		expected.add(3);
		expected.add(5);
		expected.add(7);
		expected.add(8);
		expected.add(9);
		expected.add(10);
		Assert.assertEquals(expected, actual);

        boolean thrown = false;
        try
        {
            actual = StringUtil.parseRange("1-3, 5, 7-", 6);
        }
        catch(IllegalArgumentException e)
        {
            thrown = true;
        }

        Assert.assertTrue(thrown);
	}

	@Test
	public void testSplit()
	{
		List<String> actual = Arrays.asList(StringUtil.split("abc", ""));
		List<String> expected = Arrays.asList(new String[] {"a", "b", "c"});
		Assert.assertEquals(expected, actual);

		actual = Arrays.asList(StringUtil.split("ab|", "|"));
		expected = Arrays.asList(new String[] {"ab", ""});
		Assert.assertEquals(expected, actual);

		actual = Arrays.asList(StringUtil.split("ab|d", "|"));
		expected = Arrays.asList(new String[] {"ab", "d"});
		Assert.assertEquals(expected, actual);

		actual = Arrays.asList(StringUtil.split("|ab|d", "|"));
		expected = Arrays.asList(new String[] {"", "ab", "d"});
		Assert.assertEquals(expected, actual);

		actual = Arrays.asList(StringUtil.split("||", "|"));
		expected = Arrays.asList(new String[] {"", "", ""});
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testReplaceChars()
	{
		final String chars = " ?#;";
		final char replacement = '_';

		final String src0 = "#123 ?(A;B).csv";
		String dst0 = StringUtil.replaceChars(src0.trim(), chars, replacement);
		Assert.assertEquals("_123__(A_B).csv", dst0);

		final String src1 = "API_Auction_Negative _Testing(OrdPlecement;Replace).csv";
		String dst1 = StringUtil.replaceChars(src1.trim(), chars, replacement);
		Assert.assertEquals("API_Auction_Negative__Testing(OrdPlecement_Replace).csv", dst1);

		final String src2 = "USD_Real_Real_v8 Semi betta1.csv";
		String dst2 = StringUtil.replaceChars(src2.trim(), chars, replacement);
		Assert.assertEquals("USD_Real_Real_v8_Semi_betta1.csv", dst2);

	}

}
