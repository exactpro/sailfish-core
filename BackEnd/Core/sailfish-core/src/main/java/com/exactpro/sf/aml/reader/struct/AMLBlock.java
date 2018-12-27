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
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.visitors.IAMLElementVisitor;

public class AMLBlock extends AMLElement implements Iterable<AMLElement> {
    private static final String NULL_ELEMENT_MESSAGE = "element cannot be null";
    private static final String NULL_ELEMENTS_MESSAGE = "elements cannot be null";

    private final List<AMLElement> elements;

    public AMLBlock() {
        this(0);
    }

    public AMLBlock(int line) {
        this(line, Collections.<String, SimpleCell>emptyMap(), false);
    }

    public AMLBlock(int line, Map<String, SimpleCell> cells) {
        this(line, cells, Collections.<AMLElement>emptyList(), false);
    }

    public AMLBlock(int line, Map<String, SimpleCell> cells, boolean skipOptional) {
        this(line, cells, Collections.<AMLElement>emptyList(), skipOptional);
    }

    public AMLBlock(int line, Map<String, SimpleCell> cells, Collection<AMLElement> elements) {
        this(line, cells, elements, false);
    }

    public AMLBlock(int line, Map<String, SimpleCell> cells, Collection<AMLElement> elements, boolean skipOptional) {
        super(line, cells, skipOptional);
        this.elements = new ArrayList<AMLElement>();
        addAllElements(elements);
    }

    public AMLBlock addElement(AMLElement element) {
        Objects.requireNonNull(element, NULL_ELEMENT_MESSAGE);
        elements.add(element);

        return this;
    }

    public AMLBlock addElement(int index, AMLElement element) {
        Objects.requireNonNull(element, NULL_ELEMENT_MESSAGE);
        elements.add(index, element);

        return this;
    }

    public AMLBlock setElement(int index, AMLElement element) {
        Objects.requireNonNull(element, NULL_ELEMENT_MESSAGE);
        elements.set(index, element);

        return this;
    }

    public AMLElement getElement(int index) {
        return elements.get(index);
    }

    public AMLBlock removeElement(int index) {
        elements.remove(index);
        return this;
    }

    public AMLBlock removeAllElements() {
        elements.clear();
        return this;
    }

    public AMLBlock addAllElements(Collection<AMLElement> elements) {
        Objects.requireNonNull(elements, NULL_ELEMENTS_MESSAGE);

        for(AMLElement element : elements) {
            addElement(element);
        }

        return this;
    }

    public AMLBlock addAllElements(int index, Collection<AMLElement> elements) {
        Objects.requireNonNull(elements, NULL_ELEMENTS_MESSAGE);

        for(AMLElement element : elements) {
            addElement(index++, element);
        }

        return this;
    }

    public List<AMLElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public Iterator<AMLElement> iterator() {
        return elements.iterator();
    }

    @Override
    public void accept(IAMLElementVisitor visitor) throws AMLException {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("line", line);
        builder.append("uid", uid);
        builder.append("cells", cells.values());
        builder.append("elements", elements.size());

        return builder.toString();
    }

    @Override
    public AMLBlock clone() {
        return clone(true);
    }

    public AMLBlock clone(boolean deep) {
        AMLBlock cloned = new AMLBlock(line, cells, skipOptional);

        for(AMLElement element : elements) {
            cloned.elements.add(deep ? element.clone() : element);
        }

        return cloned;
    }
}
