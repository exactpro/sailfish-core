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
package com.exactpro.sf.aml.visitors;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.AMLBlockUtility;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLMatrixWrapper;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

public class AMLBlockHasherVisitor implements IAMLElementVisitor {
    private final int MULTIPLIER = 37;
    private final int INITIAL = 17;

    private final AMLMatrixWrapper wrapper;
    private final Set<String> references;
    private final Map<String, String> staticVariables;
    private final Iterable<AMLBlock> blocks;
    private final Map<AMLBlock, Integer> cache;

    private int hash = INITIAL;

    public AMLBlockHasherVisitor(AMLMatrixWrapper wrapper, Map<String, String> staticVariables, Map<AMLBlock, Integer> cache, Set<String> references) {
        this.wrapper = Objects.requireNonNull(wrapper, "wrapper cannot be null");
        this.references = Objects.requireNonNull(references, "references cannot be null");
        this.staticVariables = staticVariables;
        ListMultimap<AMLBlockType, AMLBlock> blockMap = wrapper.getBlocks();
        this.blocks = Iterables.concat(blockMap.get(AMLBlockType.AfterTCBlock), blockMap.get(AMLBlockType.GlobalBlock), blockMap.get(AMLBlockType.BeforeTCBlock));
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
    }

    @Override
    public void visit(AMLElement element) throws AMLException {
        if(!element.isExecutable()) {
            return;
        }

        hash *= MULTIPLIER;

        for(Entry<String, SimpleCell> e : element.getCells().entrySet()) {
            String name = e.getKey();

            if(name.startsWith(Column.getIgnoredPrefix())) {
                continue;
            }

            hash += name.hashCode() ^ e.getValue().getValue().hashCode();
        }

        JavaStatement statement = JavaStatement.value(element.getValue(Column.Action));

        if(statement == JavaStatement.INCLUDE_BLOCK) {
            String reference = element.getValue(Column.Template);
            AMLBlock block = wrapper.getBlockByReference(AMLBlockType.Block, reference);

            // protection against recursion
            if(block != null && references.add(reference)) {
                hash = hash * MULTIPLIER + getHash(block);
                references.remove(reference);
            }
        } else if(statement == JavaStatement.SET_STATIC && staticVariables != null) {
            String reference = element.getValue(Column.Reference);
            String value = staticVariables.get(reference);

            if(value != null) {
                hash = hash * MULTIPLIER + value.hashCode();
            }
        }
    }

    @Override
    public void visit(AMLBlock block) throws AMLException {
        if(!block.isExecutable()) {
            return;
        }

        visit((AMLElement)block);
        AMLBlockType type = AMLBlockType.value(block.getValue(Column.Action));

        if(type == AMLBlockType.TestCase) {
            for(AMLBlock b : blocks) {
                hash = hash * MULTIPLIER + getHash(b);
            }
        }

        for(AMLElement element : block) {
            element.accept(this);
        }
    }

    private int getHash(AMLBlock block) throws AMLException {
        Integer hash = cache.get(block);

        if(hash == null) {
            cache.put(block, hash = AMLBlockUtility.hash(block, wrapper, staticVariables, cache, references));
        }

        return hash;
    }

    public int getHash() {
        return hash;
    }
}
