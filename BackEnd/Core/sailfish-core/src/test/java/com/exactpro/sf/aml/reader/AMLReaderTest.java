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
package com.exactpro.sf.aml.reader;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.util.AbstractTest;

public class AMLReaderTest extends AbstractTest {
    @Test
    public void positiveTest() throws Exception {
        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/reader/positive-test.csv");

        try(AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrix matrix = AMLReader.read(matrixReader);
            List<AMLBlock> blocks = matrix.getBlocks();

            Assert.assertEquals(3, blocks.size());

            AMLBlock block = blocks.get(0);

            Assert.assertEquals(5, block.getLine());
            Assert.assertFalse(block.isExecutable());
            Assert.assertEquals(1, block.getElements().size());
            Assert.assertEquals(AMLElement.class, block.getElements().get(0).getClass());

            block = blocks.get(1);

            Assert.assertEquals(9, block.getLine());
            Assert.assertTrue(block.isExecutable());
            Assert.assertEquals(1, block.getElements().size());
            Assert.assertEquals(AMLBlock.class, block.getElements().get(0).getClass());

            block = blocks.get(2);
            List<AMLElement> elements = block.getElements();

            Assert.assertEquals(24, block.getLine());
            Assert.assertTrue(block.isExecutable());
            Assert.assertEquals(3, block.getElements().size());

            Assert.assertEquals(AMLBlock.class, elements.get(0).getClass());
            Assert.assertEquals(AMLBlock.class, elements.get(1).getClass());
            Assert.assertEquals(AMLBlock.class, elements.get(2).getClass());

            Assert.assertFalse(elements.get(0).isExecutable());
            Assert.assertTrue(elements.get(1).isExecutable());

            block = (AMLBlock)elements.get(1);

            Assert.assertTrue(block.getElements().get(0).isExecutable());
            Assert.assertFalse(block.getElements().get(1).isExecutable());

            block = (AMLBlock)elements.get(2);

            Assert.assertEquals(3, block.getElements().size());
        }
    }

    @Test
    public void negativeTest() throws Exception {
        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/reader/negative-test.csv");

        try(AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLReader.read(matrixReader);
            Assert.fail("No errors were detected");
        } catch(AMLException e) {
            Collection<Alert> errors = e.getAlertCollector().getAlerts();

            Assert.assertEquals(17, errors.size());

            Alert error = new Alert(2, null, null, "Block is not opened", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(3, null, null, "No block to close", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(6, null, null, "Unclosed block at line: 5", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(7, null, null, "Invalid close statement: Block end (expected: Test case end)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(9, null, null, "Unclosed block at line: 5", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(10, null, null, "Missing 'IF', 'ELIF' or 'ELSE' statement", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(11, null, null, "Missing 'IF' statement", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(13, null, null, "Missing 'IF' or 'ELIF' statement", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(14, null, null, "Missing 'IF' or 'ELIF' statement", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(16, null, null, "Unclosed block at line: 15", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(17, null, null, "Missing 'REPEAT' statement", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(20, null, null, "Unclosed block at line: 18", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(21, null, null, "Unclosed block at line: 18", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(22, null, null, "Unclosed block at line: 18", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(22, null, null, "Unclosed block at line: 15", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(22, null, null, "Unclosed block at line: 12", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(22, null, null, "Unclosed block at line: 5", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));

            Assert.assertEquals(0, errors.size());
        }
    }

    @Test
    public void optionalTest() throws Exception {
        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/reader/optional-test.csv");
        try (AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrix matrix = AMLReader.read(matrixReader, true);
            checkElements(matrix, true);
        }
        try (AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrix matrix = AMLReader.read(matrixReader, false);
            checkElements(matrix, false);
        }
    }

    @Test
    public void testValuesInExecuteColumn() throws Exception {
        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/reader/invalid-execute-value-test.csv");
        try (AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrix matrix = AMLReader.read(matrixReader, false);
            List<AMLBlock> blocks = matrix.getBlocks();
            Assert.assertEquals("Unexpected block count", 1, blocks.size());
            AMLBlock testCase = blocks.get(0);
            Assert.assertEquals("Unexpected block's element count", 5, testCase.getElements().size());
            int n = 0;
            Assert.assertTrue("Block with invalid value should be executable", testCase.getElement(n++).isExecutable());
            Assert.assertTrue("Optional block executable state is incorrect", testCase.getElement(n++).isExecutable());
            Assert.assertTrue("Block with empty value should be executable", testCase.getElement(n++).isExecutable());
            Assert.assertTrue("Block with 'Y' should be executable", testCase.getElement(n++).isExecutable());
            Assert.assertFalse("Block with 'N' shouldn't be executable", testCase.getElement(n++).isExecutable());
        }
    }

    private void checkElements(AMLMatrix matrix, boolean skipOptional) {
        List<AMLBlock> blocks = matrix.getBlocks();
        Assert.assertEquals(3, blocks.size());

        // global block
        AMLBlock block = blocks.get(0);
        Assert.assertEquals(5, block.getLine());

        if (skipOptional) {
            Assert.assertFalse(block.isExecutable());
        } else {
            Assert.assertTrue(block.isExecutable());
        }
        Assert.assertEquals(1, block.getElements().size());
        Assert.assertEquals(AMLElement.class, block.getElements().get(0).getClass());

        // test case 1
        block = blocks.get(1);
        Assert.assertEquals(9, block.getLine());
        Assert.assertTrue(block.isExecutable());
        Assert.assertEquals(1, block.getElements().size());
        Assert.assertEquals(AMLBlock.class, block.getElements().get(0).getClass());

        AMLBlock repeatBlock = (AMLBlock) block.getElements().get(0);
        Assert.assertEquals(10, repeatBlock.getLine());
        Assert.assertTrue(repeatBlock.isExecutable());
        Assert.assertEquals(2, repeatBlock.getElements().size());
        Assert.assertEquals(AMLElement.class, repeatBlock.getElements().get(0).getClass());
        Assert.assertEquals(AMLBlock.class, repeatBlock.getElements().get(1).getClass());

        AMLBlock nestedRepeatBlock = (AMLBlock) repeatBlock.getElements().get(1);
        Assert.assertEquals(12, nestedRepeatBlock.getLine());
        if (skipOptional) {
            Assert.assertFalse(nestedRepeatBlock.isExecutable());
        } else {
            Assert.assertTrue(nestedRepeatBlock.isExecutable());
        }
        Assert.assertEquals(2, nestedRepeatBlock.getElements().size());
        Assert.assertEquals(AMLElement.class, nestedRepeatBlock.getElements().get(0).getClass());
        Assert.assertEquals(AMLBlock.class, nestedRepeatBlock.getElements().get(1).getClass());

        // test case 2
        block = blocks.get(2);
        List<AMLElement> elements = block.getElements();
        Assert.assertEquals(24, block.getLine());
        Assert.assertTrue(block.isExecutable());
        Assert.assertEquals(3, block.getElements().size());

        Assert.assertEquals(AMLElement.class, elements.get(0).getClass());
        Assert.assertEquals(AMLElement.class, elements.get(1).getClass());
        Assert.assertEquals(AMLElement.class, elements.get(2).getClass());
        Assert.assertTrue(elements.get(0).isExecutable());
        if (skipOptional) {
            Assert.assertFalse(elements.get(1).isExecutable());
        } else {
            Assert.assertTrue(elements.get(1).isExecutable());
        }
        Assert.assertFalse(elements.get(2).isExecutable());
    }
}
