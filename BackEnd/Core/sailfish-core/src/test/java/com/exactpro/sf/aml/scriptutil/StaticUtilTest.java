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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.Convention;

public class StaticUtilTest {

    @Test
    public void testRemoveUtilityCall() {
        Assert.assertEquals("plugin:class.function().Bug( \"bug_text_1\" ,  \"actual_clordid\" ).Check(x) ",
                StaticUtil.removeUtilityCall("um.call(SailfishURI.parse(\"plugin:class.function\")).Bug( \"bug_text_1\" ,  \"actual_clordid\" ).Check(x) "));
        Assert.assertEquals("plugin:class.function(2).Bug( \"bug_text_1\" ,  \"actual_clordid\" ).Check(x) ",
                StaticUtil.removeUtilityCall("um.call(SailfishURI.parse(\"plugin:class.function\"), 2).Bug( \"bug_text_1\" ,  \"actual_clordid\" ).Check(x) "));
        Assert.assertEquals("plugin:class.function(plugin:class.function(\"actual_clordid\"  + 1)).Bug( \"bug_text_1\" ,  \"actual_clordid\" ).Check(x) ",
                StaticUtil.removeUtilityCall("um.call(SailfishURI.parse(\"plugin:class.function\"), um.call(SailfishURI.parse(\"plugin:class.function\"), \"actual_clordid\"  + 1)).Bug( \"bug_text_1\" ,  \"actual_clordid\" ).Check(x) "));
    }

    @Test
    public void testSimpleFilterWithInsertMvel() throws Exception {
        IMessage refMessage = new MapMessage("namespace1", "name1");
        // values stored in HashMap. Add some amount of fields
        refMessage.addField("value", "1");
        refMessage.addField("valu", "2");
        refMessage.addField("val", "3");
        refMessage.addField("va", "4");
        refMessage.addField("v", "5");

        IMessage iMessage = new MapMessage("namespace", "name");
        iMessage.addField("ref", refMessage);

        StaticUtil.IFilter filter = StaticUtil.filter(0, null, "mes.ref.value + mes.ref.valu + mes.ref.val + mes.ref.va + mes.ref.v > x", "mes", iMessage);
        String condition = filter.getCondition();

        Assert.assertEquals("\"1\" + \"2\" + \"3\" + \"4\" + \"5\" > x", condition);
    }

    @Test
    public void testSimpleFilterWithInsertMvel_2() throws Exception {
    	List<IMessage> lst = new ArrayList<>();
    	for (int i=0; i<5; i++) {
    		IMessage msg = new MapMessage("namespace", "name");
    		msg.addField("val", i);
    		lst.add(msg);
    	}
        IMessage iMessage = new MapMessage("namespace", "name");
		iMessage.addField("ref", lst);

		StaticUtil.IFilter filter = StaticUtil.filter(0, null, "mes.ref[0].val + mes.ref[3].val > x", "mes", iMessage);
        String condition = filter.getCondition();

        Assert.assertEquals("0 + 3 > x", condition);
    }

    @Test
    public void testSimpleFilterWithInsertMvel_simpleCollections() throws Exception {
    	List<IMessage> lst = new ArrayList<>();
    	for (int i=0; i<5; i++) {
    		IMessage msg = new MapMessage("namespace", "name");
    		msg.addField("val", i);
    		lst.add(msg);
    	}
        IMessage iMessage = new MapMessage("namespace", "name");
		iMessage.addField("ref", Arrays.asList(1, 2, 3, 4, 5));

		StaticUtil.IFilter filter = StaticUtil.filter(0, null, "mes.ref[0] + mes.ref[3] > x", "mes", iMessage);
        String condition = filter.getCondition();

        Assert.assertEquals("1 + 4 > x", condition);
    }
    @Test
    public void testFilter() throws Exception {
        String value = "1";
        IMessage iMessage = new MapMessage("namespace", "name");
        IMessage refMessage = new MapMessage("namespace1", "name1");
        refMessage.addField("val", value);
        iMessage.addField("ref", refMessage);
        StaticUtil.IFilter filter = StaticUtil.filter(0, null, "mes.ref.val", "mes", iMessage);
        String condition = filter.getCondition();
        Assert.assertEquals("\"" + value + "\"", condition);
    }

