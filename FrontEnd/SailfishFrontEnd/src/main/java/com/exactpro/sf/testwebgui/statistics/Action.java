/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.testwebgui.statistics;


import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TestCaseRun;

import java.util.List;
import java.util.function.BiFunction;

public enum Action {
    ADD("Add") {
        @Override
        public BiFunction<TestCaseRun, List<Tag>, Boolean> getTargetAction() {
            return (testCaseRun, tags) -> testCaseRun.addTags(tags, true);
        }
    },
    REMOVE("Remove"){
        @Override
        public BiFunction<TestCaseRun, List<Tag>, Boolean> getTargetAction() {
            return (testCaseRun, tags) -> testCaseRun.removeTags(tags);
        }
    };

    private String label;

    private Action(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public abstract BiFunction<TestCaseRun, List<Tag>, Boolean> getTargetAction();
}
