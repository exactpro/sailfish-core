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

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "stactionruns_known_bugs")
@AssociationOverrides({
        @AssociationOverride(name = "id.actionRun",
            joinColumns = @JoinColumn(name = "stactionrun_id")),
        @AssociationOverride(name = "id.knownBug",
            joinColumns = @JoinColumn(name = "known_bug_id"))
})
public class ActionRunKnownBug {

    @EmbeddedId
    private ActionRunKnownBugId id = new ActionRunKnownBugId();

    private Boolean reproduced;

    public ActionRunKnownBug() {
    }

    public ActionRunKnownBug(ActionRun actionRun, KnownBug knownBug, Boolean reproduced) {
        id.setActionRun(actionRun);
        id.setKnownBug(knownBug);
        this.reproduced = reproduced;
    }

    public ActionRun getActionRun() {
        return id.getActionRun();
    }

    public void setActionRun(ActionRun actionRun) {
        id.setActionRun(actionRun);
    }

    public KnownBug getKnownBug() {
        return id.getKnownBug();
    }

    public void setKnownBug(KnownBug knownBug) {
        id.setKnownBug(knownBug);
    }

    public Boolean getReproduced() {
        return reproduced;
    }

    public void setReproduced(Boolean reproduced) {
        this.reproduced = reproduced;
    }
}
