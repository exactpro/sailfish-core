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
package com.exactpro.sf.comparison;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.aml.scriptutil.StaticUtil;

public class MessageComparatorBenchmark {
    public static void main(String[] args) {
        ComplexFilterState state = new ComplexFilterState();

        state.init();

        while(true) {
            MessageComparator.compare(state.actual, state.expected, state.settings);
        }
    }

    @State(Scope.Thread)
    public static abstract class AbstractState {
        public IMessage actual;
        public IMessage expected;
        public ComparatorSettings settings;

        @Setup(Level.Trial)
        public void init() {
            File dictionaryFile = new File("src/main/workspace/cfg/dictionaries/test_aml.xml");
            IDictionaryStructure dictionary;

            try(InputStream dictionaryStream = new FileInputStream(dictionaryFile)) {
                dictionary = new XmlDictionaryStructureLoader().load(dictionaryStream);
            } catch(IOException e) {
                throw new EPSCommonException(e);
            }

            actual = createActualMessage();
            expected = createExpectedMessage();
            settings = new ComparatorSettings().setDictionaryStructure(dictionary);
        }

        public abstract IMessage createActualMessage();

        public IMessage createExpectedMessage() {
            return createActualMessage();
        }
    }

    public static class SimpleState extends AbstractState {
        @Override
        public IMessage createActualMessage() {
            IMessage message = new MapMessage("TestAML", "SimpleMessage");

            message.addField("FBoolean", true);
            message.addField("FByte", (byte)1);
            message.addField("FCharacter", 'A');
            message.addField("FShort", (short)1);
            message.addField("FInteger", 1);
            message.addField("FLong", 1L);
            message.addField("FFloat", 1f);
            message.addField("FDouble", 1d);
            message.addField("FBigDecimal", BigDecimal.ONE);
            message.addField("FString", "abc");

            return message;
        }
    }

    public static class FilterState extends SimpleState {
        @Override
        public IMessage createExpectedMessage() {
            IMessage message = new MapMessage("TestAML", "SimpleMessage");

            message.addField("FBoolean", StaticUtil.simpleFilter(0, null, "true"));
            message.addField("FByte", StaticUtil.simpleFilter(0, null, "(byte)1"));
            message.addField("FCharacter", StaticUtil.simpleFilter(0, null, "'A'.charAt(0)"));
            message.addField("FShort", StaticUtil.simpleFilter(0, null, "(short)1"));
            message.addField("FInteger", StaticUtil.simpleFilter(0, null, "1"));
            message.addField("FLong", StaticUtil.simpleFilter(0, null, "1L"));
            message.addField("FFloat", StaticUtil.simpleFilter(0, null, "1f"));
            message.addField("FDouble", StaticUtil.simpleFilter(0, null, "1d"));
            message.addField("FBigDecimal", StaticUtil.simpleFilter(0, null, "BigDecimal.ONE"));
            message.addField("FString", StaticUtil.simpleFilter(0, null, "'abc'"));

            return message;
        }
    }

    public static class ComplexState extends SimpleState {
        @Override
        public IMessage createActualMessage() {
            IMessage message = new MapMessage("TestAML", "ArrayMessage");

            message.addField("BooleanArray", Arrays.asList(true, false, true, false));
            message.addField("IntegerArray", Arrays.asList(1, 2, 3, 4));
            message.addField("DoubleArray", Arrays.asList(1d, 2d, 3d, 4d));
            message.addField("BigDecimalArray", Arrays.asList(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN));
            message.addField("StringArray", Arrays.asList("asd", "sdf", "dfg", "fgh"));
            message.addField("MessageArray", Arrays.asList(super.createActualMessage(), super.createActualMessage()));

            return message;
        }
    }

    public static class ComplexFilterState extends ComplexState {
        @Override
        public IMessage createExpectedMessage() {
            IMessage message = createActualMessage();
            IMessage filter = new FilterState() {{ init(); }}.createExpectedMessage();
            message.addField("MessageArray", Arrays.asList(filter, filter));
            return message;
        }
    }

    private ComparisonResult benchmark(AbstractState state, Blackhole blackhole) {
        return MessageComparator.compare(state.actual, state.expected, state.settings);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmarkSimpleMessage(SimpleState state, Blackhole blackhole) {
        blackhole.consume(benchmark(state, blackhole));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmarkSimpleFilterMessage(FilterState state, Blackhole blackhole) {
        blackhole.consume(benchmark(state, blackhole));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmarkComplexMessage(ComplexState state, Blackhole blackhole) {
        blackhole.consume(benchmark(state, blackhole));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmarkComplexFilterMessage(ComplexFilterState state, Blackhole blackhole) {
        blackhole.consume(benchmark(state, blackhole));
    }
}
