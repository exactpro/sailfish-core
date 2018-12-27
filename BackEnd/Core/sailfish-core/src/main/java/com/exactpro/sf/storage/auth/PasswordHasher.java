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
package com.exactpro.sf.storage.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordHasher {
	private static final String SALT = "LongStringForExtraSecurity@#$!%^&*(*)1234567890#";

	private static final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static final Logger logger = LoggerFactory
			.getLogger(PasswordHasher.class);

	public static String getHash(String toHash) {

		MessageDigest messageDigest = null;

		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update((toHash + SALT).getBytes("UTF-8"));

		} catch (NoSuchAlgorithmException e) {

			logger.error("Could not get hash!", e);
			return null;

		} catch (UnsupportedEncodingException e) {

			logger.error("Could not get hash!", e);
			return null;
		}

		return encodeHex(messageDigest.digest());
	}

	protected static String encodeHex(final byte[] data) {

		final int l = data.length;
		final char[] out = new char[l << 1];

		// two characters form the hex value.

		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
			out[j++] = DIGITS_LOWER[0x0F & data[i]];
		}

		return new String(out);
	}

	public static String getSalt() {
		return SALT;
	}

}