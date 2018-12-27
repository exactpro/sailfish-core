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
package com.exactpro.sf.aml.scriptutil;

import java.util.regex.Pattern;

import com.exactpro.sf.common.util.Range;

public class MessageCount {

    protected static final Pattern EXPRESSION_PATTERN;

    static {
        String operationRegex = "(>|<|>=|<=|=|\\!=)";
        String arithmeticOperationRegex = "(\\+|\\-|\\*|\\/)";
        String referenceRegex = "(\\$\\{\\w+([.:]\\w+(\\[\\d+\\])?)+\\})";
        String arithmeticReferenceRegex = "\\(?" + referenceRegex + "(" + arithmeticOperationRegex + "\\d+)?\\)?";
        String valueOrReferenceRegex = "(\\d+|" + arithmeticReferenceRegex + ")";
        String functionRegex = "(#\\{(Expected(Any|Empty)?)\\(.*?\\)(\\.(Bug(Any|Empty)?|Actual|validate)\\(.*?\\))*\\})";
        EXPRESSION_PATTERN = Pattern.compile("^("
                    + operationRegex + "?"
                    + valueOrReferenceRegex
                + ")|("
                    + "[\\(\\[]"
                    + valueOrReferenceRegex
                    + "\\.\\."
                    + valueOrReferenceRegex
                    + "[\\)\\]]"
                + ")|("
                    + functionRegex
                + ")$"
                );
    }

	enum Operation {
		equals("="),
		notEquals("!="),
		greater(">"),
		greaterOrEquals(">="),
		less("<"),
		lessOrEquals("<=");

		private String value;

		private Operation(String s)
		{
			this.value = s;
		}

		public String getValue() {
			return this.value;
		}

	}

	private int count;
	private Range range;
	private Operation operation;

	private MessageCount(int count, Operation op) {
		this.count = count;
		this.operation = op;
	}

    private MessageCount(Range range) {
		this.range = range;
	}

    /**
     * @param value input value, which is the number. May be with prefix =, !=, <..
     * @return MessageCount instance or null, if input value is incorrect
     */
	public static MessageCount fromString(String value) {

		MessageCount mc;

		if ((mc = parse(value, Operation.equals)) != null) return mc;
		if ((mc = parse(value, Operation.notEquals)) != null) return mc;
		if ((mc = parse(value, Operation.greaterOrEquals)) != null) return mc;
		if ((mc = parse(value, Operation.greater)) != null) return mc;
		if ((mc = parse(value, Operation.lessOrEquals)) != null) return mc;
		if ((mc = parse(value, Operation.less)) != null) return mc;
        if (isInterval(value)){
            Range range = Range.newInstance(value);
            return new MessageCount(range);
        }
		try {
			int i = Integer.parseInt(value);
			if(i >= 0) {
				mc = new MessageCount(i, Operation.equals);
			}
		} catch (NumberFormatException ignore){}

		return mc;
	}

    protected static boolean isInterval(String value){
        String trimValue = value.replace(" ", "");
        return trimValue.matches("(\\[|\\()?\\d+(\\.\\.|\\-)\\d+(\\]|\\))?");
    }

    public static void main(String[] args)
    {
        System.out.println(isValidExpression("#{ExpectedAny(1).BugAny('zzz', 2)}"));
    }

    public static boolean isValidExpression(String value)
    {
    	String trimValue = value.replaceAll(" ", "");
    	return EXPRESSION_PATTERN.matcher(trimValue).matches();
    }

	private static MessageCount parse(String s, Operation op) {
		String str = s.trim();
		if (str.startsWith(op.getValue())) {
			str = str.substring(op.getValue().length());
			str = str.trim(); // handle '> 10'
			try{
				int count = Integer.parseInt(str);
				if(count >= 0) {
					return new MessageCount(count, op);
				}
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}

    /**
     * @return count or 0, if input value is interval
     */
	public int getCount() {
		return this.count;
	}

	public Operation getOperation() {
		return this.operation;
	}

	/**
	 * Check that argument against expected message count.
	 * @param i actual number of messages
	 * @return comparison between actual and expected number of messages
	 */
	public boolean checkInt(int i)
	{
		if(range == null) {
            switch (this.operation) {
                case equals:
                    return i == this.count;
                case greater:
                    return i > this.count;
                case greaterOrEquals:
                    return i >= this.count;
                case less:
                    return i < this.count;
                case lessOrEquals:
                    return i <= this.count;
                case notEquals:
                    return i != this.count;
                default:
                    return false;
            }
        } else{
            return range.contain(i);
        }

	}

	@Override
	public String toString() {
		if(range == null){
            return (this.operation != Operation.equals ? operation.getValue() : "") + this.count;
        } else{
            return range.toString();
        }
	}
}
