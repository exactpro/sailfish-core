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
package com.exactpro.sf.scriptrunner;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityCallException;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityNotFoundException;

public class TestAbstractCaller {
    private UtilityClass util = new UtilityClass();

    @Test
    public void testIntFunc() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(1, util.<Object>call("func", 1));
    }

    @Test
    public void testDoubleFunc() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(1.5, util.<Object>call("func", 1.5));
    }

    @Test
    public void testDoubleFuncWithIntArg() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        Assert.assertEquals(5.0, util.<Object>call("funcDouble", 5));
    }

    @Test
    public void testIntVarArgFunc() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        int[] actual = util.call("funcVarInt", 1, 2, 3);
        int[] expected = { 1, 2, 3 };
        Assert.assertArrayEquals(expected, actual);
    }

    @Test
    public void testDoubleVarArgFunc() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        double[] actual = util.call("funcVarDouble", 1.1, 2.2, 3.3);
        double[] expected = { 1.1, 2.2, 3.3 };
        Assert.assertArrayEquals(expected, actual, 0);
    }

    @Test
    public void testDoubleVarArgFuncWithIntArgs() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        double[] actual = util.call("funcVarDouble", 1, 2, 3);
        double[] expected = { 1, 2, 3 };
        Assert.assertArrayEquals(expected, actual, 0);
    }

    @Test
    public void testVarArgFuncWithSingleArg() throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        int[] actual = util.call("funcVarInt", 1);
        int[] expected = { 1 };
        Assert.assertArrayEquals(expected, actual);
    }

    class UtilityClass extends AbstractCaller {
        @UtilityMethod
        public int func(int arg) {
            return arg;
        }

        @UtilityMethod
        public double func(double arg) {
            return arg;
        }

        @UtilityMethod
        public double funcDouble(double arg) {
            return arg;
        }

        @UtilityMethod
        public int[] funcVarInt(int... args) {
            return args;
        }

        @UtilityMethod
        public double[] funcVarDouble(double... args) {
            return args;
        }
    }
}
