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

import java.util.Date;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.util.BugDescription;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

public class Report extends BaseEntity {
    private String host;
    private String user;
    private long id;
    private Date date;
    private SetMultimap<String, BugDescription> allKnownBugs = LinkedHashMultimap.create();
    private SetMultimap<String, BugDescription> reproducedBugs = LinkedHashMultimap.create();

    public Report() {
        super();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void putAllKnownBugs(String testCaseDescription, Set<BugDescription> bugDescriptions) {
        allKnownBugs.putAll(testCaseDescription, bugDescriptions);
    }

    public void putReproducedBugs(String testCaseDescription, Set<BugDescription> bugDescriptions) {
        reproducedBugs.putAll(testCaseDescription, bugDescriptions);
    }

    public SetMultimap<String, BugDescription> getAllKnownBugsMap() {
        return ImmutableSetMultimap.copyOf(allKnownBugs);
    }

    public SetMultimap<String, BugDescription> getReproducedBugsMap() {
        return ImmutableSetMultimap.copyOf(reproducedBugs);
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("host", host);
        builder.append("user", user);
        builder.append("id", id);
        builder.append("date", date);
        builder.append(super.toString());

        return builder.toString();
    }
}
