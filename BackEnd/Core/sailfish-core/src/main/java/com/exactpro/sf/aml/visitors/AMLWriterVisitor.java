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

import java.io.IOException;

import com.exactpro.sf.aml.AMLBlockBrace;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixWriter;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;

public class AMLWriterVisitor implements IAMLElementVisitor {
    private final AdvancedMatrixWriter matrixWriter;

    public AMLWriterVisitor(AdvancedMatrixWriter matrixWriter) {
        this.matrixWriter = matrixWriter;
    }

    @Override
    public void visit(AMLElement element) throws AMLException {
        try {
            matrixWriter.writeCells(element.getCells());
        } catch(IOException e) {
            throw new AMLException(e);
        }
    }

    @Override
    public void visit(AMLBlock block) throws AMLException {
        String actionName = block.getValue(Column.Action);
        AMLBlockBrace blockBrace = AMLBlockBrace.value(actionName);
        JavaStatement statement = JavaStatement.value(actionName);

        if(blockBrace != null || statement != null) {
            visit((AMLElement)block);
        }

        for(AMLElement element : block) {
            element.accept(this);
        }

        if(blockBrace != null) {
            new AMLElement().setValue(Column.Action, blockBrace.getInversed().getName().toLowerCase()).accept(this);
        } else if(statement == JavaStatement.BEGIN_LOOP) {
            new AMLElement().setValue(Column.Action, JavaStatement.END_LOOP.getValue().toLowerCase()).accept(this);
        } else if(actionName == null) { // wrapper block for conditional statement doesn't have action
            new AMLElement().setValue(Column.Action, JavaStatement.END_IF.getValue().toLowerCase()).accept(this);
        }
    }
}
