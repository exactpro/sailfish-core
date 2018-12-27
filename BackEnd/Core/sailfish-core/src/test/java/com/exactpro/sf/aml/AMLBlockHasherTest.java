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
package com.exactpro.sf.aml;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.reader.AMLReader;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.aml.writer.AMLWriter;
import com.exactpro.sf.util.AbstractTest;

public class AMLBlockHasherTest extends AbstractTest {
    @Test
    public void positiveTest() throws Exception {
        Map<String, String> staticVariables = new HashMap<>();

        staticVariables.put("sv", "1");

        int firstBlock = getHash("positive/first-block.csv", null);
        int firstBlockWithGlobalBlock = getHash("positive/first-block-with-global-block.csv", null);
        int firstBlockWithGlobalBlockStaticSubstitution = getHash("positive/first-block-with-global-block.csv", staticVariables);
        int multipleTestCases = getHash("positive/multiple-test-cases.csv", null);
        int multipleTestCasesReordered = getHash("positive/multiple-test-cases-reordered.csv", null);
        int testCase = getHash("positive/test-case.csv", null);
        int testCaseStaticSubstitution = getHash("positive/test-case.csv", staticVariables);
        int testCaseColumnAltered = getHash("positive/test-case-column-altered.csv", null);
        int testCaseColumnCommented = getHash("positive/test-case-column-commented.csv", null);
        int testCaseColumnsReordered = getHash("positive/test-case-columns-reordered.csv", null);
        int testCaseIfNotExecutable = getHash("positive/test-case-if-not-executable.csv", null);
        int testCaseRowCommented = getHash("positive/test-case-row-commented.csv", null);
        int testCaseRowNotExecutable = getHash("positive/test-case-row-not-executable.csv", null);
        int testCaseRowsReordered = getHash("positive/test-case-rows-reordered.csv", null);
        int testCaseWithAfterTestCaseBlock = getHash("positive/test-case-with-after-test-case-block.csv", null);
        int testCaseWithAfterTestCaseBlockCommented = getHash("positive/test-case-with-after-test-case-block-commented.csv", null);
        int testCaseWithGlobalBlock = getHash("positive/test-case-with-global-block.csv", null);
        int testCaseWithGlobalBlockStaticSubstitution = getHash("positive/test-case-with-global-block.csv", staticVariables);
        int testCaseWithGlobalBlockAltered = getHash("positive/test-case-with-global-block-altered.csv", null);
        int testCaseWithGlobalBlockCommented = getHash("positive/test-case-with-global-block-commented.csv", null);
        int testCaseWithIncludeBlock = getHash("positive/test-case-with-include-block.csv", null);
        int testCaseWithIncludeBlockStaticSubstitution = getHash("positive/test-case-with-include-block.csv", staticVariables);
        int testCaseWithIncludeBlockAltered = getHash("positive/test-case-with-include-block-altered.csv", null);

        Assert.assertEquals(392283681, firstBlock);
        Assert.assertEquals(392283681, firstBlockWithGlobalBlock);
        Assert.assertEquals(392283681, firstBlockWithGlobalBlockStaticSubstitution);
        Assert.assertEquals(-1642266903, testCase);
        Assert.assertEquals(1763097094, testCaseStaticSubstitution);
        Assert.assertEquals(205871019, testCaseColumnAltered);
        Assert.assertEquals(-1642266903, testCaseColumnCommented);
        Assert.assertEquals(-1642266903, testCaseColumnsReordered);
        Assert.assertEquals(-1642266903, testCaseIfNotExecutable);
        Assert.assertEquals(-1642266903, testCaseRowCommented);
        Assert.assertEquals(-1642266903, testCaseRowNotExecutable);
        Assert.assertEquals(-1720311295, testCaseRowsReordered);
        Assert.assertEquals(-2083148313, testCaseWithAfterTestCaseBlock);
        Assert.assertEquals(-1642266903, testCaseWithAfterTestCaseBlockCommented);
        Assert.assertEquals(1615500406, testCaseWithGlobalBlock);
        Assert.assertEquals(746878792, testCaseWithGlobalBlockStaticSubstitution);
        Assert.assertEquals(-699995341, testCaseWithGlobalBlockAltered);
        Assert.assertEquals(-1642266903, testCaseWithGlobalBlockCommented);
        Assert.assertEquals(-2108300103, testCaseWithIncludeBlock);
        Assert.assertEquals(1357959779, testCaseWithIncludeBlockStaticSubstitution);
        Assert.assertEquals(-1046445890, testCaseWithIncludeBlockAltered);

        Assert.assertEquals(firstBlock, firstBlockWithGlobalBlock);
        Assert.assertEquals(firstBlock, firstBlockWithGlobalBlockStaticSubstitution);

        Assert.assertTrue(testCase != testCaseStaticSubstitution);
        Assert.assertTrue(testCase != testCaseColumnAltered);
        Assert.assertTrue(testCase != testCaseRowsReordered);

        Assert.assertEquals(testCase, testCaseColumnsReordered);
        Assert.assertEquals(testCase, testCaseColumnCommented);
        Assert.assertEquals(testCase, testCaseIfNotExecutable);
        Assert.assertEquals(testCase, testCaseRowCommented);
        Assert.assertEquals(testCase, testCaseRowNotExecutable);
        Assert.assertEquals(testCase, testCaseWithAfterTestCaseBlockCommented);
        Assert.assertEquals(testCase, testCaseWithGlobalBlockCommented);

        Assert.assertTrue(testCase != testCaseWithAfterTestCaseBlock);
        Assert.assertTrue(testCase != testCaseWithGlobalBlock);
        Assert.assertTrue(testCaseWithGlobalBlock != testCaseWithGlobalBlockAltered);
        Assert.assertTrue(testCaseWithGlobalBlock != testCaseWithGlobalBlockStaticSubstitution);

        Assert.assertTrue(testCase != testCaseWithIncludeBlock);
        Assert.assertTrue(testCaseWithIncludeBlock != testCaseWithIncludeBlockAltered);
        Assert.assertTrue(testCaseWithIncludeBlock != testCaseWithIncludeBlockStaticSubstitution);

        Assert.assertEquals(multipleTestCases, multipleTestCasesReordered);
    }

