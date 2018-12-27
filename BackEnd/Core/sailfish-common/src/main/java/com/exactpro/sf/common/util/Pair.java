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

public class Pair<A extends Object, B extends Object> {

	private A first;
	private B second;

	public Pair(A first, B second)
	{
		this.setFirst(first);
		this.setSecond(second);
	}

	public void setFirst(A first) {
		this.first = first;
	}

	public A getFirst() {
		return first;
	}

	public void setSecond(B second) {
		this.second = second;
	}

	public B getSecond() {
		return second;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.first);
		sb.append("=");
		sb.append(this.second);
		return sb.toString();
	}
}
