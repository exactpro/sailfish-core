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

import static org.junit.Assert.*;

import org.junit.Test;

public class RangeTest {

	@Test
	public void testContain()
	{
		Range r = Range.newInstance("0-1");
		assertTrue(r.contain(0));
		assertTrue(r.contain(1));
		assertTrue(r.notContain(2));
		System.out.println(r.toStringEx());
	}

	@Test
	public void testContain2()
	{
		Range r = Range.newInstance(" 1 ,3,5-7, 9 - ");
		assertTrue(r.notContain(0));
		assertTrue(r.contain(1));
		assertTrue(r.notContain(2));
		assertTrue(r.contain(3));
		assertTrue(r.notContain(4));
		assertTrue(r.contain(5));
		assertTrue(r.contain(6));
		assertTrue(r.contain(7));
		assertTrue(r.notContain(8));
		assertTrue(r.contain(9));
		System.out.println(r.toStringEx());
	}

	@Test
	public void testContain3()
	{
		Range r = Range.newInstance("(-8..-6), [-5..-5], 1 ,( 2 .. 4), [5..8), (8 .. 10)");
		assertTrue(r.notContain(-8));
		assertTrue(r.contain(-7));
		assertTrue(r.notContain(-6));
		assertTrue(r.contain(-5));
		assertTrue(r.notContain(0));
		assertTrue(r.contain(1));
		assertTrue(r.notContain(2));
		assertTrue(r.contain(3));
		assertTrue(r.notContain(4));
		assertTrue(r.contain(5));
		assertTrue(r.contain(6));
		assertTrue(r.contain(7));
		assertTrue(r.notContain(8));
		assertTrue(r.contain(9));
		System.out.println(r.toStringEx());
	}

	@Test(expected=InvalidRangeException.class)
	public void testInvalid1()
	{
		Range.newInstance("a");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid2()
	{
		Range.newInstance("[1..]");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid3()
	{
		Range.newInstance("[..1]");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid4()
	{
		Range.newInstance("[a..1]");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid5()
	{
		Range.newInstance("[2..1]");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid6()
	{
		Range.newInstance("[1..2}");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid7()
	{
		Range.newInstance("{1..1]");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid8()
	{
		Range.newInstance("(1...1)");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid9()
	{
		Range.newInstance("[1.1]");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid10()
	{
		Range.newInstance("[1 .... 1]");
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid11()
	{
		Range.newInstance("[10..1]");
	}
	@Test(expected=NullPointerException.class)
	public void testInvalid12()
	{
		Range.newInstance(null);
	}
	@Test(expected=InvalidRangeException.class)
	public void testInvalid13()
	{
		Range.newInstance("");
	}
}
