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

import java.util.ArrayList;
import java.util.List;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;

public class AMLBlockFlattenerVisitor implements IAMLElementVisitor {
    private final List<AMLElement> elements = new ArrayList<>();
    private final boolean onlyExecutable;

    public AMLBlockFlattenerVisitor(boolean onlyExecutable) {
        this.onlyExecutable = onlyExecutable;
    }

    @Override
    public void visit(AMLElement element) throws AMLException {
        if(onlyExecutable && !element.isExecutable()) {
            return;
        }

        elements.add(element);
    }

    @Override
    public void visit(AMLBlock block) throws AMLException {
        if(onlyExecutable && !block.isExecutable()) {
            return;
        }

        for(AMLElement element : block) {
            element.accept(this);
        }
    }

    public List<AMLElement> getElements() {
        return elements;
    }
}
