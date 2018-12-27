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

import java.io.Serializable;

import com.exactpro.sf.embedded.statistics.StatisticsUtils;
import com.exactpro.sf.util.BugDescription;

public class KnownBugRow implements Serializable {

    private String subject;

    private String[] categories;

    private boolean reproduced;

    public KnownBugRow(String knownBugView, Boolean reproduced) {
        BugDescription bugDescription = StatisticsUtils.restoreKnownBugFromJson(knownBugView);
        this.subject = bugDescription.getSubject().toUpperCase();
        this.categories = bugDescription.getCategories().list().toArray(new String[0]);
        this.reproduced = reproduced;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public boolean getReproduced() {
        return reproduced;
    }

    public void setReproduced(boolean reproduced) {
        this.reproduced = reproduced;
    }
}
