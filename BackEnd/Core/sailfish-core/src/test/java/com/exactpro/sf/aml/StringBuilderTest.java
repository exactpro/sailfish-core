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

import org.junit.Ignore;
import org.junit.Test;

public class StringBuilderTest {

	@Ignore // no assertions
	@Test
	public void testStringBuilderPerformance()
	{
		int nIterations = 10000000;

		String s = "";
		String s2 = "";
		String s3 = "";
		for (int i=0; i < 300; i++) {
			s += i;
			s2 = i+s2;
			s3 = i+s3+i;
		}
		
		long time = System.currentTimeMillis();
		for (int i=0; i<nIterations; i++)
		{
			StringBuffer sb = new StringBuffer();
			sb.append(" ");
			sb.append(i);
			sb.append(" text");
			sb.append(i);
			sb.append(s);
			sb.append("asd");
			sb.append(s2);
			sb.append(i);
			sb.append(s3);
			sb.append("\r\n");
			sb.append(i);
			sb.append("asdas");
		}
		time = System.currentTimeMillis() - time;
		System.out.println(time);

		time = System.currentTimeMillis();
		for (int i=0; i<nIterations; i++)
		{
			new String(" "+i+" text"+i+s+"asd"+s2+i+s3+"\r\n"+i+"asdas");
		}
		time = System.currentTimeMillis() - time;
		System.out.println(time);

	}
}
