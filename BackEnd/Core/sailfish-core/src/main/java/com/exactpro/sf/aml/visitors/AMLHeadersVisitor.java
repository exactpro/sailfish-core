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
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;

public class AMLHeadersVisitor implements IAMLElementVisitor {

    private final List<SimpleCell> mainHeaders;

    private AMLElement lastDefineHeader = null;

    public AMLHeadersVisitor(List<SimpleCell> mainHeaders) {
        this.mainHeaders = new ArrayList<>(mainHeaders);
    }

    @Override
    public void visit(AMLElement element) throws AMLException {
        String actionName = element.getValue(Column.Action);
        JavaStatement statement = JavaStatement.value(actionName);
        if (statement == JavaStatement.DEFINE_HEADER) {
             lastDefineHeader = element;
        } else {
            for (String fieldCaption: element.getCells().keySet()) {
                if (lastDefineHeader == null || fieldCaption.startsWith(Column.getSystemPrefix())) {
                    //add to main headers
                    boolean isNewField = true;
                    for (SimpleCell cell: mainHeaders) {
                        if (cell.getValue().equals(fieldCaption)) {
                            isNewField = false;
                            break;
                        }
                    }
                    if (isNewField)
                        mainHeaders.add(new SimpleCell(fieldCaption));
                } else {
                    if (!lastDefineHeader.getCells().keySet().contains(fieldCaption)) {
                        lastDefineHeader.setValue(fieldCaption, fieldCaption);
                    }
                }
            }
        }
    }

    @Override
    public void visit(AMLBlock block) throws AMLException {
        visit((AMLElement) block);

        for(AMLElement element : block) {
            element.accept(this);
        }
    }

    public List<SimpleCell> getMainHeaders() {
        return mainHeaders;
    }

}
