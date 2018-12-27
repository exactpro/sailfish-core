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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class AMLBlockProcessor {
    public static ListMultimap<AMLBlockType, AMLTestCase> process(ListMultimap<AMLBlockType, AMLTestCase> allBlocks, AMLSettings settings, IActionManager actionManager) throws AMLException {
        AlertCollector alertCollector = new AlertCollector();

        List<AMLTestCase> testCases = allBlocks.get(AMLBlockType.TestCase);
        List<AMLTestCase> blocks = allBlocks.get(AMLBlockType.Block);
        List<AMLTestCase> beforeTCBlocks = allBlocks.get(AMLBlockType.BeforeTCBlock);
        List<AMLTestCase> afterTCBlocks = allBlocks.get(AMLBlockType.AfterTCBlock);
        List<AMLTestCase> firstBlocks = allBlocks.get(AMLBlockType.FirstBlock);
        List<AMLTestCase> lastBlocks = allBlocks.get(AMLBlockType.LastBlock);
        List<AMLTestCase> globalBlocks = allBlocks.get(AMLBlockType.GlobalBlock);

        retainExecutableTestCases(testCases, settings);

        if(testCases.isEmpty()) {
            alertCollector.add(new Alert("Nothing to execute"));
        }

        checkBlockReferences(testCases, alertCollector);
        checkBlockReferences(blocks, alertCollector);
        checkBlockReferences(beforeTCBlocks, alertCollector);
        checkBlockReferences(afterTCBlocks, alertCollector);

        insertGlobalBlocks(testCases, globalBlocks, alertCollector);
        insertFirstAndLastBlocks(testCases, firstBlocks, lastBlocks);

        Set<String> invalidBlockReferences = getRecursiveBlockReferences(blocks, alertCollector);

        insertIncludeBlocks(testCases, blocks, invalidBlockReferences, alertCollector, actionManager);
        insertIncludeBlocks(beforeTCBlocks, blocks, invalidBlockReferences, alertCollector, actionManager);
        insertIncludeBlocks(afterTCBlocks, blocks, invalidBlockReferences, alertCollector, actionManager);

        copyStaticActions(testCases, AMLLangConst.AML2.matches(settings.getLanguageURI()), alertCollector);
        setLastOutcomes(testCases);

        ListMultimap<AMLBlockType, AMLTestCase> processedBlocks = ArrayListMultimap.create();

        processedBlocks.putAll(AMLBlockType.TestCase, testCases);
        processedBlocks.putAll(AMLBlockType.BeforeTCBlock, beforeTCBlocks);
        processedBlocks.putAll(AMLBlockType.AfterTCBlock, afterTCBlocks);

        for(AMLTestCase testCase : allBlocks.values()) {
            for(AMLAction action : testCase.getActions()) {
                List<String> dependencies = action.getDependencies();

                if(dependencies.isEmpty()) {
                    continue;
                }

                for(String dependency : dependencies) {
                    AMLAction dependencyAction = testCase.findActionByRef(dependency);

                    if(dependencyAction == null) {
                        String reference = ObjectUtils.defaultIfNull(action.getReference(), action.getReferenceToFilter());
                        alertCollector.add(new Alert(action.getLine(), reference, Column.Dependencies.getName(), "Dependency on unknown action: " + dependency));
                    }

                    if(dependencyAction == action) {
                        alertCollector.add(new Alert(action.getLine(), dependency, Column.Dependencies.getName(), "Action cannot depend on itself"));
                    }
                }
            }
        }

        if(alertCollector.getCount(AlertType.ERROR) > 0) {
            throw new AMLException("Failed to process blocks", alertCollector);
        }

        return processedBlocks;
    }

    private static void checkBlockReferences(List<AMLTestCase> blocks, AlertCollector alertCollector) {
        Map<String, AMLTestCase> referenceToBlock = new HashMap<>();

        for(AMLTestCase block : blocks) {
            if(!block.hasReference()) {
                continue;
            }

            String reference = block.getReference();
            AMLTestCase otherBlock = referenceToBlock.get(reference);

            if(otherBlock != null) {
                addError(alertCollector, block, Column.Reference, "Duplicate reference at line: " + otherBlock.getLine());
            } else {
                referenceToBlock.put(reference, block);
            }
        }
    }

    private static void retainExecutableTestCases(List<AMLTestCase> testCases, AMLSettings settings) {
        if(testCases.isEmpty()) {
            return;
        }

        AMLTestCase lastTestCase = testCases.get(testCases.size() - 1);
        Set<Integer> range = StringUtil.parseRange(settings.getTestCasesRange(), lastTestCase.getMatrixOrder() + 1);

        for(Iterator<AMLTestCase> it = testCases.iterator(); it.hasNext();) {
            AMLTestCase testCase = it.next();

            if(!range.contains(testCase.getMatrixOrder()) || testCase.isEmpty()) {
                it.remove();
            }
        }
    }

    private static void setLastOutcomes(List<AMLTestCase> testCases) {
        for(AMLTestCase testCase : testCases) {
            Map<String, AMLAction> lastOutcome = new HashMap<>();
            Map<String, AMLAction> groupFinished = new HashMap<>();

            for(AMLAction action : testCase.getActions()) {
                if(action.getOutcome() != null) {
                    lastOutcome.put(action.getOutcome(), action);
                    groupFinished.put(action.getOutcomeGroup(), action);
                }
            }

            for(AMLAction action : lastOutcome.values()) {
                action.setLastOutcome(true);
            }

            for(AMLAction action : groupFinished.values()) {
                action.setGroupFinished(true);
            }
        }
    }

    private static void insertGlobalBlocks(List<AMLTestCase> testCases, List<AMLTestCase> globalBlocks, AlertCollector alertCollector) {
        List<AMLAction> globalActions = new ArrayList<>();

        for(AMLTestCase block : globalBlocks) {
            globalActions.addAll(block.getActions());
        }

        for(AMLTestCase testCase : testCases) {
            for(int i = 0; i < globalActions.size(); i++) {
                try {
                    testCase.addAction(i, globalActions.get(i).clone());
                } catch(AMLException e) {
                    alertCollector.add(new Alert(e.getMessage()));
                }
            }
        }
    }

    private static void insertFirstAndLastBlocks(List<AMLTestCase> testCases, List<AMLTestCase> firstBlocks, List<AMLTestCase> lastBlocks) {
        testCases.addAll(0, firstBlocks);
        testCases.addAll(lastBlocks);

        for(int i = 0; i < testCases.size(); i++) {
            testCases.get(i).setExecOrder(i + 1);
        }
    }

    private static void insertIncludeBlocks(List<AMLTestCase> blocks, List<AMLTestCase> includeBlocks, Set<String> invalidBlockReferences, AlertCollector alertCollector, IActionManager actionManager) {
        for(AMLTestCase block : blocks) {
            insertIncludeBlocks(block, includeBlocks, invalidBlockReferences, alertCollector, actionManager);
        }
    }

    private static void insertIncludeBlocks(AMLTestCase block, List<AMLTestCase> blocks, Set<String> invalidBlockReferences, AlertCollector alertCollector, IActionManager actionManager) {
        List<AMLAction> actions = block.getActions();
        int actionsSize = actions.size();

        for(int i = 0; i < actionsSize; i++) {
            AMLAction action = actions.get(i);

            if(JavaStatement.INCLUDE_BLOCK == JavaStatement.value(action.getActionURI())) {
                String blockRef = action.getTemplate();

                if(invalidBlockReferences.contains(blockRef)) {
                    actions.remove(i--);
                    actionsSize--;

                    continue;
                }

                AMLTestCase includeBlock = getBlockByReference(blockRef, blocks);

                if(includeBlock == null) {
                    actions.remove(i--);
                    actionsSize--;

                    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.Template.getName(), "Reference to unknown block: " + blockRef));

                    continue;
                }

                action.setActionURI(AMLLangConst.INIT_MAP_URI);
                action.setActionInfo(actionManager.getActionInfo(AMLLangConst.INIT_MAP_URI));

                String actionRef = action.getReference();
                List<AMLAction> blockActions = includeBlock.clone().getActions();

                for(AMLAction blockAction : blockActions) {
                    blockAction.setIncludeBlockReference(actionRef);

                    if(!block.isAddToReport()) {
                        blockAction.setAddToReport(false);
                    }
                }

                AMLAction refAction = action.clone();

                refAction.getParameters().clear();
                refAction.setTemplate(null);
                refAction.setTag(null);

                action.setIncludeBlockReference(actionRef);
                action.setReference(blockRef);
                action.setTemplate(null);

                actions.add(i, refAction);
                actions.addAll(i + 2, blockActions);

                actionsSize += blockActions.size() + 1;
            }
        }
    }

    private static Set<String> getRecursiveBlockReferences(List<AMLTestCase> blocks, AlertCollector alertCollector) {
        Stack<String> refStack = new Stack<>();
        Stack<AMLAction> actionStack = new Stack<>();
        Set<String> recursiveBlocks = new HashSet<>();

        for(AMLTestCase b : blocks) {
            checkBlock(b, blocks, refStack, actionStack, recursiveBlocks, alertCollector);
        }

        return recursiveBlocks;
    }

    private static void checkBlock(AMLTestCase block, List<AMLTestCase> blocks, Stack<String> refStack, Stack<AMLAction> actionStack, Set<String> recursiveBlocks, AlertCollector alertCollector) {
        for(AMLAction a : block.getActions()) {
            if(JavaStatement.INCLUDE_BLOCK == JavaStatement.value(a.getActionURI())) {
                String blockReference = a.getTemplate();
                AMLTestCase refBlock = getBlockByReference(blockReference, blocks);

                if(refBlock == null) {
                    alertCollector.add(new Alert(a.getLine(), a.getUID(), a.getReference(), Column.Template.getName(), "Reference to unknown block: " + blockReference));
                    continue;
                }

                if(refStack.contains(blockReference)) {
                    AMLAction firstAction = actionStack.firstElement();
                    List<String> path = new ArrayList<>(refStack);

                    path.add(blockReference);
                    recursiveBlocks.addAll(refStack);

                    alertCollector.add(new Alert(firstAction.getLine(), a.getUID(), firstAction.getReference(), "Recursion detected: " + StringUtils.join(path, " -> ")));

                    continue;
                }

                refStack.push(blockReference);
                actionStack.push(a);
                checkBlock(refBlock, blocks, refStack, actionStack, recursiveBlocks, alertCollector);
                actionStack.pop();
                refStack.pop();
            }
        }
    }

    private static AMLTestCase getBlockByReference(String reference, List<AMLTestCase> blocks) {
        if(reference == null) {
            return null;
        }

        for(AMLTestCase block : blocks) {
            if(reference.equals(block.getReference())) {
                return block;
            }
        }

        return null;
    }

    private static void copyStaticActions(List<AMLTestCase> testCases, boolean aml2, AlertCollector alertCollector) {
        Map<String, AMLAction> allActions = new HashMap<>();
        Map<String, AMLAction> currentActions = new HashMap<>();

        for(AMLTestCase testCase : testCases) {
            for(AMLAction action : testCase.getActions()) {
                boolean setStatic = JavaStatement.SET_STATIC.getURI().equals(action.getActionURI());

                if(!setStatic && (aml2 || !action.isStaticAction())) {
                    continue;
                }

                currentActions.put(action.getReference(), action);
            }

            for(AMLAction action : allActions.values()) {
                try {
                    testCase.addAction(action);
                } catch(AMLException e) {
                    // ignore duplicate reference because it can appear
                    // if static variable is defined in other block
                }
            }

            allActions.putAll(currentActions);
            currentActions.clear();
        }
    }

    private static void addError(AlertCollector alertCollector, AMLTestCase block, Column column, String message, Object... args) {
        alertCollector.add(new Alert(block.getLine(), block.getUID(), block.getReference(), column != null ? column.getName() : null, String.format(message, args)));
    }
}
