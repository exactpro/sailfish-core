/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.aml.iomatrix;

import com.exactpro.sf.aml.AMLBlockBrace;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Pair;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.reflect.Reflection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class JSONMatrixReader implements IMatrixReader {

    private static final Supplier<SimpleCell[]> SUPPLIER = () -> {
        throw new NoSuchElementException();
    };
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private final DEBUG dbg = new DEBUG();

    private Iterator<Integer> rowIterator;
    private Supplier<SimpleCell[]> rowsSupplier;

    private final AtomicInteger tableRowCounter = new AtomicInteger(1);


    public JSONMatrixReader(File file, MatrixFileTypes type ) throws IOException {

        Pair<Iterator<Integer>, Supplier<SimpleCell[]>> parseResult = parseToTable(JSONMatrixParser.readValue(file, type));
        rowIterator = parseResult.getFirst();
        rowsSupplier = parseResult.getSecond();
    }

    private Pair<Iterator<Integer>, Supplier<SimpleCell[]>> parseToTable(CustomValue jsonNode) throws IOException {

        Table<Integer, String, SimpleCell> table = Reflection.newProxy(Table.class, new SafetyCheckInvocationHandler());

        for(CustomValue testCaseWrapper:jsonNode.getArrayValue()) {
            if (testCaseWrapper.getObjectValue().size() > 1) {
                throw new EPSCommonException("Too many nodes in testCase wrapper");
            }

            testCaseWrapper.getObjectValue().forEach((blockKey, commonBlock) -> {
                String commonBlockType = blockKey.getKey();
                AMLBlockBrace blockBrace = AMLBlockBrace.value(commonBlockType);

                Objects.requireNonNull(commonBlock, "'AML block' node must be presented");
                Objects.requireNonNull(blockBrace, "Unknown block type " + commonBlockType);

                int localRowCounter = tableRowCounter.getAndIncrement();
                table.put(localRowCounter, Column.Action.getName(), new SimpleCell(commonBlockType,blockKey.getLine()));

                commonBlock.getObjectValue().forEach((actionKey, actionNode) -> {
                    String reference = actionKey.getKey();

                    logger.debug("reading {}", reference);

                    if (actionNode.isObject()) {

                        int nestedCount = countNestedReferences(actionNode);
                        int target = tableRowCounter.get() + nestedCount;
                        table.put(target, Column.Reference.getName(), new SimpleCell(reference, actionKey.getLine()));
                        consumeNode(actionNode, table, target,  actionKey.getLine());
                        //FIXME will add additional empty row at last action
                        tableRowCounter.getAndIncrement();
                    } else if (!actionNode.isArray()) {
                        table.put(localRowCounter, reference,  new SimpleCell(actionNode.getSimpleValue().toString(), actionKey.getLine()));
                    } else{
                        throw new IllegalStateException(String.format("Invalid value type array %s found in block %s, number line %s", reference, commonBlockType, actionKey.getLine()));
                    }
                });

                table.put(tableRowCounter.getAndIncrement(), Column.Action.getName(), new SimpleCell(blockBrace.getInversed().getName(),blockKey.getLine()));
            });
        }

        Set<String> columns = ImmutableSet.<String>builder().addAll(table.columnKeySet()).add(Column.Id.getName()).build();
        columns.forEach(column -> table.put(0, column, new SimpleCell(column)));

        Iterator<Integer> rowIterator = dbg.DEBUG_SORT ? table.rowKeySet().iterator() : new TreeSet<>(table.rowKeySet()).iterator();

        Supplier<SimpleCell[]> supplier = SUPPLIER;
        //preserve header
        if (rowIterator.hasNext()) {

            supplier = () -> {
                int currentRow = rowIterator.next();
                Map<String, SimpleCell> row = new HashMap<>(table.row(currentRow));

                return columns.stream()
                        .map(key -> row.getOrDefault(key, new SimpleCell("")))
                        .toArray(SimpleCell[]::new);
            };
        }

        return new Pair<>(rowIterator, supplier);
    }

    @Override
    public SimpleCell[] readCells() throws IOException {
        return rowsSupplier.get();
    }

    @Override
    public String[] read() throws IOException {
        return Stream.of(rowsSupplier.get())
                .map(SimpleCell::getValue)
                .toArray(String[]::new);
    }

    @Override
    public boolean hasNext() {
        return rowIterator.hasNext();
    }

    @Override
    public void close() throws Exception {
        //TODO
    }

    private <T> Stream<T> wrapIter(Iterator<T> src) {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(src, Spliterator.SIZED);
        return StreamSupport.stream(spliterator, false);
    }

    /***
     * consumes json node desribing action or reference
     * @param node - jsom node
     * @param toTable - table to write rows
     * @param target - position of node (current pos + deps count)
     * @return generated or specified ref of action/explicit ref
     */
    private String consumeNode(CustomValue node, Table<Integer, String, SimpleCell> toTable, int target, int nodeNumberLine) {

        node.getObjectValue().forEach((actionKey, actionFieldNode) -> {
            String actionFieldKey = actionKey.getKey();

            Consumer<SimpleCell> actionFieldPut = value -> toTable.put(target, actionFieldKey, value);

            logger.debug("{} consuming with {}", dbg.openTabs(),  actionFieldKey);

            Function<CustomValue, String> processObj = jsonNode -> {
                int nestedLvls = countNestedReferences(jsonNode);
                String ref = consumeNode(jsonNode, toTable, tableRowCounter.get() + nestedLvls, actionKey.getLine());
                tableRowCounter.incrementAndGet();
                return ref;
            };

            if (actionFieldNode.isObject()) {
                actionFieldPut.accept(new SimpleCell("[" + processObj.apply(actionFieldNode) + "]", actionKey.getLine()));
            } else if (actionFieldNode.isArray()) {
                String ref = wrapIter(actionFieldNode.getArrayValue().iterator())
                        //FIXME need unwrap ref syntax? or write only ref name in json
                        .map(elNode -> elNode.isObject() ? processObj.apply(elNode) : elNode.getSimpleValue().toString())
                        .collect(Collectors.joining(","));

                actionFieldPut.accept(new SimpleCell("[" + ref + "]", actionKey.getLine()));
            } else if (actionFieldNode.isSimple()) {
                actionFieldPut.accept(new SimpleCell(actionFieldNode.getSimpleValue().toString(), actionKey.getLine()));
            }

            logger.debug("{} consuming with {} done -> {}", dbg.closeTabs(), actionFieldKey, toTable.get(target, Column.Reference.getName()));
        });

        if (!toTable.contains(target, Column.Reference.getName())) {
            toTable.put(target, Column.Reference.getName(), new SimpleCell("implicit_ref" + target, nodeNumberLine));
        }

        return toTable.get(target, Column.Reference.getName()).getValue();
    }

    /***
     * Compute nested inline object nodes for reserve place for it
      * @param node - json node
     * @return count of rows to reserve
     */
    private int countNestedReferences(CustomValue node) {

        int counter = 0;

        if(node.getObjectValue()!=null) {
            for (Map.Entry<KeyValue, CustomValue> el : node.getObjectValue().entrySet()) {
                if (el.getValue().isObject()) {
                    counter += 1 + countNestedReferences(el.getValue());
                } else if (el.getValue().isArray()) {
                    counter += wrapIter(el.getValue().getArrayValue().iterator())
                            .mapToInt(n -> !n.isSimple() ? 1 + countNestedReferences(n) : 0)
                            .sum();
                }
            }
        }
        return counter;
    }

    /***
     * Ivocation Handler for checking that rows content no mixes while generating inplace rows/arrays
     */
    private class SafetyCheckInvocationHandler implements InvocationHandler {

        private final Table<Integer, String, String> table = HashBasedTable.create();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            Object result = method.invoke(table, args);

            if ("put".equals(method.getName())) {

                if (logger.isDebugEnabled()) {
                    logger.debug("{}{} -> write ['{}':'{}']", dbg.currentTabs(), args[0], args[1], args[2]);
                }

                SimpleCell old = (SimpleCell) result;

                if (old != null && old.getValue() != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("\uD83D\uDCA9{}{} overrides value {} at {} column with {} position", dbg.currentTabs(), args[2], old, args[1], args[0]);
                    }
                    throw new EPSCommonException(String.format("%s overrides value %s at %s column with %s position", args[2], old, args[1], args[0]));
                }

                return old;
            }

            return result;
        }
    }

    private static final class DEBUG {

        static final boolean DEBUG_SORT = false;
        private final AtomicInteger debug_tabs = new AtomicInteger();

        private String openTabs() {
            return StringUtils.repeat("  ", debug_tabs.getAndIncrement());
        }
        private String currentTabs() {
            return StringUtils.repeat("  ",  debug_tabs.get());
        }
        private String closeTabs() {
            return StringUtils.repeat("  ", debug_tabs.decrementAndGet());
        }

    }
}
