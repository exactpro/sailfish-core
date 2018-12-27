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

public enum AMLBlockBrace {
    TestCaseStart("Test case start", true),
    TestCaseEnd("Test case end", false),

    // actions from this block will be substituted to each test case as first actions
    GlobalBlockStart("Global Block start", true),
    GlobalBlockEnd("Global Block end", false),

    // actions from this block will be executed _before_every_ test case (as @Before)
    BeforeTCBlockStart("Before Test Case Block start", true),
    BeforeTCBlockEnd("Before Test Case Block end", false),

    // actions from this block will be executed _after_every_ test case (as @After)
    AfterTCBlockStart("After Test Case Block start", true),
    AfterTCBlockEnd("After Test Case Block end", false),

    // actions from this block can be substituted to test case by 'Include block' action
    BlockStart("Block start", true),
    BlockEnd("Block end", false),

    // actions from this block will be executed _before_ first test case in matrix (as @BeforeClass)
    // executed as separate test case
    FirstBlockStart("First Block start", true),
    FirstBlockEnd("First Block end", false),

    // actions from this block will be executed _after_ last test case in matrix (as @AfterClass)
    // executed as separate test case
    LastBlockStart("Last Block start", true),
    LastBlockEnd("Last Block end", false);

    private final String blockBrace;
    private final boolean start;

    AMLBlockBrace(String s, boolean isStart) {
        this.blockBrace = s;
        this.start = isStart;//blockBrace.endsWith("start");
    }

    public String getName() {
        return this.blockBrace;
    }

    public boolean isStart() {
		return start;
	}

	public static AMLBlockBrace value(String key) {
	    if(key == null) {
	        return null;
	    }

        for (AMLBlockBrace c : AMLBlockBrace.values()) {
            if (c.getName().equalsIgnoreCase(key)) {
                return c;
            }
        }

        return null;
    }

    public AMLBlockBrace getInversed() {
        switch (this) {
            case TestCaseStart:
                return TestCaseEnd;
            case TestCaseEnd:
                return TestCaseStart;
            case GlobalBlockStart:
                return GlobalBlockEnd;
            case GlobalBlockEnd:
                return GlobalBlockStart;
            case BeforeTCBlockStart:
                return BeforeTCBlockEnd;
            case BeforeTCBlockEnd:
                return BeforeTCBlockStart;
            case AfterTCBlockStart:
                return AfterTCBlockEnd;
            case AfterTCBlockEnd:
                return AfterTCBlockStart;
            case BlockStart:
                return BlockEnd;
            case BlockEnd:
                return BlockStart;
            case FirstBlockStart:
                return FirstBlockEnd;
            case FirstBlockEnd:
                return FirstBlockStart;
            case LastBlockStart:
                return LastBlockEnd;
            case LastBlockEnd:
                return LastBlockStart;
        }

        throw new IllegalStateException("Unknown enumeration " + this);
    }

    @Override
    public String toString() {
        return this.blockBrace;
    }
}
