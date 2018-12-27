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
package com.exactpro.sf.aml.iomatrix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import org.junit.Assert;
import org.junit.Test;

public class AdvancedMatrixWriterTest {

    // TODO 1.8 fails

    private static List<Map<String, SimpleCell>> rows = new ArrayList<>();

    private String tmpDir = "build" + File.separator + "tmp";
    private File csv = new File(tmpDir + File.separator + "test.csv");

    static {
        Map<String, SimpleCell> first = new HashMap<>();
        first.put("#action", new SimpleCell("Action"));
        first.put("value", new SimpleCell("Value"));
        rows.add(first);
        // row with new header
        Map<String, SimpleCell> second = new HashMap<>();
        second.put("#new field", new SimpleCell("new"));
        second.put("#action", new SimpleCell("nothing"));
        rows.add(second);
        // row with unknown value
        Map<String, SimpleCell> third = new HashMap<>();
        third.put("#action", new SimpleCell("nothiing"));
        third.put("value", new SimpleCell("Value"));
        third.put("#new field", new SimpleCell("new2"));
        third.put("unknown", new SimpleCell("default value"));
        rows.add(third);
    }

    @Test
    public void test() throws Exception {

        try (AdvancedMatrixWriter writer = new AdvancedMatrixWriter(csv,
                Arrays.asList(new SimpleCell[] { new SimpleCell("#action"), new SimpleCell("value") }));) {
            writer.writeCells(rows.get(0));

            // Define header for row with new fields
            writer.writeDefineHeader(Arrays.asList(new SimpleCell[] { new SimpleCell("#action"),
                    new SimpleCell("value"), new SimpleCell("#new field") }));
            writer.writeCells(rows.get(1));

            // try write new field without redefining headers, all unknown
            // fields will shift to right
            writer.writeCells(rows.get(2));

            // try add new system field "#new field" in third row, but it
            // inherited from second row

            boolean error = false;
            try {
                writer.writeDefineHeader(Arrays.asList(new SimpleCell[] { new SimpleCell("#action"),
                        new SimpleCell("value"), new SimpleCell("#new field"), new SimpleCell("#new field") }));
            } catch (IOException e) {
                error = true;
                Assert.assertEquals(
                        "Invalid matrix structure. Detected duplicated fields at C7, D7,  positions. Field name is #new field",
                        e.getMessage());
            }

            Assert.assertTrue("DefineHeader command added already contains \"#new field\" field.", error);
        }

        try (AdvancedMatrixReader reader = new AdvancedMatrixReader(csv)) {
            while (reader.hasNext()) {
                Assert.assertEquals(getMap("{value=Value, #action=Action}"), reader.readCells());
                Assert.assertEquals(getMap("{#new field=#new field, value=value, #action=DefineHeader}"),
                        reader.readCells());
                Assert.assertEquals(getMap("{#new field=new, #action=nothing}"), reader.readCells());
                Assert.assertEquals(getMap("{#new field=#new field, value=value, unknown=unknown, #action=DefineHeader}"),
                        reader.readCells());
                Assert.assertEquals(getMap("{#new field=new2, value=Value, unknown=default value, #action=nothiing}"),
                        reader.readCells());
            }
        }
    }

    private Map<String, SimpleCell> getMap(String s) {

        HashMap<String, SimpleCell> result = new HashMap<>();

        s = s.substring(1, s.length() - 1);

        String[] entries = s.split(", ");

        for (String entry : entries) {
            String[] kv = entry.split("=");
            switch (kv.length) {
            case 2:
                result.put(kv[0], new SimpleCell(kv[1]));
                break;
            case 1:
                result.put(kv[0], new SimpleCell());
                break;
            case 0:
                throw new RuntimeErrorException(new Error("Not valid sample"));
            }
        }

        return result;
    }
}
