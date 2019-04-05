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

    private final MathUtil mathUtil = new MathUtil();

    @Test
    public void TestMin() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(10d, mathUtil.min(11d, 10d, 13d));
    }

    @Test
    public void TestMax() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(13d, mathUtil.max(11d, 10d, 13d));
    }

    @Test
    public void TestAbs() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Double(11), (Double)mathUtil.abs(-11d));
    }

    @Test
    public void TestFloor() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Double(-11), (Double)mathUtil.floor(-11d));
    }

    @Test
    public void TestMinDouble() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Double(-20), (Double)mathUtil.minDouble(-11d, -10d, -20d));
    }

    @Test
    public void TestMaxDouble() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Double(-10), (Double)mathUtil.maxDouble(-11d, -10d, -20d));
    }

    @Test
    public void TestCeil() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Double(-11d), (Double)mathUtil.ceil(-11d));
    }

    @Test
    public void TestMinBigDecimal() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new BigDecimal(10), mathUtil.minBigDecimal(new BigDecimal(10), new BigDecimal(20)));
    }

    @Test
    public void TestMaxBigDecimal() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new BigDecimal(20), mathUtil.maxBigDecimal(new BigDecimal(10), new BigDecimal(20)));
    }

    @Test
    public void TestRoundZero() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Double(0), (Double)mathUtil.roundZero(-11d, 1, 1));
    }

    @Test
    public void TestMinLong() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Long(-20L), (Long)mathUtil.minLong(-11l, -10l, -20l));
    }

    @Test
    public void TestMaxLong() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Long(-10L), (Long)mathUtil.maxLong(-11l, -10l, -20l));
    }

    @Test
    public void TestMinInt() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Integer(-20), (Integer)mathUtil.minInt(-11, -10, -20));
    }

    @Test
    public void TestMaxInt() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Integer(-10), (Integer)mathUtil.maxInt(-11, -10, -20));
    }

    @Test
    public void TestNextUp() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Double(-10.999999999999999), (Double)mathUtil.nextUp(-11d));
    }

    @Test
    public void TestMinChar() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Character('0'), (Character)mathUtil.minChar('1', '0', '2'));
    }

    @Test
    public void TestMaxChar() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(new Character('2'), (Character)mathUtil.maxChar('1', '0', '2'));
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
