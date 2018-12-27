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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.help.helpmarshaller.jsoncontainers.HelpJsonContainer;


public class UtilSearchStrategy implements ISearchStrategy{
    @Override public void search(List<List<Integer>> jsonResult, HelpJsonContainer node, SearchOptions o, Map<String, Boolean> fileResult,
            Set<String> checkedFiles, String pluginName, List<Integer> rowKey) {
        if (!o.isSearchUtils()) {
            return;
        }
        if (Search.matchText(node.getName(), o)) {
            jsonResult.add(rowKey);
        }
    }
}
