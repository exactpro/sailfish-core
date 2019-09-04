/*
 * ****************************************************************************
 *  Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ****************************************************************************
 */

package com.exactpro.sf.scriptrunner.impl.jsonreport.beans;

import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BugCategory implements IJsonReportNode {

    private String name;

    @JsonIgnore
    private Set<BugCategory> subCategories = new HashSet<>();

    @JsonIgnore
    private Set<Bug> bugs = new HashSet<>();

    public BugCategory(String name) {
        this.name = name;
    }

    @JsonCreator
    public BugCategory(@JsonProperty("name") String name, @JsonProperty("subNodes") Collection<IJsonReportNode> subNodes) {
        this.name = name;
        this.bugs = subNodes.stream().filter(item -> item instanceof Bug).map(item -> (Bug) item).collect(Collectors.toSet());
        this.subCategories = subNodes.stream().filter(item -> item instanceof BugCategory).map(item -> (BugCategory) item).collect(Collectors.toSet());
    }

    public void placeBugInTree(Bug bug) {
        placeBugInATreeRecursive(new ArrayList<>(), this, bug);
    }

    private static void placeBugInATreeRecursive(List<String> path, BugCategory current, Bug bug) {
        List<String> bugCategories = bug.getDescription().getCategories().list();

        if (path.equals(bugCategories)) {
            current.addSubNodes(bug);
            return;
        }

        String nextCategoryName = bugCategories.stream().filter(name -> !path.contains(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("unable to find next bug category"));

        BugCategory nextCategory = current.getSubCategories().stream().filter(cat -> cat.getName().equals(nextCategoryName)).findAny()
                .orElse(new BugCategory(nextCategoryName));

        if (!current.subCategories.contains(nextCategory)) {
            current.addSubNodes(nextCategory);
        }

        path.add(nextCategoryName);
        placeBugInATreeRecursive(path, nextCategory, bug);
    }

    private Set<Bug> collectBugsFromTreeRecursive(BugCategory currentNode) {
        Set<Bug> result = new HashSet<>(currentNode.bugs);
        currentNode.subCategories.forEach(category -> result.addAll(collectBugsFromTreeRecursive(category)));
        return result;
    }

    @Override
    public void addSubNodes(Collection<? extends IJsonReportNode> nodes) {
        for (IJsonReportNode node : nodes) {
            if (node instanceof Bug) {
                bugs.add((Bug) node);
            } else if (node instanceof BugCategory) {
                subCategories.add((BugCategory) node);
            } else {
                throw new IllegalArgumentException("unsupported child node type: " + node.getClass());
            }
        }
    }

    @JsonProperty("subNodes")
    public List<IJsonReportNode> getSubNodes() {
        return Stream.concat(bugs.stream(), subCategories.stream()).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<BugCategory> getSubCategories() {
        return subCategories;
    }

    public Collection<Bug> getAllBugs() {
        return collectBugsFromTreeRecursive(this);
    }
}