    @Test
    public void testFilterWithArray() throws Exception {
        String value = "1";
        IMessage refMessage1 = new MapMessage("value", "value");
        refMessage1.addField("val", value);
        IMessage ref = new MapMessage("ref", "ref");
        ref.addField("subRef", Collections.singletonList(refMessage1));
        IMessage mes = new MapMessage("mes", "mes");
        mes.addField("ref", ref);
        StaticUtil.IFilter filter = StaticUtil.filter(0, null, "mes.ref.subRef[0].val", "mes", mes);
        String condition = filter.getCondition();
        Assert.assertEquals("\"" + value + "\"", condition);
    }

    @Test
    public void testFilterHashMap() throws Exception {
        String value = "1";
        HashMap<String, Object> map = new HashMap<>();
        IMessage msg = new MapMessage("namespace", "name");
        msg.addField("val", value);
        map.put("msg", msg);

        StaticUtil.IFilter filter = StaticUtil.filter(0, null, "map.msg.val", "map", map);
        String condition = filter.getCondition();
        Assert.assertEquals("\"" + value + "\"", condition);
    }

    @Test
    public void testFilter_TernaryOperator() throws Exception {
    	// from email 'smth interesting)' from <artem.kuchin@exactprosystems.com> Jul 10 2015
        Integer value = 123;
        IMessage request = new MapMessage("namespace", "name");
        request.addField("ReceivedMarketDepth", value);
        StaticUtil.IFilter filter = null;

        filter = StaticUtil.filter(
                0,
                null,
        		"v0.ReceivedMarketDepth != '#' ? x == v1.ReceivedMarketDepth : x == null",
        		"v0", request,
        		"v1", request
        );
        Assert.assertEquals("123 != '#' ? x == 123 : x == null", filter.getCondition());

        filter = StaticUtil.filter(
                0,
                null,
        		"v0.ReceivedMarketDepth != '*' ? x == v1.ReceivedMarketDepth : x == null",
        		"v0", request,
        		"v1", request
        );
        Assert.assertEquals("123 != '*' ? x == 123 : x == null", filter.getCondition());
    }

    @Test
    public void testFilter_StringEscape() throws Exception {
    	// from email 'smth interesting)' from <artem.kuchin@exactprosystems.com> Jul 10 2015
        Integer value = 123;
        IMessage request = new MapMessage("namespace", "name");
        request.addField("ReceivedMarketDepth", value);

        StaticUtil.IFilter filter = StaticUtil.filter(
                0,
                null,
        		"v0.ReceivedMarketDepth!='v0.ReceivedMarketDepth'",
        		"v0", request
        );
        Assert.assertEquals("123!='v0.ReceivedMarketDepth'", filter.getCondition());

        filter = StaticUtil.filter(0, null, " \" x \" ");
        Assert.assertEquals(" \" x \" ", filter.getCondition());

        filter = StaticUtil.filter(0, null, "\"x\"");
        Assert.assertEquals("\"x\"", filter.getCondition());

        filter = StaticUtil.filter(0, null, "\" x \"");
        Assert.assertEquals("\" x \"", filter.getCondition());
    }

    @Test
    public void testFilter_UtilFn() throws Exception {
    	// from email 'Trad-x > SF > AML3' from <leonid.samoletov@exactprosystems.com> Jul 14 2015
        Integer value = 123;
        IMessage request = new MapMessage("namespace", "name");
        request.addField("OrderQty", value);

        StaticUtil.IFilter filter = StaticUtil.filter(
                0,
                null,
                "com.exactpro.sf.actions.MathUtil.roundZero(Double.valueOf(s1), x, v0.OrderQty, )",
    			"v0", request,
    			"s1", "123.456"
        );
        Assert.assertEquals("MathUtil.roundZero(Double.valueOf(\"123.456\"), x, 123, )", filter.getCondition());
    }

