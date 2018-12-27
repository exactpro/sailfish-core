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
package com.exactpro.sf;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.services.util.ServiceUtil;
import com.google.common.primitives.UnsignedLong;

/**
 * @author nikita.smirnov
 *
 */
public class TestUnsigned {
	
	private static final UnsignedLong MIN_UN_LONG = UnsignedLong.valueOf(10000L);
	private static final UnsignedLong MAX_UN_LONG = UnsignedLong.valueOf(100000000L);
	private static final BigInteger MIN_B_DEC = new BigInteger("10000");
	private static final BigInteger MAX_B_DEC = new BigInteger("100000000");
	
	private static final long TIMES = 1000000L;
	
//	@Test
	public void testUnsignedTime() {
		BigInteger bigint = new BigInteger("18446744073709551615");

		byte[] array = bigint.toByteArray();
		
		long start = System.nanoTime();
		
		for (int i = 0; i < TIMES; i++) {
			doubleValue(array, MIN_B_DEC);
		}
		
		System.out.println((System.nanoTime() - start) / 1000000L);
		
		start = System.nanoTime();
		
		for (int i = 0; i < TIMES; i++) {
			doubleValue(array, MAX_B_DEC);
		}
		
		System.out.println((System.nanoTime() - start) / 1000000L);
		
		array = ServiceUtil.normalisate(array, 8);
		
		start = System.nanoTime();
		
		for (int i = 0; i < TIMES; i++) {
			ServiceUtil.convertFromUint64(array, MIN_UN_LONG);
		}
		
		System.out.println((System.nanoTime() - start) / 1000000L);
		
		start = System.nanoTime();
		
		for (int i = 0; i < TIMES; i++) {
			ServiceUtil.convertFromUint64(array, MAX_UN_LONG);
		}
		
		System.out.println((System.nanoTime() - start) / 1000000L);
	}
	
//	@Test
	public void testUnsignedConvertTime() {
		BigInteger bigint = new BigInteger("18446744073709551615");

		byte[] array = bigint.toByteArray();
		
		long start = System.nanoTime();
		
		for (int i = 0; i < TIMES * 100; i++) {
			doubleValue(array);
		}
		
		System.out.println((System.nanoTime() - start) / 1000000L);
		
		start = System.nanoTime();
		
		for (int i = 0; i < TIMES * 100; i++) {
			ServiceUtil.convertFromUint64(array);
		}
		
		System.out.println((System.nanoTime() - start) / 1000000L);
	}

	@Test
	public void testUnsignedEquals() {
		BigInteger bigint = new BigInteger("18446744073709551615");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("1844674407370955161");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("184467440737095516");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("18446744073709551");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("1844674407370955");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("184467440737095");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("18446744073709");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("1844674407370");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("184467440737");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("18446744073");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
		bigint = new BigInteger("1844674407");
		check(bigint.toByteArray(), MAX_UN_LONG, MAX_B_DEC);
	}
	
	@Test
	public void testUnsignedConvert() {
		BigInteger bigint = new BigInteger("18446744073709551615");
		
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("1844674407370955161");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("184467440737095516");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("18446744073709551");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("1844674407370955");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("184467440737095");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("18446744073709");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("1844674407370");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("184467440737");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("18446744073");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
		bigint = new BigInteger("1844674407");
		Assert.assertEquals(bigint.toString(), ServiceUtil.convertFromUint64(bigint.toByteArray()).toString());
	}
	
	@Test
	public void testNormalization() {
		byte[] array = new byte[] { 0, 1, 2, 3, 4 };
		
		Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4 }, ServiceUtil.normalisate(array, 4));
		Assert.assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, ServiceUtil.normalisate(array, 5));
		Assert.assertArrayEquals(new byte[] { 0, 0, 1, 2, 3, 4 }, ServiceUtil.normalisate(array, 6));
		
		Assert.assertArrayEquals(new byte[0], ServiceUtil.normalisate(new byte[] {0, 0, 0}, 0));
		
		String msg = null;
		try {
			ServiceUtil.normalisate(array, 3);
		} catch (RuntimeException e) {
			msg = e.getMessage();
		}
		
		Assert.assertEquals("Massive compression does not execute without data loss", msg);
		
		msg = null;
		try {
			ServiceUtil.normalisate(array, 0);
		} catch (RuntimeException e) {
			msg = e.getMessage();
		}
		
		Assert.assertEquals("Massive compression does not execute without data loss", msg);
		
		msg = null;
		try {
			ServiceUtil.normalisate(array, -1);
		} catch (RuntimeException e) {
			msg = e.getMessage();
		}
		
		Assert.assertEquals("Size cannot be negative", msg);
		
		msg = null;
		try {
			ServiceUtil.normalisate(null, -1);
		} catch (RuntimeException e) {
			msg = e.getMessage();
		}
		
		Assert.assertEquals("Array is null", msg);
	}
	
	/**
	 * @param bigint
	 * @param unLg
	 */
	private void check(byte[] array, UnsignedLong unLgDivider, BigInteger bdDivider) {
		
		byte[] arrayBD = array;
		byte[] arrayUL = ServiceUtil.normalisate(arrayBD, 8);
		
		double doubleDB = doubleValue(arrayBD, bdDivider);
		double doubleUL = ServiceUtil.convertFromUint64(arrayUL, unLgDivider);
		
		System.out.println(doubleDB + " " + doubleUL);
		Assert.assertEquals(doubleDB, doubleUL, 0);
	}
	
	/**
	 * @param bigint
	 * @param unLg
	 * @return 
	 */
	private double doubleValue(byte[] array, BigInteger bdDivider) {
		
		BigInteger bigInt = new BigInteger(array);

		BigDecimal bigDec = new BigDecimal(bigInt).divide(new BigDecimal(bdDivider));

		return bigDec.doubleValue();
	}
	
	/**
	 * @param bigint
	 * @param unLg
	 * @return 
	 */
	private BigDecimal doubleValue(byte[] array) {
		
		BigInteger bigInt = new BigInteger(array);

		return new BigDecimal(bigInt);
	}
}
