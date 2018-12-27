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
package com.exactpro.sf.scriptrunner.impl.htmlreport.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.util.BugDescription;

public abstract class BaseEntity {
    private String name;
    private String description;
    private List<Object> elements;
    private Set<BugDescription> allKnownBugs;
    private Set<BugDescription> reproducedBugs;

    public BaseEntity() {
        elements = new ArrayList<>();
        allKnownBugs = new HashSet<>();
        reproducedBugs = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Object> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void addElement(Object element) {
        elements.add(element);
    }

    public Set<BugDescription> getAllKnownBugs() {
        return Collections.unmodifiableSet(allKnownBugs);
    }

    public void addAllKnownBugs(Set<BugDescription> allKnownBugs) {
        this.allKnownBugs.addAll(allKnownBugs);
    }

    public Set<BugDescription> getReproducedBugs() {
        return Collections.unmodifiableSet(reproducedBugs);
    }

    public void addReproducedBugs(Set<BugDescription> reproducedBugs) {
        this.reproducedBugs.addAll(reproducedBugs);
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("name", name);
        builder.append("description", description);
        builder.append("elements", elements.size());
        builder.append("allKnownBugs", allKnownBugs);
        builder.append("reproducedBugs", reproducedBugs);

        return builder.toString();
    }
}
