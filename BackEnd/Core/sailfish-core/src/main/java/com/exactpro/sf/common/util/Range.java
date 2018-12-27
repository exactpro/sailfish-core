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

import java.util.ArrayList;
import java.util.List;

/**
 * Valid values:
 * [-20 ..-17], (-15..-11),  [-9..-5), (-3..-1], 1, 3-5, [7..9], 11-
 * @author dmitry.guriev
 *
 */
public class Range {

	private String origRange;
	private List<Operator> operators;

	public static Range newInstance(String s)
	{
		if (s == null) {
			throw new NullPointerException("Range argument cannot be null.");
		}

		Range range = new Range();
		range.origRange = s;
		range.parseRange(s);
		return range;
	}

	private Range()
	{
		this.operators = new ArrayList<Operator>();
	}

	@Override
	public String toString()
	{
		return this.origRange;
	}

	public String toStringEx()
	{
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<this.operators.size(); i++)
		{
			sb.append("(");
			Operator op = this.operators.get(i);
			sb.append(op.toString());
			if (i+1 != this.operators.size()) {
				sb.append(") && ");
			} else {
				sb.append(")");
			}
		}
		
		return sb.toString();
	}
	
	public boolean contain(long n)
	{
		if (operators.size() == 0) {
			return false;
		}
		
		for (Operator op : operators)
		{
			if (op.match(n))
				return true;
		}
		return false;
	}

	public boolean notContain(long n)
	{
		if (operators.size() == 0) {
			return true;
		}

		for (Operator op : operators)
		{
			if (op.match(n))
				return false;
		}
		return true;
	}

	private void parseRange(String s)
	{
		String[] arr = s.split(",");
		for (int i=0; i<arr.length; i++) {
			Operator op = parseSingleInterval(arr[i], i+1 == arr.length);
			if (op != null) {
				this.operators.add(op);
			}
		}
	}

	private Operator parseSingleInterval(final String interval, final boolean isLast)
	{
		String s = interval.replace(" ", "");
		if (s.length() == 0) {
			throw new InvalidRangeException("Empty interval detected.");
		}

		char firstChar = s.charAt(0);
		if (firstChar == '[' || firstChar == '(')
		{
			char lastChar = s.charAt(s.length() -1);
			s = s.substring(1, s.length()-1);
			String[] arr = s.split("\\.\\.");

			if (arr.length != 2) {
				throw new InvalidRangeException("Invalid interval: "+interval);
			}
			try {
				long n1 = Long.parseLong(arr[0]);
				long n2 = Long.parseLong(arr[1]);

				if (n1 > n2) {
					throw new InvalidRangeException("Invalid interval: "+interval);
				}

				if (n1 == n2 && firstChar == '(' && lastChar == ')')
				{
					return null;
				}

				OP_AND c = new OP_AND();
				if (firstChar == '[') {
					c.add(new Condition_GreaterOrEqual(n1));
				} else if (firstChar == '(') {
					c.add(new Condition_Greater(n1));
				} else {
					throw new InvalidRangeException("Invalid open interval char '"+firstChar+"' in "+interval);
				}
				if (lastChar == ']') {
					c.add(new Condition_LessOrEqual(n2));
				} else if (lastChar == ')') {
					c.add(new Condition_Less(n2));
				} else {
					throw new InvalidRangeException("Invalid close interval char '"+lastChar+"' in "+interval);
				}
				return c;
			} catch (NumberFormatException e) {
				throw new InvalidRangeException("Invalid numbers in interval: "+interval);
			}
		}

		if (s.endsWith("-"))
		{
			if (false == isLast) {
				throw new InvalidRangeException("Only last interval can be specified without endpoint: "+interval);
			}

			try {
				long n = Long.parseLong(s.substring(0, s.length()-1));
				OP_AND c = new OP_AND();
				c.add(new Condition_GreaterOrEqual(n));
				return c;
			} catch (NumberFormatException e) {
				throw new InvalidRangeException("Invalid numbers in interval: "+interval);
			}
		}

		if (s.contains("-"))
		{
			String[] arr = s.split("-");
			if (arr.length != 2) {
				throw new InvalidRangeException("Invalid interval: "+interval);
			}
			try {
				long n1 = Long.parseLong(arr[0]);
				long n2 = Long.parseLong(arr[1]);

				if (n1 > n2) {
					throw new InvalidRangeException("Invalid numbers in interval: "+interval);
				}

				OP_AND c = new OP_AND();
				c.add(new Condition_GreaterOrEqual(n1));
				c.add(new Condition_LessOrEqual(n2));
				return c;
			} catch (NumberFormatException e) {
				throw new InvalidRangeException("Invalid numbers in interval: "+interval);
			}
		}

		try {
			long n = Long.parseLong(s);
			OP_AND c = new OP_AND();
			c.add(new Condition_Equal(n));
			return c;
		} catch (NumberFormatException e) {
			throw new InvalidRangeException("Invalid numbers in interval: "+interval);
		}
	}

	protected abstract class AbstractCondition
	{
		protected long number;
		abstract boolean checkCondition(long n);
	}

	protected class Condition_Less extends AbstractCondition
	{
		protected Condition_Less(long n) {
			this.number = n;
		}

		@Override
		public boolean checkCondition(long n) {
			return n < this.number;
		}
		
		@Override
		public String toString() {
			return "X < "+this.number;
		}
	}

	protected class Condition_LessOrEqual extends AbstractCondition
	{
		protected Condition_LessOrEqual(long n) {
			this.number = n;
		}

		@Override
		protected boolean checkCondition(long n) {
			return n <= this.number;
		}

		@Override
		public String toString() {
			return "X <= "+this.number;
		}
	}

	protected class Condition_Equal extends AbstractCondition
	{
		protected Condition_Equal(long n) {
			this.number = n;
		}

		@Override
		protected boolean checkCondition(long n) {
			return n == this.number;
		}

		@Override
		public String toString() {
			return "X == "+this.number;
		}
	}

	protected class Condition_NotEqual extends AbstractCondition
	{
		protected Condition_NotEqual(long n) {
			this.number = n;
		}

		@Override
		protected boolean checkCondition(long n) {
			return n != this.number;
		}

		@Override
		public String toString() {
			return "X != "+this.number;
		}
	}

	protected class Condition_GreaterOrEqual extends AbstractCondition
	{
		protected Condition_GreaterOrEqual(long n) {
			this.number = n;
		}

		@Override
		protected boolean checkCondition(long n) {
			return n >= this.number;
		}

		@Override
		public String toString() {
			return "X >= "+this.number;
		}
	}

	protected class Condition_Greater extends AbstractCondition
	{
		protected Condition_Greater(long n) {
			this.number = n;
		}

		@Override
		protected boolean checkCondition(long n) {
			return n > this.number;
		}

		@Override
		public String toString() {
			return "X > "+this.number;
		}
	}

	protected abstract class Operator
	{
		protected List<AbstractCondition> conditions;
		protected Operator() {
			this.conditions = new ArrayList<AbstractCondition>();
		}
		protected abstract boolean match(long n);
		protected void add(AbstractCondition cond) {
			this.conditions.add(cond);
		}
	}

	protected class OP_AND extends Operator
	{
		protected OP_AND() {
			super();
		}

		@Override
		protected boolean match(long n)
		{
			for (AbstractCondition a : this.conditions)
			{
				if (!a.checkCondition(n)) {
					return false;
				}
			}
			return true;
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<this.conditions.size(); i++) {
				sb.append(this.conditions.get(i).toString());
				if (i+1 != this.conditions.size()) {
					sb.append(" && ");
				}
			}
			return sb.toString();
		}
	}

	protected class OP_OR extends Operator
	{
		protected OP_OR() {
			super();
		}

		@Override
		protected boolean match(long n)
		{
			for (AbstractCondition a : this.conditions)
			{
				if (a.checkCondition(n)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<this.conditions.size(); i++) {
				sb.append(this.conditions.get(i).toString());
				if (i+1 != this.conditions.size()) {
					sb.append(" || ");
				}
			}
			return sb.toString();
		}

	}
}
