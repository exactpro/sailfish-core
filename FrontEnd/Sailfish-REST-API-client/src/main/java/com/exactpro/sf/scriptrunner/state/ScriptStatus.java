/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.scriptrunner.state;

public enum ScriptStatus {
    INIT_FAILED("INIT FAILED", 0), // initialization failed
    INTERRUPTED("INTERRUPTED", 1), // script was interrupted
    EXECUTED("EXECUTED", 2),
    NOT_STARTED("NOT STARTED", 3),
    NONE("NONE", 4),
    CANCELED("CANCELED", 5),
    RUN_FAILED("RUN FAILED", 6);

    private final String title;
    private final int weight;

    ScriptStatus(String title, int weight) {
        this.title = title;
        this.weight = weight;
    }

    public String getTitle() {
        return title;
    }

    public int getWeight() {
        return weight;
    }
}
