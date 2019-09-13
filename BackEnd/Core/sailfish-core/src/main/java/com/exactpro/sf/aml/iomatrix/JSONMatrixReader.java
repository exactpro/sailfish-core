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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.reflect.Reflection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JSONMatrixReader implements IMatrixReader {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private final DEBUG dbg = new DEBUG();

    private final Iterator<Integer> rowIterator;
    private final Supplier<String[]> rowsSupplier;

    private final AtomicInteger tableRowCounter = new AtomicInteger(1);

    public JSONMatrixReader(File file, MatrixFileTypes type) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {

            ObjectMapper objectMapper;
            switch (type) {
            case JSON:
                objectMapper = new ObjectMapper();
                break;
            case YAML:
                objectMapper = new ObjectMapper(new YAMLFactory());
                break;
            default:
                throw new EPSCommonException("Unsupported matrix type " + type);
            }

            objectMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

            JsonNode rootNode = objectMapper.reader().readTree(inputStream);
            Pair<Iterator<Integer>, Supplier<String[]>> parseResult = parseToTable(rootNode);
            rowIterator = parseResult.getFirst();
            rowsSupplier = parseResult.getSecond();
        }
    }

    private Pair<Iterator<Integer>, Supplier<String[]>> parseToTable(JsonNode rootNode) throws IOException {

        Table<Integer, String, String> table = Reflection.newProxy(Table.class, new SafetyCheckInvocationHandler());

        rootNode.elements().forEachRemaining(testCaseWrapper -> {
            if (testCaseWrapper.size() > 1) {
                throw new EPSCommonException("Too many nodes in testCase wrapper");
            }

            testCaseWrapper.fields().forEachRemaining(commonBlockEntry -> {

                String commonBlockType = commonBlockEntry.getKey();
                JsonNode commonBlock = commonBlockEntry.getValue();
                AMLBlockBrace blockBrace = AMLBlockBrace.value(commonBlockType);

                Objects.requireNonNull(commonBlock, "'AML block' node must be presented");
                Objects.requireNonNull(blockBrace, "Unknown block type " + commonBlockType);

                table.put(tableRowCounter.getAndIncrement(), Column.Action.getName(), commonBlockType);

                commonBlock.fields().forEachRemaining(actionEntry -> {
                    String reference = actionEntry.getKey();
                    JsonNode actionNode = actionEntry.getValue();

                    logger.debug("reading {}", reference);

                    int nestedCount = countNestedReferences(actionNode);
                    int target = tableRowCounter.get() + nestedCount;

                    table.put(target, Column.Reference.getName(), reference);
                    consumeNode(actionNode, table, target);
                    //FIXME will add additional empty row at last action
                    tableRowCounter.getAndIncrement();
                });

                table.put(tableRowCounter.getAndIncrement(), Column.Action.getName(), blockBrace.getInversed().getName());
            });
        });

        Set<String> columns = ImmutableSet.<String>builder().addAll(table.columnKeySet()).add(Column.Id.getName()).build();
        columns.forEach(column -> table.put(0, column, column));

        Iterator<Integer> rowIterator = dbg.DEBUG_SORT ? table.rowKeySet().iterator() : new TreeSet<>(table.rowKeySet()).iterator();

        Supplier<String[]> supplier = () -> {
            throw new NoSuchElementException();
        };
        //preserve header
        if (rowIterator.hasNext()) {

            supplier = () -> {
                int currentRow = rowIterator.next();
                Map<String, String> row = new HashMap<>(table.row(currentRow));
                if (!row.containsKey(Column.Id.getName())) {
                    row.put(Column.Id.getName(), String.valueOf(currentRow));
                }

                return columns.stream()
                        .map(key -> row.getOrDefault(key, ""))
                        .toArray(String[]::new);
            };
        }

        return new Pair<>(rowIterator, supplier);
    }

    @Override
    public SimpleCell[] readCells() throws IOException {

        return Stream.of(read())
                .map(SimpleCell::new)
                .toArray(SimpleCell[]::new);
    }

    @Override
    public String[] read() throws IOException {

        return rowsSupplier.get();
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
    private String consumeNode(JsonNode node, Table<Integer, String, String> toTable, int target) {

        if (!node.fields().hasNext()) {
            throw new EPSCommonException("Passed node without properties");
        }

        node.fields().forEachRemaining(actionFieldsEntry -> {
            String actionFieldKey = actionFieldsEntry.getKey();
            JsonNode actionFieldNode = actionFieldsEntry.getValue();

            Consumer<String> actionFieldPut = value -> toTable.put(target, actionFieldKey, value);

            logger.debug("{}consuming {} with {}", dbg.openTabs(), actionFieldNode.getNodeType(), actionFieldKey);

            Function<JsonNode, String> processObj = jsonNode -> {
                int nestedLvls = countNestedReferences(jsonNode);
                String ref = consumeNode(jsonNode, toTable, tableRowCounter.get() + nestedLvls);
                tableRowCounter.incrementAndGet();
                return ref;
            };

            if (actionFieldNode.isObject()) {
                actionFieldPut.accept("[" + processObj.apply(actionFieldNode) + "]");
            } else if (actionFieldNode.isArray()) {
                String ref = wrapIter(actionFieldNode.elements())
                        //FIXME need unwrap ref syntax? or write only ref name in json
                        .map(elNode -> elNode.isObject() ? processObj.apply(elNode) : elNode.textValue())
                        .collect(Collectors.joining(","));

                actionFieldPut.accept("[" + ref + "]");
            } else {
                actionFieldPut.accept(actionFieldNode.asText());
            }

            logger.debug("{}consuming {} with {} done -> {}",
                    dbg.closeTabs(), actionFieldNode.getNodeType(), actionFieldKey, toTable.get(target, Column.Reference.getName()));
        });

        if (!toTable.contains(target, Column.Reference.getName())) {
            toTable.put(target, Column.Reference.getName(), "implicit_ref" + target);
        }

        return toTable.get(target, Column.Reference.getName());
    }

    /***
     * Compute nested inline object nodes for reserve place for it
      * @param node - json node
     * @return count of rows to reserve
     */
    private int countNestedReferences(JsonNode node) {

        int counter = 0;

        for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
            JsonNode el = it.next();
            if (el.isObject()) {
                counter += 1 + countNestedReferences(el);
            } else if (el.isArray()) {
                counter += wrapIter(el.elements())
                        .mapToInt(n -> 1 + countNestedReferences(n))
                        .sum();
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

                String old = (String) result;

                if (old != null && !old.isEmpty()) {
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
