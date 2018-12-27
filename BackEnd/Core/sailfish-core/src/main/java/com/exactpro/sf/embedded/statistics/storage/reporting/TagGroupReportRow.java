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
package com.exactpro.sf.embedded.statistics.storage.reporting;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@SuppressWarnings("serial")
public class TagGroupReportRow implements Serializable {

	private String[] dimensionsPath;

	private long totalExecTime;

	private long totalTcCount;

	private long passedCount;

	private long failedCount;

	private long conditionallyPassedCount;

	private BigDecimal passedPercent;

	private BigDecimal failedPercent;

	private BigDecimal conditionallyPassedPercent;
	
	private int totalMatrices;

	private int failedMatrices;

	private String formattedExecTime;

	public String getPathEnd() {

		if(this.dimensionsPath != null) {
			return this.dimensionsPath[dimensionsPath.length-1];
		} else {
			return null;
		}

	}

	public String[] getDimensionsPath() {
		return dimensionsPath;
	}

	public void setDimensionsPath(String[] dimensionsPath) {
		this.dimensionsPath = dimensionsPath;
	}

	public long getTotalExecTime() {
		return totalExecTime;
	}

	public void setTotalExecTime(long totalExecTime) {
		this.totalExecTime = totalExecTime;
	}

	public long getTotalTcCount() {
		return totalTcCount;
	}

	public void setTotalTcCount(long totalTcCount) {
		this.totalTcCount = totalTcCount;
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

    public BigDecimal getPassedPercent() {
		return passedPercent;
	}

	public void setPassedPercent(BigDecimal passedPercent) {
		this.passedPercent = passedPercent;
	}

	public BigDecimal getFailedPercent() {
		return failedPercent;
	}

	public void setFailedPercent(BigDecimal failedPercent) {
		this.failedPercent = failedPercent;
	}

	public BigDecimal getConditionallyPassedPercent() {
        return conditionallyPassedPercent;
    }

    public void setConditionallyPassedPercent(BigDecimal conditionallyPassedPercent) {
        this.conditionallyPassedPercent = conditionallyPassedPercent;
    }

    public int getTotalMatrices() {
        return totalMatrices;
    }

    public void setTotalMatrices(int totalMatrices) {
        this.totalMatrices = totalMatrices;
    }

    public int getFailedMatrices() {
        return failedMatrices;
    }

    public void setFailedMatrices(int failedMatrices) {
        this.failedMatrices = failedMatrices;
    }

    @Override
	public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("dimensionsPath", dimensionsPath);
        builder.append("totalExecTime", totalExecTime);
        builder.append("totalTcCount", totalTcCount);
        builder.append("passedCount", passedCount);
        builder.append("conditionallyPassedCount", conditionallyPassedCount);
        builder.append("failedCount", failedCount);
        builder.append("passedPercent", passedPercent);
        builder.append("conditionallyPassedPercent", conditionallyPassedPercent);
        builder.append("failedPercent", failedPercent);
        builder.append("totalMatrices", totalMatrices);

        return builder.toString();
	}

	public String getFormattedExecTime() {
		return formattedExecTime;
	}

	public void setFormattedExecTime(String formattedExecTime) {
		this.formattedExecTime = formattedExecTime;
	}

}
