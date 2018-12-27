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

import java.util.Collections;
import java.util.Set;

import com.exactpro.sf.util.BugDescription;

public class StatusDescription
{
	private final StatusType status;

	private final String description;

	private final Throwable cause;

	private final boolean updateTestCaseStatus;

    private final Set<BugDescription> knownBugs;

	public StatusDescription(StatusType status, String description)
	{
        this(status, description, true, null);
	}

    public StatusDescription(StatusType status, String description, boolean updateTestCaseStatus) {
        this(status, description, updateTestCaseStatus, null);
    }

    public StatusDescription(StatusType status, String description, Set<BugDescription> knownBugs) {
        this(status, description, null, true, knownBugs);
    }

	public StatusDescription(StatusType status, String description, Throwable cause)
	{
        this(status, description, cause, true, null);
	}

    public StatusDescription(StatusType status, String description, Throwable cause, Set<BugDescription> knownBugs) {
        this(status, description, cause, true, knownBugs);
    }

    public StatusDescription(StatusType status, String description, Throwable cause, boolean updateTestCaseStatus) {
        this(status, description, cause, updateTestCaseStatus, null);
    }

    public StatusDescription(StatusType status, String description, boolean updateTestCaseStatus, Set<BugDescription> knownBugs)
	{
		this.status = status;

		this.description = description;

		this.cause = null;

		this.updateTestCaseStatus = updateTestCaseStatus;

        if (knownBugs == null) {
            this.knownBugs = Collections.emptySet();
        } else {
            this.knownBugs = Collections.unmodifiableSet(knownBugs);
        }
	}

    public StatusDescription(StatusType status, String description, Throwable cause, boolean updateTestCaseStatus, Set<BugDescription> knownBugs)
	{
		this.status = status;

		this.description = description;

		this.cause = cause;

		this.updateTestCaseStatus = updateTestCaseStatus;

        if (knownBugs == null) {
            this.knownBugs = Collections.emptySet();
        } else {
            this.knownBugs = Collections.unmodifiableSet(knownBugs);
        }
	}

	public StatusType getStatus() {
		return status;
	}

	public String getDescription() {
		return description;
	}

	public Throwable getCause() {
		return cause;
	}

	public boolean isUpdateTestCaseStatus() {
		return updateTestCaseStatus;
	}

    public Set<BugDescription> getKnownBugs() {
        return knownBugs;
    }
}
