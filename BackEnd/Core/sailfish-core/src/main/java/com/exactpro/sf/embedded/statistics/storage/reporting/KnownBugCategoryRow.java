/*******************************************************************************
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

package com.exactpro.sf.embedded.statistics.storage.reporting;

import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

public class KnownBugCategoryRow {
    private static final String BUG_SEPARATOR = ", ";
    private static final String OPEN_BRACKETS = "(";
    private static final String CLOSE_BRACKETS = ")";

    private String[] category;

    private TreeSet<String> reproducedBugs = new TreeSet<>();

    private TreeSet<String> nonReproducedBugs = new TreeSet<>();

    public KnownBugCategoryRow(String[] category) {
        this.category = category;
    }

    public String[] getCategory() {
        return category;
    }

    public void setCategory(String[] category) {
        this.category = category;
    }

    public TreeSet<String> getReproducedBugs() {
        return reproducedBugs;
    }

    public void setReproducedBugs(TreeSet<String> reproducedBugs) {
        this.reproducedBugs = reproducedBugs;
    }

    public TreeSet<String> getNonReproducedBugs() {
        return nonReproducedBugs;
    }

    public void setNonReproducedBugs(TreeSet<String> nonReproducedBugs) {
        this.nonReproducedBugs = nonReproducedBugs;
    }

    public String getCategoryString() {
        StringBuilder concatCategory = new StringBuilder(StringUtils.join(category, OPEN_BRACKETS));
        if (category.length > 0) {
            concatCategory.append(StringUtils.repeat(CLOSE_BRACKETS, category.length - 1));
        }
        return concatCategory.toString();
    }

    public String getReproducedBugsString() {
        return StringUtils.join(reproducedBugs, BUG_SEPARATOR);
    }

    public String getNonReproducedBugsString() {
        return StringUtils.join(nonReproducedBugs, BUG_SEPARATOR);
    }
}
