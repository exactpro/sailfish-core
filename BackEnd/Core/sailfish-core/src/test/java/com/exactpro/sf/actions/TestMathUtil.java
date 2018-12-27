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
package com.exactpro.sf.actions;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityCallException;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityNotFoundException;

public class TestMathUtil {

    private MathUtil mathUtil = new MathUtil();

    @Test
    public void TestMin() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("min", 11d, 10d, 13d), mathUtil.min(11d, 10d, 13d));
    }

    @Test
    public void TestAbs() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("abs", -11d), (Double)mathUtil.abs(-11d));
    }

    @Test
    public void TestFloor() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("floor", -11d), (Double)mathUtil.floor(-11d));
    }

    @Test
    public void TestMinDouble() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("minDouble", -11d, -10d, -20d), (Double)mathUtil.minDouble(-11d, -10d, -20d));
    }

    @Test
    public void TestCeil() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("ceil", -11d), (Double)mathUtil.ceil(-11d));
    }

    @Test
    public void TestMinBigDecimal() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("minBigDecimal", new BigDecimal(10), new BigDecimal(20)), mathUtil.minBigDecimal(new BigDecimal(10), new BigDecimal(20)));
    }

    @Test
    public void TestRoundZero() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("roundZero", -11d, 1, 1), (Double)mathUtil.roundZero(-11d, 1, 1));
    }

    @Test
    public void TestMinLong() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("minLong", -11l, -10l, -20l), (Long)mathUtil.minLong(-11l, -10l, -20l));
    }

    @Test
    public void TestMinInt() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("minInt", -11, -10, -20), (Integer)mathUtil.minInt(-11, -10, -20));
    }

    @Test
    public void TestNextUp() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("nextUp", -11d), (Double)mathUtil.nextUp(-11d));
    }

    @Test
    public void TestMinChar() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("minChar", '1', '0', '2'), (Character)mathUtil.minChar('1', '0', '2'));
    }

    @Test
    public void TestRound() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("round", -11d, 1, "HALF_UP"), mathUtil.round(-11d, 1, "HALF_UP"));

        double source = 1.3451;
        BigDecimal decimalSource = BigDecimal.valueOf(source);
        Assert.assertEquals(Double.valueOf(1.35), mathUtil.round(source, 2, "HALF_UP"));
        Assert.assertEquals(Double.valueOf(1.35), mathUtil.round(source, 2, "UP"));
        Assert.assertEquals(Double.valueOf(1.34), mathUtil.round(source, 2, "DOWN"));
        Assert.assertEquals(Double.valueOf(1.35), mathUtil.round(source, 2, "HALF_DOWN"));
        Assert.assertEquals(Double.valueOf(1.35), mathUtil.round(source, 2, "CEILING"));
        Assert.assertEquals(Double.valueOf(1.34), mathUtil.round(source, 2, "FLOOR"));

        Assert.assertEquals(BigDecimal.valueOf(1.35), mathUtil.round(decimalSource, 2, "HALF_UP"));
        Assert.assertEquals(BigDecimal.valueOf(1.35), mathUtil.round(decimalSource, 2, "UP"));
        Assert.assertEquals(BigDecimal.valueOf(1.34), mathUtil.round(decimalSource, 2, "DOWN"));
        Assert.assertEquals(BigDecimal.valueOf(1.35), mathUtil.round(decimalSource, 2, "HALF_DOWN"));
        Assert.assertEquals(BigDecimal.valueOf(1.35), mathUtil.round(decimalSource, 2, "CEILING"));
        Assert.assertEquals(BigDecimal.valueOf(1.34), mathUtil.round(decimalSource, 2, "FLOOR"));

        Assert.assertEquals(Double.valueOf(1.345), mathUtil.round(source, 3, "HALF_UP"));
        Assert.assertEquals(Double.valueOf(1.346), mathUtil.round(source, 3, "UP"));
        Assert.assertEquals(Double.valueOf(1.345), mathUtil.round(source, 3, "DOWN"));
        Assert.assertEquals(Double.valueOf(1.345), mathUtil.round(source, 3, "HALF_DOWN"));
        Assert.assertEquals(Double.valueOf(1.346), mathUtil.round(source, 3, "CEILING"));
        Assert.assertEquals(Double.valueOf(1.345), mathUtil.round(source, 3, "FLOOR"));

        Assert.assertEquals(BigDecimal.valueOf(1.345), mathUtil.round(decimalSource, 3, "HALF_UP"));
        Assert.assertEquals(BigDecimal.valueOf(1.346), mathUtil.round(decimalSource, 3, "UP"));
        Assert.assertEquals(BigDecimal.valueOf(1.345), mathUtil.round(decimalSource, 3, "DOWN"));
        Assert.assertEquals(BigDecimal.valueOf(1.345), mathUtil.round(decimalSource, 3, "HALF_DOWN"));
        Assert.assertEquals(BigDecimal.valueOf(1.346), mathUtil.round(decimalSource, 3, "CEILING"));
        Assert.assertEquals(BigDecimal.valueOf(1.345), mathUtil.round(decimalSource, 3, "FLOOR"));

        source = -1.3451;
        decimalSource = BigDecimal.valueOf(source);
        Assert.assertEquals(Double.valueOf(-1.35), mathUtil.round(source, 2, "HALF_UP"));
        Assert.assertEquals(Double.valueOf(-1.35), mathUtil.round(source, 2, "UP"));
        Assert.assertEquals(Double.valueOf(-1.34), mathUtil.round(source, 2, "DOWN"));
        Assert.assertEquals(Double.valueOf(-1.35), mathUtil.round(source, 2, "HALF_DOWN"));
        Assert.assertEquals(Double.valueOf(-1.34), mathUtil.round(source, 2, "CEILING"));
        Assert.assertEquals(Double.valueOf(-1.35), mathUtil.round(source, 2, "FLOOR"));

        Assert.assertEquals(BigDecimal.valueOf(-1.35), mathUtil.round(decimalSource, 2, "HALF_UP"));
        Assert.assertEquals(BigDecimal.valueOf(-1.35), mathUtil.round(decimalSource, 2, "UP"));
        Assert.assertEquals(BigDecimal.valueOf(-1.34), mathUtil.round(decimalSource, 2, "DOWN"));
        Assert.assertEquals(BigDecimal.valueOf(-1.35), mathUtil.round(decimalSource, 2, "HALF_DOWN"));
        Assert.assertEquals(BigDecimal.valueOf(-1.34), mathUtil.round(decimalSource, 2, "CEILING"));
        Assert.assertEquals(BigDecimal.valueOf(-1.35), mathUtil.round(decimalSource, 2, "FLOOR"));

        Assert.assertEquals(Double.valueOf(-1.345), mathUtil.round(source, 3, "HALF_UP"));
        Assert.assertEquals(Double.valueOf(-1.346), mathUtil.round(source, 3, "UP"));
        Assert.assertEquals(Double.valueOf(-1.345), mathUtil.round(source, 3, "DOWN"));
        Assert.assertEquals(Double.valueOf(-1.345), mathUtil.round(source, 3, "HALF_DOWN"));
        Assert.assertEquals(Double.valueOf(-1.345), mathUtil.round(source, 3, "CEILING"));
        Assert.assertEquals(Double.valueOf(-1.346), mathUtil.round(source, 3, "FLOOR"));

        Assert.assertEquals(BigDecimal.valueOf(-1.345), mathUtil.round(decimalSource, 3, "HALF_UP"));
        Assert.assertEquals(BigDecimal.valueOf(-1.346), mathUtil.round(decimalSource, 3, "UP"));
        Assert.assertEquals(BigDecimal.valueOf(-1.345), mathUtil.round(decimalSource, 3, "DOWN"));
        Assert.assertEquals(BigDecimal.valueOf(-1.345), mathUtil.round(decimalSource, 3, "HALF_DOWN"));
        Assert.assertEquals(BigDecimal.valueOf(-1.345), mathUtil.round(decimalSource, 3, "CEILING"));
        Assert.assertEquals(BigDecimal.valueOf(-1.346), mathUtil.round(decimalSource, 3, "FLOOR"));
    }

    @Test
    public void TestRoundUp() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(mathUtil.call("roundUp", -1.001), (Integer)mathUtil.roundUp(-1.001d));

        Assert.assertEquals(-2, mathUtil.roundUp(-1.001d));
        Assert.assertEquals(2, mathUtil.roundUp(1.001d));
    }
}
