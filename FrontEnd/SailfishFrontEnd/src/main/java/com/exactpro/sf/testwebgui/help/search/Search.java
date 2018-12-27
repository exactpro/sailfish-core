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
package com.exactpro.sf.testwebgui.help.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.primefaces.model.TreeNode;

import com.exactpro.sf.help.HelpBuilder;
import com.exactpro.sf.help.helpmarshaller.HelpEntityName;
import com.exactpro.sf.help.helpmarshaller.HelpEntityType;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.HelpJsonContainer;
import com.exactpro.sf.testwebgui.BeanUtil;

public class Search {

    private static final String SOME_SYMBOL = "*";

    @SuppressWarnings("serial")
    private static final Map<HelpEntityType, ISearchStrategy> strategy = new HashMap<HelpEntityType, ISearchStrategy>() {{
        put(HelpEntityType.NAMED, new NamedSearchStrategy());
        put(HelpEntityType.ACTION, new ActionSearchStrategy());
        put(HelpEntityType.UTIL, new UtilSearchStrategy());
        put(HelpEntityType.METHOD, new MethodSearchStrategy());
        put(HelpEntityType.DICTIONARY, new DictionarySearchStrategy());
        put(HelpEntityType.FIELD, new FieldMessageSearchStrategy());
        put(HelpEntityType.MESSAGE, new FieldMessageSearchStrategy());
        put(HelpEntityType.COMPONENT, new ComponentSearchStrategy());
        put(HelpEntityType.ERROR, new NamedSearchStrategy());
    }};

    private Search() {
        // hide constructor
    }

    public static List<TreeNode> doSearch(SearchOptions o) {

        if (o.getRootNode() == null)
            return null;

        List<TreeNode> results = new ArrayList<>();

        findChilds(results, o.getRootNode(), o, new HashMap<String, Boolean>(), new HashSet<String>());

        return results;
    }

    private static void findChilds(List<TreeNode> result, TreeNode parentNode, SearchOptions o, Map<String, Boolean> fileResult,
            Set<String> checkedFiles) {

        Map<String, TreeNode> plugins = new TreeMap<>();

        if (o.isInSelection()) {
            plugins.put(BeanUtil.getHelpContentHolder().getPluginName(parentNode), parentNode);
        } else {
            for (TreeNode pluginNode : parentNode.getChildren()) {
                plugins.put(BeanUtil.getHelpContentHolder().getPluginName(pluginNode), pluginNode);
            }
        }

        List<List<Integer>> jsonResults = new ArrayList<>();

        for (String pluginName : plugins.keySet()) {
            TreeNode pluginNode = plugins.get(pluginName);

            List<Integer> rowKey = new ArrayList<>();
            if (!o.isInSelection()) {
                rowKey.add(BeanUtil.getHelpContentHolder().getNodeIndex(pluginNode));
            }
            findJsonChilds(jsonResults, BeanUtil.getHelpContentHolder().getContainer(pluginNode), o, fileResult, checkedFiles, pluginName, rowKey);

        }

        for (List<Integer> jsonResult : jsonResults) {

            TreeNode resultNode = parentNode;

            for (Integer nodeIndex : jsonResult) {

                if (resultNode.getChildren().get(0).getData() == null) {
                    BeanUtil.getHelpContentHolder().buildFromJson(resultNode, BeanUtil.getHelpContentHolder().getPluginName(resultNode));

                    if (!result.isEmpty() && result.get(result.size() - 1).getRowKey().equals(resultNode.getRowKey())) {
                        result.set(result.size() - 1, resultNode);
                    }

                    if (resultNode.getRowKey().equals(parentNode.getRowKey())) {
                        parentNode = resultNode;
                    }

                }

                resultNode = resultNode.getChildren().get(nodeIndex);
            }

            result.add(resultNode);

        }

    }

    protected static boolean searchInFile(HelpJsonContainer container, SearchOptions o, Map<String, Boolean> fileResult, Set<String> checkedFiles,
            String pluginName) {

        String filePath = container.getFilePath();

        if (checkedFiles.add(filePath)) {

            String description = BeanUtil.getHelpContentHolder().getDescription(container, pluginName);

            fileResult.put(filePath, Search.matchText(description, o));
        }

        return fileResult.get(filePath);
    }

    public static List<List<Integer>> searchFromJson(List<List<Integer>> jsonResult, HelpJsonContainer parentNode, SearchOptions o,
            Map<String, Boolean> fileResult, Set<String> checkedFiles, String pluginName, List<Integer> rowKey) {

        parentNode.setChildNodes(BeanUtil.getHelpContentHolder().getContainer(parentNode, pluginName).getChildNodes());

        findJsonChilds(jsonResult, parentNode, o, fileResult, checkedFiles, pluginName, rowKey);

        return jsonResult;

    }

    private static void findJsonChilds(List<List<Integer>> jsonResult, HelpJsonContainer parentNode, SearchOptions o, Map<String, Boolean> fileResult,
            Set<String> checkedFiles, String pluginName, List<Integer> rowKey) {

        List<HelpJsonContainer> children = parentNode.getChildNodes();

        if (children == null) {
            if (parentNode.getFilePath().contains(HelpBuilder.JSON) && parentNode.getChildNodes() == null) {
                searchFromJson(jsonResult, parentNode, o, fileResult, checkedFiles, pluginName, rowKey);

            }
            return;
        }

        for (int i = 0; i < children.size(); i++) {

            if (jsonResult.size() == SearchOptions.MAX_SEARCH_SIZE) {
                return;
            }

            HelpJsonContainer childNode = children.get(i);

            List<Integer> currentRowKey = new ArrayList<>(rowKey);
            currentRowKey.add(i);

            if ((!o.isSearchDictionaries() && HelpEntityName.DICTIONARIES.getValue().equals(childNode.getName()))
                    || (!o.isSearchActions()) && HelpEntityName.ACTIONS.getValue().equals(childNode.getName())) {
                continue;
            }

            strategy.get(childNode.getType()).search(jsonResult, childNode, o, fileResult, checkedFiles, pluginName, currentRowKey);

            findJsonChilds(jsonResult, childNode, o, fileResult, checkedFiles, pluginName, currentRowKey);
        }
    }

    public static boolean matchText(final String string, final SearchOptions o) {

        String text = string;
        String pattern = o.getSearchText();

        if (o.isIgnoreCase()) {
            text = text.toLowerCase();
            pattern = pattern.toLowerCase();
        }

        if (!pattern.contains(SOME_SYMBOL)) {
            return text.contains(pattern);
        }

        pattern = pattern.replaceAll("\\" + SOME_SYMBOL, ".+");
        return text.matches(pattern);
    }
}
