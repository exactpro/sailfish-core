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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.util.AbstractTest;
import com.google.common.collect.ImmutableList;

public class LegReorderTest extends AbstractTest {

	private final static String NS = "namespace";

	@SuppressWarnings("unchecked")
	@Test
	public void testExactLegMatch() {
        ComparatorSettings settings = new ComparatorSettings();

		// Build Message:
		IMessage msg_leg1 = fromString("leg", "FLD_1=10|FLD_2=20|");
		IMessage msg_leg2 = fromString("leg", "FLD_1=30|FLD_2=40|");
		IMessage message = fromString("name", "FIELD=ABC|", "LEGS", asList(msg_leg1, msg_leg2));

		// Build Filter
		IMessage filter_leg1 = fromString("leg", "FLD_1=10|FLD_2=20|");
		IMessage filter_leg2 = fromString("leg", "FLD_1=30|FLD_2=40|");
		IMessage filter = fromString("name", null, "LEGS", asList(filter_leg1, filter_leg2));


		IMessage result = LegReorder.reorder(message, filter, settings);
		List<IMessage> result_legs = (List<IMessage>) result.getField("LEGS");
        Assert.assertEquals(filter_leg1.<Object>getField("FLD_1"), result_legs.get(0).<Object>getField("FLD_1"));
        Assert.assertEquals(filter_leg1.<Object>getField("FLD_2"), result_legs.get(0).<Object>getField("FLD_2"));
        Assert.assertEquals(filter_leg2.<Object>getField("FLD_1"), result_legs.get(1).<Object>getField("FLD_1"));
        Assert.assertEquals(filter_leg2.<Object>getField("FLD_2"), result_legs.get(1).<Object>getField("FLD_2"));

		// Filter with swapped legs
		IMessage filter_swap = fromString("name", null, "LEGS", asList(filter_leg2, filter_leg1));

		IMessage result_swapped = LegReorder.reorder(message, filter_swap, settings);
		List<IMessage> result_legs_swapped = (List<IMessage>) result_swapped.getField("LEGS");
        Assert.assertEquals(filter_leg2.<Object>getField("FLD_1"), result_legs_swapped.get(0).<Object>getField("FLD_1"));
        Assert.assertEquals(filter_leg2.<Object>getField("FLD_2"), result_legs_swapped.get(0).<Object>getField("FLD_2"));
        Assert.assertEquals(filter_leg1.<Object>getField("FLD_1"), result_legs_swapped.get(1).<Object>getField("FLD_1"));
        Assert.assertEquals(filter_leg1.<Object>getField("FLD_2"), result_legs_swapped.get(1).<Object>getField("FLD_2"));

        Assert.assertEquals(message.<Object>getField("FIELD"), result_swapped.<Object>getField("FIELD"));
	}

	static List<IMessage> asList(IMessage...msgs) {
		// throw UnsupportedOperationException on Leg's array modification
		return ImmutableList.copyOf(msgs);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSortByNestedLeg() {
        ComparatorSettings settings = new ComparatorSettings();

		// Build Message:
		IMessage msg_leg1 = fromString("leg", "FLD_1=10|FLD_2=20|", "NESTED", fromString("leg", "FLD_3=50|FLD_4=60|"));
		IMessage msg_leg2 = fromString("leg", "FLD_1=30|FLD_2=40|", "NESTED", fromString("leg", "FLD_3=70|FLD_4=80|"));
		IMessage message = fromString("name", "FIELD=value|", "LEGS", asList(msg_leg1, msg_leg2));

		// Build Filter: Here we sort by nested legs
		IMessage filter_leg1 = fromString("leg", null, "NESTED", fromString("leg", "FLD_3=50|FLD_4=60|"));
		IMessage filter_leg2 = fromString("leg", null, "NESTED", fromString("leg", "FLD_3=70|FLD_4=80|"));
		IMessage filter = fromString("name", null, "LEGS", asList(filter_leg1, filter_leg2));

		IMessage result = LegReorder.reorder(message, filter, settings);
		List<IMessage> result_legs = (List<IMessage>) result.getField("LEGS");
        Assert.assertEquals(msg_leg1.<Object>getField("FLD_1"), result_legs.get(0).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg1.<Object>getField("FLD_2"), result_legs.get(0).<Object>getField("FLD_2"));
		Assert.assertEquals("FLD_3=50|FLD_4=60", result_legs.get(0).getField("NESTED").toString());
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_1"), result_legs.get(1).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_2"), result_legs.get(1).<Object>getField("FLD_2"));
		Assert.assertEquals("FLD_3=70|FLD_4=80", result_legs.get(1).getField("NESTED").toString());

		// Filter with swapped legs
		IMessage filter_swap = fromString("name", null, "LEGS", asList(filter_leg2, filter_leg1));

