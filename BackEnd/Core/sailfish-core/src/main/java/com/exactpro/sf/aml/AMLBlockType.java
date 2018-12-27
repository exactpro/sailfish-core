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

public enum AMLBlockType {
    TestCase("Test Case", AMLBlockBrace.TestCaseStart, AMLBlockBrace.TestCaseEnd),
    GlobalBlock("Global Block", AMLBlockBrace.GlobalBlockStart, AMLBlockBrace.GlobalBlockEnd),
    BeforeTCBlock("Before Test Case Block", AMLBlockBrace.BeforeTCBlockStart, AMLBlockBrace.BeforeTCBlockEnd),
    AfterTCBlock("After Test Case Block", AMLBlockBrace.AfterTCBlockStart, AMLBlockBrace.AfterTCBlockEnd),
    Block("Block", AMLBlockBrace.BlockStart, AMLBlockBrace.BlockEnd),
    FirstBlock("First Block", AMLBlockBrace.FirstBlockStart, AMLBlockBrace.FirstBlockEnd),
    LastBlock("Last Block", AMLBlockBrace.LastBlockStart, AMLBlockBrace.LastBlockEnd);

    private final String name;
    private final AMLBlockBrace openingBrace;
    private final AMLBlockBrace closingBrace;

    private AMLBlockType(String name, AMLBlockBrace openingBrace, AMLBlockBrace closingBrace) {
        this.name = name;
        this.openingBrace = openingBrace;
        this.closingBrace = closingBrace;
    }

    public String getName() {
        return name;
    }

    public AMLBlockBrace getOpeningBrace() {
        return openingBrace;
    }

    public AMLBlockBrace getClosingBrace() {
        return closingBrace;
    }

    public static AMLBlockType value(String value) {
        if(value == null) {
            return null;
        }

        return value(AMLBlockBrace.value(value));
    }

    public static AMLBlockType value(AMLBlockBrace blockBrace) {
        if(blockBrace == null) {
            return null;
        }

        for(AMLBlockType blockType : AMLBlockType.values()) {
            if(blockType.openingBrace == blockBrace || blockType.closingBrace == blockBrace) {
                return blockType;
            }
        }

        return null;
    }
}
