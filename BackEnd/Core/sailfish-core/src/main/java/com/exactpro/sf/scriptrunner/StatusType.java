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
package com.exactpro.sf.scriptrunner;

import com.exactpro.sf.common.util.EPSCommonException;

public enum StatusType
{
	PASSED(1),
	CONDITIONALLY_PASSED(2),
	FAILED(0),
	CONDITIONALLY_FAILED(3),
	SKIPPED(4),
	NA(5);

    private final int id;

    private StatusType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static StatusType getStatusType(int id) {
        for(StatusType statusType : StatusType.values()) {
            if(statusType.id == id) {
                return statusType;
            }
        }

        throw new EPSCommonException("Unknown status type id: " + id);
    }

    public static StatusType getStatusType(String name) {
        for(StatusType statusType : StatusType.values()) {
            if(statusType.name().equalsIgnoreCase(name)) {
                return statusType;
            }
        }

        throw new EPSCommonException("Unknown status type name: " + name);
    }
}