	@Test
	public void testSimpleFilter_TernaryOperator() throws Exception {
		StaticUtil.IFilter filter = StaticUtil.simpleFilter(0, null, "2==3 ? 3 : 4");
		String condition = filter.getCondition();
		Assert.assertEquals("4", condition);
	}

    @Test
    public void testFilter_IncorrectReference() throws Exception {
        StaticUtil.IFilter filter = StaticUtil.filter(0, null, "v0.ReceivedMarketDepth != 1");
        Assert.assertEquals("v0.ReceivedMarketDepth != 1", filter.getCondition());
    }

    @Test
    public void testNotNullFilterCreationFromValue() {
        IFilter filter = StaticUtil.simpleFilter(0, null, "x", "x", Convention.CONV_PRESENT_OBJECT);
        Assert.assertEquals("com.exactpro.sf.aml.scriptutil.StaticUtil.NotNullFilter", filter.getClass().getCanonicalName());
    }

    @Test
    public void testNullFilterCreationFromValue() {
        IFilter filter = StaticUtil.simpleFilter(0, null, "x", "x", Convention.CONV_MISSED_OBJECT);
        Assert.assertEquals("com.exactpro.sf.aml.scriptutil.StaticUtil.NullFilter", filter.getClass().getCanonicalName());
    }

    @Test
    public void testSimpleFilterPositive() {
        validateSimpleFilter("12", "12");
        validateSimpleFilter("-1", "-1");

        // problem with byte: short, int, long are ok, but byte isn't. WTF ?
        validateSimpleFilter("\"12\"", "12", false, true);
        validateSimpleFilter("\"-1\"", "-1", false, true);

        // problem with float: double, BigDecimal are ok, but float isn't. WTF ?
        validateSimpleFilter("\"-11.01\"", "-11.01", true, true);
        validateSimpleFilter("-11.01", "-11.01", true, true);
    }

    @Test
    public void testNaNAndInfinity() {
        IFilter filter = StaticUtil.simpleFilter(0, null, "Double.NaN");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Double.NaN));
        filter = StaticUtil.simpleFilter(0, null, "Double.POSITIVE_INFINITY");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Double.POSITIVE_INFINITY));
        filter = StaticUtil.simpleFilter(0, null, "Double.NEGATIVE_INFINITY");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Double.NEGATIVE_INFINITY));

        filter = StaticUtil.simpleFilter(0, null, "Float.NaN");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Float.NaN));
        filter = StaticUtil.simpleFilter(0, null, "Float.POSITIVE_INFINITY");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Float.POSITIVE_INFINITY));
        filter = StaticUtil.simpleFilter(0, null, "Float.NEGATIVE_INFINITY");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testRegexFilterPositive() {
        IFilter filter = StaticUtil.regexFilter(0, null, "^Test.*");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate("TestRegExp"));

        filter = StaticUtil.regexFilter(0, null, "^[A-Za-z]{1,20}$");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate("TestRegExp"));

        filter = StaticUtil.regexFilter(0, null, "^[\\-0-9]{0,5}$");
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate("-30"));
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate("00203"));
    }

    private void validateSimpleFilter(String expected, String actual) {
        validateSimpleFilter(expected, actual, false, false);
    }
    private void validateSimpleFilter(String expected, String actual, boolean floatOnly, boolean ignoreProblemTypes) {
        IFilter filter = StaticUtil.simpleFilter(0, null, expected);
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(actual));

        if (!floatOnly) {
            if (!ignoreProblemTypes) {
                Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Byte.valueOf(actual)));
            }
            Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Short.valueOf(actual)));
            Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Integer.valueOf(actual)));
            Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Long.valueOf(actual)));
        }

        if (!ignoreProblemTypes) {
            Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Float.valueOf(actual)));
        }
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(Double.valueOf(actual)));
        Assert.assertEquals(ExpressionResult.EXPRESSION_RESULT_TRUE, filter.validate(new BigDecimal(actual)));
    }
}