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
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.exactpro.sf.common.messages.IMetadata;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.util.AbstractTest;

import quickfix.EvolutionQFJMessage;
import quickfix.InvalidMessage;

@RunWith(Enclosed.class)
public class EvolutionQFJMessageTest {
    private static final MsgMetaData EMPTY_METADATA = new MsgMetaData(IMetadata.EMPTY);
    @RunWith(Parameterized.class)
    public static class EvolutionQFJMessageNegativeTest extends AbstractTest {
        @Parameters(name = "Name={0}")
        public static Iterable<Object[]> parameters() {
            return Arrays.asList(
                    createParameters(
                            "testInvalidMessageNo9Tag",
                            "8=FIX.4.4|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|",
                            msg -> "Not valid header: 8=FIX.4.4\u000135=8, missing 9 tag. Valid tag values: 8, 9, 35."
                    ),
                    createParameters("testInvalidMessageNo35Tag",
                            "8=FIX.4.4|9=289|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|",
                            msg -> String.format("Not valid message: %s, missing tag 35", msg)
                    ),
                    createParameters("testInavalidMessageExtraTagHeader",
                            "8=FIX.4.4|34=1090|9=289|35=8|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|",
                            msg -> "Header contains invalid tag 34. Valid tag values: 8, 9, 35."
                    ),
                    createParameters("testInvalidMessageNotTrailer",
                            "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|",
                            msg -> String.format("Not a valid message: %s, missing tag 10", msg)
                    ),
                    createParameters("testInvalidMessageNotTrailer2",
                            "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=12|",
                            msg -> String.format("Not a valid message: %s, missing tag 10", msg)
                    ),
                    createParameters("wrongNumberOfDigitsInCheckSum",
                            "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=12|10=53|",
                            msg -> "Incorrect CheckSum format: 10=53\u0001; must have 3 digits and SOH in the end"
                    ),
                    createParameters("missingSohInTheEndOfCheckSum",
                            "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=12|10=053",
                            msg -> "Incorrect CheckSum format: 10=053; must have 3 digits and SOH in the end"
                    ));
        }

        private static Object[] createParameters(String name, String rawMessage, Function<String, String> errorSupplier) {
            String actualMessage = rawMessage.replace("|", "\001");
            return new Object[] { name, actualMessage, errorSupplier.apply(actualMessage) };
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
            byte[] array = message.getBytes();
            thrown.expect(InvalidMessage.class);
            thrown.expectMessage(error);
            EvolutionQFJMessage msg = new EvolutionQFJMessage(array, EMPTY_METADATA);
        }
    }

    public static class EvolutionQFJMessagePositiveTest extends AbstractTest {
        @Test
        public void testFromArray() throws InvalidMessage {
            String message = "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|"
                    .replace("|", "\001");
            byte[] array = message.getBytes();
            String msg = new EvolutionQFJMessage(array, EMPTY_METADATA).toString();
            Assert.assertEquals(msg, message);
        }

        @Test
        public void testMessageSwapped8And9Tags() throws InvalidMessage {
            String swappedTagsMessage = "9=289|8=FIX.4.4|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|"
                    .replace("|", "\001");
            String validMessage = "8=FIX.4.4|9=289|35=8|34=1090|49=TESTSELL1|52=20180920-18:23:53.671|56=TESTBUY1|6=113.35|11=636730640278898634|14=3500.0000000000|15=USD|17=20636730646335310000|21=2|31=113.35|32=3500|37=20636730646335310000|38=7000|39=1|40=1|54=1|55=MSFT|60=20180920-18:23:53.531|150=F|151=3500|453=1|448=BRK2|447=D|452=1|10=151|"
                    .replace("|", "\001");
            byte[] array = swappedTagsMessage.getBytes();
            String msg = new EvolutionQFJMessage(array, EMPTY_METADATA).toString();
            Assert.assertEquals(msg, validMessage);
        }
    }
}




