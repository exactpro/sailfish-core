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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.AMLBlockBrace;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;

public class AMLReader {
    public static AMLMatrix read(AdvancedMatrixReader matrixReader) throws IOException, AMLException {
        return read(matrixReader, false);
    }

    public static AMLMatrix read(AdvancedMatrixReader matrixReader, boolean skipOptional) throws IOException, AMLException {
        AlertCollector alertCollector = new AlertCollector();
        List<AMLBlock> blocks = new ArrayList<>();
        Stack<JavaStatement> statementStack = new Stack<>();
        Stack<AMLBlock> blockStack = new Stack<>();
        AMLBlockBrace blockBrace = null;
        List<SimpleCell> header = prepareHeader(matrixReader.getHeader());
        int line = 0;

        while(matrixReader.hasNext()) {
            line = matrixReader.getRowNumber();
            Map<String, SimpleCell> cells = matrixReader.readCells();

            if(cells.isEmpty()) {
                continue;
            }

            AMLElement element = new AMLElement(line, cells, skipOptional);

            if(AMLLangConst.YES.equalsIgnoreCase(element.getValue(Column.Comment))) {
                continue;
            }

            String actionName = element.getValue(Column.Action);
            AMLBlockBrace currentBlockBrace = AMLBlockBrace.value(actionName);

            // workaround for "include block" action for SailfishURI init
            if(AMLLangConst.INCLUDE_BLOCK_OLD_ACTION.equalsIgnoreCase(actionName)) {
                actionName = JavaStatement.INCLUDE_BLOCK.getValue();
                element.setValue(Column.Action, actionName);
            }

            if(currentBlockBrace != null) {
                if(blockStack.isEmpty() && !currentBlockBrace.isStart()) {
                    alertCollector.add(new Alert(line, "No block to close"));
                    continue;
                }

                if(blockStack.size() == 1) {
                    if(currentBlockBrace.isStart()) {
                        alertCollector.add(new Alert(line, "Unclosed block at line: " + blockStack.peek().getLine()));
                        continue;
                    } else if(blockBrace != currentBlockBrace.getInversed()) {
                        String error = String.format("Invalid close statement: %s (expected: %s)", currentBlockBrace, blockBrace.getInversed());
                        alertCollector.add(new Alert(line, error));

                        continue;
                    }
                }

                if(blockStack.size() > 1) {
                    alertCollector.add(new Alert(line, "Unclosed block at line: " + blockStack.peek().getLine()));
                    continue;
                }

                if(currentBlockBrace.isStart()) {
                    AMLBlock block = new AMLBlock(line, cells, skipOptional);
                    blockBrace = currentBlockBrace;

                    blockStack.push(block);
                } else {
                    blocks.add(blockStack.pop());
                    blockBrace = null;
                }

                continue;
            }

            if(blockStack.isEmpty()) {
                if(element.isExecutable() && (actionName != null || element.containsCell(Column.Reference) || element.containsCell(Column.ReferenceToFilter))) {
                    alertCollector.add(new Alert(line, "Block is not opened"));
                }

                continue;
            }

            AMLBlock block = blockStack.peek();
            JavaStatement statement = JavaStatement.value(actionName);

            if(statement != null) {
                switch(statement) {
                case DEFINE_HEADER:
                case DEFINE_SERVICE_NAME:
                case INCLUDE_BLOCK:
                case SET_STATIC:
                    block.addElement(element);
                    continue;
                case BEGIN_IF:
                    AMLBlock statementBlock = new AMLBlock(line);
                    AMLBlock innerBlock = new AMLBlock(line, cells, skipOptional);

                    statementBlock.setValue(Column.Execute, element.isExecutable() ? AMLLangConst.YES : AMLLangConst.NO);
                    block.addElement(statementBlock);
                    statementBlock.addElement(innerBlock);
                    blockStack.push(statementBlock);
                    blockStack.push(innerBlock);
                    statementStack.push(statement);

                    continue;
                case BEGIN_LOOP:
                    statementBlock = new AMLBlock(line, cells, skipOptional);

                    block.addElement(statementBlock);
                    blockStack.push(statementBlock);
                    statementStack.push(statement);

                    continue;
                case BEGIN_ELIF:
                case BEGIN_ELSE:
                    if(statementStack.isEmpty()) {
                        alertCollector.add(new Alert(line, "Missing '" + JavaStatement.BEGIN_IF + "' statement"));
                        continue;
                    }

                    JavaStatement previousStatement = statementStack.peek();

                    if(previousStatement != JavaStatement.BEGIN_IF && previousStatement != JavaStatement.BEGIN_ELIF) {
                        String error = String.format("Missing '%s' or '%s' statement", JavaStatement.BEGIN_IF, JavaStatement.BEGIN_ELIF);
                        alertCollector.add(new Alert(line, error));

                        continue;
                    }

                    blockStack.pop();
                    statementStack.pop();

                    block = blockStack.peek();
                    innerBlock = new AMLBlock(line, cells, skipOptional);

                    block.addElement(innerBlock);
                    blockStack.push(innerBlock);
                    statementStack.push(statement);

                    continue;
                case END_IF:
                    if(statementStack.isEmpty() || (statementStack.peek() != JavaStatement.BEGIN_IF && statementStack.peek() != JavaStatement.BEGIN_ELIF && statementStack.peek() != JavaStatement.BEGIN_ELSE)) {
                        String error = String.format("Missing '%s', '%s' or '%s' statement", JavaStatement.BEGIN_IF, JavaStatement.BEGIN_ELIF, JavaStatement.BEGIN_ELSE);
                        alertCollector.add(new Alert(line, error));

                        continue;
                    }

                    blockStack.pop();
                    blockStack.pop();
                    statementStack.pop();

                    continue;
                case END_LOOP:
                    if(statementStack.isEmpty() || statementStack.peek() != JavaStatement.BEGIN_LOOP) {
                        String error = "Missing '" + JavaStatement.BEGIN_LOOP + "' statement";
                        alertCollector.add(new Alert(line, error));

                        continue;
                    }

                    blockStack.pop();
                    statementStack.pop();

                    continue;
                default:
                    alertCollector.add(new Alert(line, "Unknown statement: " + statement.getValue()));
                    continue;
                }
            }

            block.addElement(element);
        }

        while(!blockStack.isEmpty()) {
            alertCollector.add(new Alert(line + 1, "Unclosed block at line: " + blockStack.pop().getLine()));
        }

        if(alertCollector.getCount(AlertType.ERROR) > 0) {
            throw new AMLException("Failed to read matrix", alertCollector);
        }

        return new AMLMatrix(header, blocks);
    }

    /**
     * Removes empty columns from header
     *
     * @param header list of header columns to process
     * @return processed header without empty cells
     */
    private static List<SimpleCell> prepareHeader(List<SimpleCell> header) {
        List<SimpleCell> newHeader = new ArrayList<>();

        for(SimpleCell column : header) {
            if(StringUtils.isNotBlank(column.getValue())) {
                newHeader.add(column);
            }
        }

        return newHeader;
    }
}
