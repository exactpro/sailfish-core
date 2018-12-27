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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Profiler {

    private static Logger logger = LoggerFactory.getLogger(Profiler.class);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static Profiler instance;

    private final Map<String, Trace> traceMap;

    public Profiler() {
        traceMap = new ConcurrentHashMap<>();
    }

    public static Profiler getInstance() {
        if (instance == null) {
            synchronized (Profiler.class) {
                if (instance == null) {
                    instance = new Profiler();
                }
            }
        }
        return instance;
    }


    public void startTrace(String... tracesNames) {
        if (tracesNames != null)
            for (String traceName : tracesNames) {
                synchronized (this.traceMap) {
                    if (!this.traceMap.containsKey(traceName)) {
                        Trace trace = new Trace(traceName);
                        trace.startMethod();
                        this.traceMap.put(traceName, trace);
                    } else {
                        logger.debug("Trace {} already started", traceName);
                    }
                }
            }

    }

    public void endTrace(String... tracesNames) {
        if (tracesNames != null)
            for (String traceName : tracesNames) {
                synchronized (this.traceMap) {
                    Trace trace = this.traceMap.remove(traceName);
                    if (trace != null) {
                        trace.stop();
                        trace.stopMethod();
                        logger.debug("{}", trace);
                    } else {
                        logger.debug("Trace {} is absent", traceName);
                    }
                }
            }
    }

    public void startMethod(String... tracesNames) {
        if (tracesNames != null)
            for (String traceName : tracesNames) {
                Trace trace = this.traceMap.get(traceName);
                if (trace != null) {
                    trace.startMethod();
                } else {
                    logger.debug("Trace {} is absent", traceName);
                }
            }

    }

    public void stopMethod(String... tracesNames) {
        if (tracesNames != null)
            for (String traceName : tracesNames) {
                Trace trace = this.traceMap.get(traceName);
                if (trace != null) {
                    trace.stopMethod();
                } else {
                    logger.debug("Trace {} is absent", traceName);
                }
            }
    }

    private class Trace extends Method {
        private final Map<Long, List<Method>> methodMap;

        public Trace(String name) {
            super(name, -1);
            this.methodMap = new HashMap<>();
        }

        public void startMethod() {
            synchronized (this) {
                Thread thread = Thread.currentThread();
                List<Method> methodList = this.methodMap.get(thread.getId());
                if (methodList == null) {
                    methodList = new ArrayList<>();
                    this.methodMap.put(thread.getId(), methodList);
                }
                StackTraceElement stackTraceElement = thread.getStackTrace()[3];
                String name = stackTraceElement.getClassName() + "." +stackTraceElement.getMethodName();
                int index = 0;
                for (int i = methodList.size() - 1; i >= 0; i--) {
                    Method method = methodList.get(i);
                    if (!method.isStopped()) {
                        index = method.getLevel() + 1;
                        break;
                    }
                }
                methodList.add(new Method(name, index));
            }
        }

        public void stopMethod() {
            synchronized (this) {
                Thread thread = Thread.currentThread();
                List<Method> methodList = this.methodMap.get(thread.getId());
                if (methodList != null) {
                    StackTraceElement stackTraceElement = thread.getStackTrace()[3];
                    String name = stackTraceElement.getClassName() + "." +stackTraceElement.getMethodName();
                    for (int i = methodList.size() - 1; i >= 0; i--) {
                        Method method = methodList.get(i);
                        if (!method.isStopped() && method.name.equals(name)) {
                            method.stop();
                            return;
                        }
                    }
                    logger.debug("Method {} is absent", name);
                } else {
                    logger.debug("No one method has not been started in the Thread {}", thread.getName());
                }
            }
        }

        @Override
        public String toString() {
            synchronized (this) {
                StringBuilder builder = new StringBuilder("Trace : ");
                builder.append(this.name);
                builder.append(LINE_SEPARATOR);
                for (Map.Entry<Long, List<Method>> threadList : this.methodMap.entrySet()) {
                    builder.append("Thread : ");
                    builder.append(threadList.getKey());
                    builder.append(LINE_SEPARATOR);
                    for (Method method : threadList.getValue()) {
                        builder.append(String.format("%" + (method.getLevel() + 1) + "s", ""));
                        builder.append(method.toString());
                        builder.append(LINE_SEPARATOR);
                    }
                }

                return builder.toString();
            }
        }
    }

    private class Method {
        protected final StopWatch stopWatch;
        protected final String name;
        private final int level;

        public Method(String name, int level) {
            this.stopWatch = new StopWatch();
            this.stopWatch.start();
            this.name = name;
            this.level = level;
        }

        public void stop() {
            this.stopWatch.stop();
        }

        public boolean isStopped() {
            return this.stopWatch.isStopped();
        }

        public int getLevel() {
            return this.level;
        }

        @Override
        public String toString() {
            if (this.stopWatch.isStopped()) {
                return this.name + " " + this.stopWatch.getTime();
            } else {
                return this.name + " progress ...";
            }
        }
    }
}
