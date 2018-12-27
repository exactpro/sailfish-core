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
package com.exactpro.sf.common.profiler;

import org.junit.Test;


public class ProfilerTest {
    private static final String trace1 = "1";
    private static final String trace2 = "2";
    public static final String[] tracesNames = new String[]{trace1, trace2};

    @Test
    public void simpleTest() throws InterruptedException {
        Profiler.getInstance().startTrace(tracesNames);
        Thread.sleep(100);
        m1();
        Profiler.getInstance().endTrace(trace1);
        Thread.sleep(100);
        m2();
        Thread.sleep(1000);
        Profiler.getInstance().endTrace(trace2);
        //Profiler.getInstance().endTraces(tracesNames);
    }

    private void m1() throws InterruptedException {
        Profiler.getInstance().startMethod(tracesNames);
        Thread.sleep(1000);
        Profiler.getInstance().stopMethod(tracesNames);
    }

    private void m2() throws InterruptedException {
        Profiler.getInstance().startMethod(trace2);
        Thread.sleep(10);
        Profiler.getInstance().stopMethod(trace2);
    }
}
