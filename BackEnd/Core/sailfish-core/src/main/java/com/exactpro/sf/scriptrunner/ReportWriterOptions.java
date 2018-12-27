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

import java.io.Serializable;
import java.util.Date;

import com.exactpro.sf.scriptrunner.reportbuilder.ReportType;

@SuppressWarnings("serial")
public class ReportWriterOptions implements Serializable {
	
	public enum Duration { Today, Week, Month, Custom };
	
	private Duration selectedDuration;
	
	private boolean writeDetails = true;
	
	private Date customStart;
	private Date customEnd;
	
	private ReportType reportType;
	
	public Date getCustomStart() {
		return customStart;
	}
	public void setCustomStart(Date customStart) {
		this.customStart = customStart;
	}
	public Date getCustomEnd() {
		return customEnd;
	}
	public void setCustomEnd(Date customEnd) {
		this.customEnd = customEnd;
	}
	public boolean isWriteDetails() {
		return writeDetails;
	}
	public void setWriteDetails(boolean writeDetails) {
		this.writeDetails = writeDetails;
	}
	public Duration getSelectedDuration() {
		return selectedDuration;
	}
	public void setSelectedDuration(Duration selectedDuration) {
		this.selectedDuration = selectedDuration;
	}
	public ReportType getSelectedReportType() {
		return reportType;
	}
	public void setSelectedReportType(ReportType reportType) {
		this.reportType = reportType;
	}
}
