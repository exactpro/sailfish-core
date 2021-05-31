/*******************************************************************************
 *   Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.services.fix;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.exactpro.sf.util.AbstractTest;

import quickfix.EvolutionQFJMessage;
import quickfix.InvalidMessage;

@RunWith(Enclosed.class)
public class EvolutionQFJMessageTest {
    @RunWith(Parameterized.class)
    public static class EvolutionQFJMessageNegativeTest extends AbstractTest {
        @Parameters(name = "Name={0}")
        public static Iterable<Object[]> parameters() {
            return Arrays.asList(
                    new Object[][] {
                            { "testInvalidMessageNo9Tag",
                                    "8=FIX.4.4|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|",
                                    "Not valid header: 8=FIX.4.4\u000135=8, missing 9 tag. Valid tag values: 8, 9, 35." },
                            { "testInvalidMessageNo35Tag",
                                    "8=FIX.4.4|9=289|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|",
                                    "Not valid message: 8=FIX.4.4\u00019=289\u000134=1090\u000149=TESTSELL1\u000152=20180920-18:23:53.671\u000156=TESTBUY1\u00016=113.35\u000111=636730640278898634\u000114=3500.0000000000\u000115=USD\u000117=20636730646335310000\u000121=2\u000131=113.35\u000132=3500\u000137=20636730646335310000\u000138=7000\u000139=1\u000140=1\u000154=1\u000155=MSFT\u000160=20180920-18:23:53.531\u0001150=F\u0001151=3500\u0001453=1\u0001448=BRK2\u0001447=D\u0001452=1\u000110=151\u0001, missing tag 35" },
                            { "testInavalidMessageExtraTagHeader",
                                    "8=FIX.4.4|34=1090|9=289|35=8|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|",
                                    "Header contains invalid tag 34. Valid tag values: 8, 9, 35." },
                            { "testInvalidMessageNotTrailer",
                                    "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|",
                                    "Bad tag format:  .For input string: \"\"" },
                            { "testInvalidMessageNotTrailer2",
                                    "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=12|",
                                    "Trailer contains invalid tag 452. Valid tag value: 10." },
                    }
            );
        }

        public static final String EXPECTED_EXCEPTION_INVALID_MESSAGE = "Expected exception InvalidMessage";
        private final String name;
        private final String message;
        private final String error;

        public EvolutionQFJMessageNegativeTest(String name, String message, String error) {
            this.name = name;
            this.message = message;
            this.error = error;
        }

        @Rule
        public ExpectedException thrown = ExpectedException.none();

        @Test
        public void testInvalidMessage() throws InvalidMessage {
            String messageFix = message.replace("|", "\001");
            byte[] array = messageFix.getBytes();
            thrown.expect(InvalidMessage.class);
            thrown.expectMessage(error);
            EvolutionQFJMessage msg = new EvolutionQFJMessage(array);
        }
    }

    public static class EvolutionQFJMessagePositiveTest extends AbstractTest {
        @Test
        public void testFromArray() throws InvalidMessage {
            String message = "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|"
                    .replace("|", "\001");
            byte[] array = message.getBytes();
            String msg = new EvolutionQFJMessage(array).toString();
            Assert.assertEquals(msg, message);
        }

        @Test
        public void testMessageSwapped8And9Tags() throws InvalidMessage {
            String swappedTagsMessage = "9=289|8=FIX.4.4|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|"
                    .replace("|", "\001");
            String validMessage = "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|"
                    .replace("|", "\001");
            byte[] array = swappedTagsMessage.getBytes();
            String msg = new EvolutionQFJMessage(array).toString();
            Assert.assertEquals(msg, validMessage);
        }
    }
}