		IMessage result_swap = LegReorder.reorder(message, filter_swap, settings);
		List<IMessage> result_legs_swapped = (List<IMessage>) result_swap.getField("LEGS");
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_1"), result_legs_swapped.get(0).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_2"), result_legs_swapped.get(0).<Object>getField("FLD_2"));
		Assert.assertEquals("FLD_3=70|FLD_4=80", result_legs_swapped.get(0).getField("NESTED").toString());
        Assert.assertEquals(msg_leg1.<Object>getField("FLD_1"), result_legs_swapped.get(1).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg1.<Object>getField("FLD_2"), result_legs_swapped.get(1).<Object>getField("FLD_2"));
		Assert.assertEquals("FLD_3=50|FLD_4=60", result_legs_swapped.get(1).getField("NESTED").toString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSortNestedLeg() {
        ComparatorSettings settings = new ComparatorSettings();

		// Build Message:
		IMessage msg_leg1 = fromString("leg", "FLD_1=10|FLD_2=20|", "NESTED", asList(fromString("leg", "FLD_3=50|FLD_4=60|"), fromString("leg", "FLD_3=70|FLD_4=80|")));
		IMessage msg_leg2 = fromString("leg", "FLD_1=30|FLD_2=40|",	"NESTED", asList(fromString("leg", "FLD_3=90|FLD_4=100|"), fromString("leg", "FLD_3=110|FLD_4=120|")));
		IMessage message = fromString("name", "FIELD=value|", "LEGS", asList(msg_leg1, msg_leg2));

		// Build Filter: Here we sort by nested legs. Also we will check order of sub-legs
		IMessage filter_leg1 = fromString("leg", null, "NESTED", asList(fromString("leg", "FLD_3=90|"), fromString("leg", "FLD_3=110|")));
		IMessage filter_leg2 = fromString("leg", null, "NESTED", asList(fromString("leg", "FLD_3=70|")));
		IMessage filter = fromString("name", null, "LEGS", asList(filter_leg1, filter_leg2));

		IMessage result = LegReorder.reorder(message, filter, settings);
		List<IMessage> result_legs = (List<IMessage>) result.getField("LEGS");
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_1"), result_legs.get(0).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_2"), result_legs.get(0).<Object>getField("FLD_2"));
		Assert.assertEquals("[FLD_3=90|FLD_4=100, FLD_3=110|FLD_4=120]", result_legs.get(0).getField("NESTED").toString());
        Assert.assertEquals(msg_leg1.<Object>getField("FLD_1"), result_legs.get(1).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg1.<Object>getField("FLD_2"), result_legs.get(1).<Object>getField("FLD_2"));
		Assert.assertEquals("[FLD_3=70|FLD_4=80, FLD_3=50|FLD_4=60]", result_legs.get(1).getField("NESTED").toString());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSortNestedLegByIFilter() {
        ComparatorSettings settings = new ComparatorSettings();

		// Build Message:
		IMessage msg_leg1 = fromString("leg", "FLD_1=10|FLD_2=20|", "NESTED", asList(fromString("leg", "FLD_3=50|FLD_4=60|"), fromString("leg", "FLD_3=70|FLD_4=80|")));
		IMessage msg_leg2 = fromString("leg", "FLD_1=30|FLD_2=40|",	"NESTED", asList(fromString("leg", "FLD_4=100|"), fromString("leg", "FLD_3=110|FLD_4=120|")));
		IMessage message = fromString("name", "FIELD=value|", "LEGS", asList(msg_leg1, msg_leg2));

		// Build Filter: Here we sort by nested legs. Also we will check order of sub-legs
		IMessage filter_field = fromString("leg", null);
		filter_field.addField("FLD_3", StaticUtil.simpleFilter(0, null, "\"110\""));
		IMessage filter = fromString("name", null, "LEGS", asList(fromString("leg", null, "NESTED", asList(filter_field))));

		IMessage result = LegReorder.reorder(message, filter, settings);
		List<IMessage> result_legs = (List<IMessage>) result.getField("LEGS");
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_1"), result_legs.get(0).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_2"), result_legs.get(0).<Object>getField("FLD_2"));
		Assert.assertEquals("[FLD_3=110|FLD_4=120, FLD_4=100]", result_legs.get(0).getField("NESTED").toString());

        Assert.assertEquals(msg_leg1.<Object>getField("FLD_1"), result_legs.get(1).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg1.<Object>getField("FLD_2"), result_legs.get(1).<Object>getField("FLD_2"));
		Assert.assertEquals("[FLD_3=50|FLD_4=60, FLD_3=70|FLD_4=80]", result_legs.get(1).getField("NESTED").toString());

		// change order of legs:
		message = fromString("name", "FIELD=value|", "LEGS", asList(msg_leg2, msg_leg1));
		result = LegReorder.reorder(message, filter, settings);
		result_legs = (List<IMessage>) result.getField("LEGS");
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_1"), result_legs.get(0).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg2.<Object>getField("FLD_2"), result_legs.get(0).<Object>getField("FLD_2"));
		Assert.assertEquals("[FLD_3=110|FLD_4=120, FLD_4=100]", result_legs.get(0).getField("NESTED").toString());

        Assert.assertEquals(msg_leg1.<Object>getField("FLD_1"), result_legs.get(1).<Object>getField("FLD_1"));
        Assert.assertEquals(msg_leg1.<Object>getField("FLD_2"), result_legs.get(1).<Object>getField("FLD_2"));
		Assert.assertEquals("[FLD_3=50|FLD_4=60, FLD_3=70|FLD_4=80]", result_legs.get(1).getField("NESTED").toString());
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


	private static List<String> pareseKV(String str) {
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
