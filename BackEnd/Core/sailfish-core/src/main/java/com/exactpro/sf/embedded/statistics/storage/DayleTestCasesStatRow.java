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
package com.exactpro.sf.embedded.statistics.storage;

import java.util.Date;

import com.exactpro.sf.util.DateTimeUtility;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import java.time.LocalDateTime;

public class DayleTestCasesStatRow {

    private LocalDateTime date;

	private long passedCount;

	private long failedCount;

	private long conditionallyPassedCount;

	public DayleTestCasesStatRow(Date date,
			long passedCount, long conditionallyPassedCount, long failedCount) {

		super();

        this.date = DateTimeUtility.toLocalDateTime(date);

		this.passedCount = passedCount;
		this.conditionallyPassedCount = conditionallyPassedCount;
		this.failedCount = failedCount;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public long getPassedCount() {
		return passedCount;
	}

	public void setPassedCount(long passedCount) {
		this.passedCount = passedCount;
	}

	public long getFailedCount() {
		return failedCount;
	}

	public void setFailedCount(long failedCount) {
		this.failedCount = failedCount;
	}

	public long getConditionallyPassedCount() {
        return conditionallyPassedCount;
    }

    public void setConditionallyPassedCount(long conditionallyPassedCount) {
        this.conditionallyPassedCount = conditionallyPassedCount;
    }

    @Override
	public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("date", date);
        builder.append("passedCount", passedCount);
        builder.append("conditionallyPassedCount", conditionallyPassedCount);
        builder.append("failedCount", failedCount);

		return builder.toString();
	}


}
