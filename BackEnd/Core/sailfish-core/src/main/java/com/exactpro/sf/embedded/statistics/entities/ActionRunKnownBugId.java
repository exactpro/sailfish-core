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

package com.exactpro.sf.embedded.statistics.entities;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

@Embeddable
public class ActionRunKnownBugId implements Serializable {

    @ManyToOne(cascade = CascadeType.ALL)
    private ActionRun actionRun;

    @ManyToOne(cascade = CascadeType.ALL)
    private KnownBug knownBug;

    public ActionRunKnownBugId() {
    }

    public ActionRunKnownBugId(ActionRun actionRun, KnownBug knownBug) {
        this.actionRun = actionRun;
        this.knownBug = knownBug;
    }

    public ActionRun getActionRun() {
        return actionRun;
    }

    public void setActionRun(ActionRun actionRun) {
        this.actionRun = actionRun;
    }

    public KnownBug getKnownBug() {
        return knownBug;
    }

    public void setKnownBug(KnownBug knownBug) {
        this.knownBug = knownBug;
    }
}