    @Test
    public void negativeTest() throws Exception {
        getHash("negative/test-case-with-include-block.csv", null);
        getHash("negative/test-case-with-unknown-include-block.csv", null);
        //just check that we ignore this cases
    }

    @Test
    public void savedMatrixTest() throws Exception {
        File sourceFile = new File(BASE_DIR, "src/test/resources/aml/hasher/positive/test-case-with-empty-lines.csv");
        File targetFile = new File(BASE_DIR, "src/test/resources/aml/hasher/positive/test-case-with-empty-lines-temp.csv");

        AMLMatrix sourceMatrix = null;
        AMLMatrix targetMatrix = null;

        try(AdvancedMatrixReader reader = new AdvancedMatrixReader(sourceFile)) {
            AMLWriter.write(targetFile, sourceMatrix = AMLReader.read(reader));
        }

        try(AdvancedMatrixReader reader = new AdvancedMatrixReader(targetFile)) {
            targetMatrix = AMLReader.read(reader);
        } finally {
            targetFile.delete();
        }

        AMLMatrixWrapper sourceWrapper = new AMLMatrixWrapper(sourceMatrix);
        AMLMatrixWrapper targetWrapper = new AMLMatrixWrapper(targetMatrix);

        AMLBlock sourceBlock = sourceWrapper.getBlockByReference(AMLBlockType.TestCase, "tc");
        AMLBlock targetBlock = targetWrapper.getBlockByReference(AMLBlockType.TestCase, "tc");

        int sourceHash = AMLBlockUtility.hash(sourceBlock, sourceWrapper, null, new HashMap<AMLBlock, Integer>(), new HashSet<String>());
        int targetHash = AMLBlockUtility.hash(targetBlock, targetWrapper, null, new HashMap<AMLBlock, Integer>(), new HashSet<String>());

        Assert.assertEquals(sourceHash, targetHash);
    }

    private int getHash(String fileName, Map<String, String> staticVariables) throws Exception {
        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/hasher/" + fileName);

        try(AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrixWrapper wrapper = new AMLMatrixWrapper(AMLReader.read(matrixReader));
            AMLBlock block = wrapper.getBlockByReference(AMLBlockType.TestCase, "tc");

            if(block == null) {
                block = wrapper.getBlockByReference(AMLBlockType.FirstBlock, "tc");
            }

            return AMLBlockUtility.hash(block, wrapper, staticVariables, new HashMap<AMLBlock, Integer>(), new HashSet<String>());
        }
    }
}
