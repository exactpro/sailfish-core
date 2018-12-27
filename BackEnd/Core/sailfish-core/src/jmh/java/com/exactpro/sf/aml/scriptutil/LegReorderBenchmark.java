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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;

public class LegReorderBenchmark {

	// HOW TO RUN:
	// $ gradle jmhJar
	// $ java -jar build/libs/testtools-std-1.0-SNAPSHOT-jmh.jar

	final static String NS = "namespace";

	@State(Scope.Thread)
	public static class BMState {

		public IMessage message;
		public IMessage filter;
        public ComparatorSettings settings;

		@Setup
		public void init() {
            this.settings = new ComparatorSettings();

			// Build Message:
			IMessage msg_leg1 = fromString("leg", "FLD_1=10|FLD_2=20|", "NESTED", asList(fromString("leg", "FLD_3=50|FLD_4=60|"), fromString("leg", "FLD_3=70|FLD_4=80|")));
			IMessage msg_leg2 = fromString("leg", "FLD_1=30|FLD_2=40|",	"NESTED", asList(fromString("leg", "FLD_4=100|"), fromString("leg", "FLD_3=110|FLD_4=120|")));
			this.message = fromString("name", "FIELD=value|", "LEGS", asList(msg_leg1, msg_leg2));

			// Build Filter: Here we sort by nested legs. Also we will check order of sub-legs
			IMessage filter_field = new MapMessage(NS, "leg");
			filter_field.addField("FLD_3", StaticUtil.simpleFilter(0, null, "110"));
			this.filter = fromString("name", null, "LEGS", asList(fromString("leg", null, "NESTED", asList(filter_field))));
		}


	}

	@Benchmark
	@BenchmarkMode(Mode.Throughput)
	public IMessage benchmarkWithIFilter(BMState state) {
		return LegReorder.reorder(state.message.cloneMessage(), state.filter, state.settings);
	}

	// 'FIELDS=some field|FLD_1=10|FLD_2=20|' -> IMessage
	public static IMessage fromString(String name, String str, Object...nested) {
		MapMessage result = new MapMessage(NS, name);

		if (str != null) {
			while (str.length() != 0) {
				List<String> pair = pareseKV(str);

				String key = pair.get(0);
				String value = pair.get(1);

				str = str.substring(key.length() + value.length() + 2); // + '|' + '='

				result.addField(key.trim(), value);
			}
		}

		if (nested != null) {
			if (nested.length % 2 == 1) {
				throw new IllegalArgumentException();
			}

			int i = 0;
			while (i < nested.length) {
				if (!(nested[i] instanceof String)) {
					throw new IllegalArgumentException("Argument #" +(i+3) + " is not String");
				}
				result.addField((String) nested[i], nested[i + 1]);
				i += 2;
			}
		}


		return result;
	}

	public static List<IMessage> asList(IMessage...msgs) {
		return new ArrayList<>(Arrays.asList(msgs));
	}

	static List<String> pareseKV(String str) {
		boolean hasEquals = false;
		String key = "";
		String value = "";
		for (char c : str.toCharArray()) {
			if (c == '|') {
				return Arrays.asList(key, value);
			}
			if (c == '=') {
				hasEquals = true;
				continue;
			}
			if (hasEquals) {
				value += c;
			} else {
				key += c;
			}
		}

		return Arrays.asList(key, value);
	}
}
