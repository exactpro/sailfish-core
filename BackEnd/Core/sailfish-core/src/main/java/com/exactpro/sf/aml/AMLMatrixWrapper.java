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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

public class AMLMatrixWrapper {
    private final ListMultimap<AMLBlockType, AMLBlock> blocks = ArrayListMultimap.create();
    private final ListMultimap<AMLBlock, AMLElement> staticVariables = ArrayListMultimap.create();
    private final List<AMLElement> globalElements = new ArrayList<>();
    private final Set<String> references = new HashSet<>();

    public AMLMatrixWrapper(AMLMatrix matrix) throws AMLException {
        this(matrix, true);
    }

    public AMLMatrixWrapper(AMLMatrix matrix, boolean onlyExecutable) throws AMLException {
        for(AMLBlock block : matrix.getBlocks()) {
            if(onlyExecutable && !block.isExecutable()) {
                continue;
            }

            AMLBlockType blockType = AMLBlockType.value(block.getValue(Column.Action));
            blocks.put(blockType, block);

            if(blockType == AMLBlockType.GlobalBlock) {
                globalElements.addAll(AMLBlockUtility.flatten(block, onlyExecutable));
            }
        }

        List<AMLElement> currentVariables = new ArrayList<>();
        Iterable<AMLBlock> blocksIterable = Iterables.concat(blocks.get(AMLBlockType.FirstBlock),
                                                             blocks.get(AMLBlockType.TestCase),
                                                             blocks.get(AMLBlockType.GlobalBlock),
                                                             blocks.get(AMLBlockType.LastBlock));

        for(AMLBlock block : blocksIterable) {
            if(AMLBlockType.GlobalBlock != AMLBlockType.value(block.getValue(Column.Action))) {
                staticVariables.putAll(block, currentVariables);
            }

            currentVariables.addAll(getStaticVariables(block));
        }
    }

    public ListMultimap<AMLBlockType, AMLBlock> getBlocks() {
        return blocks;
    }

    public AMLElement getElementByReference(AMLBlock block, String reference) throws AMLException {
        if(block == null || StringUtils.isBlank(reference)) {
            return null;
        }

        Iterable<AMLElement> elements = AMLBlockUtility.flatten(block);
        AMLBlockType blockType = AMLBlockType.value(block.getValue(Column.Action));

        if(blockType == AMLBlockType.FirstBlock || blockType == AMLBlockType.TestCase || blockType == AMLBlockType.LastBlock) {
            if(blockType == AMLBlockType.TestCase) {
                elements = Iterables.concat(globalElements, elements);
            }

            elements = Iterables.concat(elements, staticVariables.get(block));
        }

        for(AMLElement element : elements) {
            if(reference.equals(element.getValue(Column.Reference)) || reference.equals(element.getValue(Column.ReferenceToFilter))) {
                return element;
            }
        }

        return null;
    }

    public AMLBlock getBlockByReference(AMLBlockType blockType, String blockReference) {
        if(StringUtils.isBlank(blockReference)) {
            return null;
        }

        for(AMLBlock block : blocks.get(blockType)) {
            if(blockReference.equals(block.getValue(Column.Reference))) {
                return block;
            }
        }

        return null;
    }

    public List<AMLElement> getStaticVariables(AMLBlock block) throws AMLException {
        if(block == null) {
            return Collections.emptyList();
        }

        List<AMLElement> staticVariables = new ArrayList<>();

        for(AMLElement element : AMLBlockUtility.flatten(block)) {
            JavaStatement statement = JavaStatement.value(element.getValue(Column.Action));

            if(statement == JavaStatement.INCLUDE_BLOCK) {
                String reference = element.getValue(Column.Template);
                AMLBlock includedBlock = getBlockByReference(AMLBlockType.Block, reference);

                // protection against recursion
                if(references.add(reference)) {
                    staticVariables.addAll(getStaticVariables(includedBlock));
                    references.remove(reference);
                }
            } else if(statement == JavaStatement.SET_STATIC && element.containsCell(Column.Reference)) {
                staticVariables.add(element);
            } else if(AMLLangConst.YES.equalsIgnoreCase(element.getValue(Column.IsStatic)) && (element.containsCell(Column.Reference) || element.containsCell(Column.ReferenceToFilter))) {
                staticVariables.add(element);
            }
        }

        return staticVariables;
    }

    public List<AMLElement> getGlobalElements() {
        return globalElements;
    }
}
