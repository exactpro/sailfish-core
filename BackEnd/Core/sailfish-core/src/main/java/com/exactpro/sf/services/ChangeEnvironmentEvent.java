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
package com.exactpro.sf.services;

import java.util.Date;

public class ChangeEnvironmentEvent extends EnvironmentEvent{
    private Status status;
    private String newEnvName;

    public ChangeEnvironmentEvent(String name, Date date, String message, Status status) {
        super(name, date, message);
        this.status = status;
    }

    public ChangeEnvironmentEvent(String name, String message, Status status) {
        super(name, message);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getNewEnvName() {
        return newEnvName;
    }

    public void setNewEnvName(String newEnvName) {
        this.newEnvName = newEnvName;
    }

    public enum Status{
        ADDED,
        DELETED,
        RENAMED
    }
}
