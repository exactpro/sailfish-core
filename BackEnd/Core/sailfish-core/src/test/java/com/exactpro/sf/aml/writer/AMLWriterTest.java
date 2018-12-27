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
package com.exactpro.sf.aml.writer;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.reader.AMLReader;
import com.exactpro.sf.util.AbstractTest;

public class AMLWriterTest extends AbstractTest {

	//TODO: add DefineHeader in test matrix when RM36919 will be implemented
	//TODO empty lines are ignored now. turn on when it will be fixed.
	//@Test
    public void test() throws IOException, AMLException, Exception {
        File inputFile = new File(BASE_DIR, "src/test/resources/aml/writer/test.csv");
        File outputFile = new File(BASE_DIR, "src/test/resources/aml/writer/temp.csv");

        AMLWriter.write(outputFile, AMLReader.read(new AdvancedMatrixReader(inputFile)));

        try(AdvancedMatrixReader inputReader = new AdvancedMatrixReader(inputFile);
            AdvancedMatrixReader outputReader = new AdvancedMatrixReader(outputFile)) {

            Assert.assertEquals(inputReader.getHeader(), outputReader.getHeader());

            while(inputReader.hasNext() && outputReader.hasNext()) {
                Assert.assertEquals(inputReader.readCells(), outputReader.readCells());
            }

            Assert.assertFalse(inputReader.hasNext());
            Assert.assertFalse(outputReader.hasNext());
        }

        outputFile.delete();
    }
}
