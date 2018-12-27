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
package com.exactpro.sf.aml.reader.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.common.util.StringUtil;

public class AMLMatrix implements Cloneable, Iterable<AMLBlock> {
    private static final String NULL_HEADER_MESSAGE = "header cannot be null";
    private static final String NULL_BLOCK_MESSAGE = "block cannot be null";
    private static final String NULL_BLOCKS_MESSAGE = "blocks cannot be null";
    private static final String NULL_COLUMN_MESSAGE = "column cannot be null";

    private final List<SimpleCell> header;
    private final List<AMLBlock> blocks;

    public AMLMatrix() {
        this(Collections.<SimpleCell>emptyList(), Collections.<AMLBlock>emptyList());
    }

    public AMLMatrix(Collection<SimpleCell> header) {
        this(header, Collections.<AMLBlock>emptyList());
    }

    public AMLMatrix(Collection<SimpleCell> header, Collection<AMLBlock> blocks) {
        this.header = new ArrayList<>();
        this.blocks = new ArrayList<>();

        addHeader(header);
        addAllBlocks(blocks);
    }

    public AMLMatrix addColumn(SimpleCell column) {
        header.add(checkColumn(column, true));
        return this;
    }

    public AMLMatrix addColumn(int index, SimpleCell column) {
        header.add(index, checkColumn(column, true));
        return this;
    }

    public AMLMatrix setColumn(int index, SimpleCell column) {
        header.set(index, checkColumn(column, true));
        return this;
    }

    public SimpleCell getColumn(int index) {
        return header.get(index);
    }

    public AMLMatrix removeColumn(SimpleCell column) {
        header.remove(checkColumn(column, false));
        return this;
    }

    public AMLMatrix removeColumn(int index) {
        header.remove(index);
        return this;
    }

    public AMLMatrix addHeader(Collection<SimpleCell> header) {
        Objects.requireNonNull(header, NULL_HEADER_MESSAGE);

        for(SimpleCell column : header) {
            addColumn(column);
        }

        return this;
    }

    public AMLMatrix addHeader(int index, Collection<SimpleCell> header) {
        Objects.requireNonNull(header, NULL_HEADER_MESSAGE);

        for(SimpleCell column : header) {
            addColumn(index++, column);
        }

        return this;
    }

    public List<SimpleCell> getHeader() {
        return Collections.unmodifiableList(header);
    }

    public AMLMatrix addBlock(AMLBlock block) {
        Objects.requireNonNull(block, NULL_BLOCK_MESSAGE);
        blocks.add(block);

        return this;
    }

    public AMLMatrix addBlock(int index, AMLBlock block) {
        Objects.requireNonNull(block, NULL_BLOCK_MESSAGE);
        blocks.add(index, block);

        return this;
    }

    public AMLMatrix setBlock(int index, AMLBlock block) {
        Objects.requireNonNull(block, NULL_BLOCK_MESSAGE);
        blocks.set(index, block);

        return this;
    }

    public AMLBlock getBlock(int index) {
        return blocks.get(index);
    }

    public AMLMatrix removeBlock(int index) {
        blocks.remove(index);
        return this;
    }

    public AMLMatrix addAllBlocks(Collection<AMLBlock> blocks) {
        Objects.requireNonNull(blocks, NULL_BLOCKS_MESSAGE);

        for(AMLBlock block : blocks) {
            addBlock(block);
        }

        return this;
    }

    public AMLMatrix addAllBlocks(int index, Collection<AMLBlock> blocks) {
        Objects.requireNonNull(blocks, NULL_BLOCK_MESSAGE);

        for(AMLBlock block : blocks) {
            addBlock(index++, block);
        }

        return this;
    }

    public List<AMLBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public Iterator<AMLBlock> iterator() {
        return blocks.iterator();
    }

    private SimpleCell checkColumn(SimpleCell column, boolean checkPresence) {
        Objects.requireNonNull(column, NULL_COLUMN_MESSAGE);
        String value = column.getValue();

        if(value == null || !StringUtil.isStripped(value)) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }

        if(checkPresence) {
            for(SimpleCell c : header) {
                if(c.getValue().equals(value)) {
                    throw new IllegalArgumentException("Duplicate value: " + value);
                }
            }
        }

        return column;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("header", header);
        builder.append("blocks", blocks.size());

        return builder.toString();
    }

    public AMLMatrix clone() {
        return clone(true);
    }

    public AMLMatrix clone(boolean deep) {
        AMLMatrix cloned = new AMLMatrix(header);

        for(AMLBlock block : blocks) {
            cloned.blocks.add(deep ? block.clone() : block);
        }

        return cloned;
    }
}
