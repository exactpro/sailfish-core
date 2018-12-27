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
package com.exactpro.sf.testwebgui.statistics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.temporal.ChronoUnit;

import com.exactpro.sf.embedded.statistics.entities.TestCase;
import com.exactpro.sf.embedded.statistics.storage.TestCaseHistoryRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="tcrHistoryBean")
@ViewScoped
@SuppressWarnings("serial")
public class TestCaseHistoryBean implements Serializable {
	
	private static final Logger logger = LoggerFactory.getLogger(TestCaseHistoryBean.class);
	
	private static final int WARN_DURATION_PERCENT = 20;
	
	private static final int GOOD_DURATION_PERCENT = 20;
	
	private static final String DURATION_FORMAT = "HH:mm:ss";
	
	private List<TestCaseHistoryRow> lastResult;
	
	private TestCase lastTestCase;
	
	private TestCase selectedTestCase;
	
	private TestCase unknownTc;
	
	private List<TestCase> lastCompleteResult;
	
	private long avgDuration;
	
	private void initDurations() {
		
		if(this.lastResult == null) {
			return;
		}
		
		long totalRuns = 0l;
		
		long sumDuration = 0l;
		
		for(TestCaseHistoryRow row : this.lastResult) {
			
			if(row.getFinished() != null) {
				
				long duration = ChronoUnit.MILLIS.between(row.getStarted(), row.getFinished());
				
				totalRuns++;
				sumDuration += duration;
				
				row.setDuration(duration);
				
				row.setDurationString(DurationFormatUtils.formatDuration(duration, DURATION_FORMAT));
				
			}
			
		}
		
		if(totalRuns != 0) {
		
			this.avgDuration = sumDuration / totalRuns;
		
		}
		
	}
	
	@PostConstruct
	public void init() {
		
		if(BeanUtil.getSfContext().getStatisticsService().isConnected()) {
		
			//this.allTestCases = BeanUtil.getSfContext().getStatisticsService().getStorage().getAllTestCases();
			
			unknownTc = BeanUtil.getSfContext().getStatisticsService().getStorage().loadUnknownTestCase();
		
		}
		
	}
	
	public List<TestCase> completeTc(String name) {
		
		List<TestCase> result = BeanUtil.getSfContext().getStatisticsService().getStorage().getTestCasesContains(name);
		
		result.remove(unknownTc);
		
		this.lastCompleteResult = result;
		
		return result;
		
	}
	
	public String getDurationClass(long duration) {
		
		long onePercent = this.avgDuration / 100l;
				
		long delta = Math.abs(this.avgDuration - duration);
				
		if(duration < this.avgDuration) { 
			
			if( delta > onePercent * GOOD_DURATION_PERCENT ) {
				return "good";
			}
			
		} else { 
			
			if( delta > onePercent * WARN_DURATION_PERCENT ) {
				return "warn";
			}
			
		}
		
		return "usual";
		
	}
	
	public void selectTestCase(String name) {
				
		this.selectedTestCase = BeanUtil.getSfContext().getStatisticsService().getStorage().getTestCaseByTcId(name);
		
	}
	
	public void generate() {
		
		try {
		
			AggregateReportParameters params = new AggregateReportParameters();
			
			params.setTestCaseId(this.selectedTestCase.getId());
			
			this.lastResult = BeanUtil.getSfContext().getStatisticsService().getReportingStorage().generateTestCaseHistoryReport(params);
			
			this.lastTestCase = this.selectedTestCase;
			
			initDurations();

            if(logger.isDebugEnabled()) {
                String lastResultString = lastResult == null ? null : Arrays.toString(lastResult.toArray());
                logger.debug("lastResults : {}", lastResultString);
            }
			
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			BeanUtil.addErrorMessage(e.getMessage(), "");
			
		}
		
	}

	public List<TestCaseHistoryRow> getLastResult() {
		return lastResult;
	}

	public void setLastResult(List<TestCaseHistoryRow> lastResult) {
		this.lastResult = lastResult;
	}

	public TestCase getSelectedTestCase() {
		return selectedTestCase;
	}

	public void setSelectedTestCase(TestCase selectedTestCase) {
		this.selectedTestCase = selectedTestCase;
	}

	public TestCase getLastTestCase() {
		return lastTestCase;
	}

	public void setLastTestCase(TestCase lastTestCase) {
		this.lastTestCase = lastTestCase;
	}

	public long getAvgDuration() {
		return avgDuration;
	}

	public void setAvgDuration(long avgDuration) {
		this.avgDuration = avgDuration;
	}

	public List<TestCase> getLastCompleteResult() {
		return lastCompleteResult;
	}

	public void setLastCompleteResult(List<TestCase> lastCompleteResult) {
		this.lastCompleteResult = lastCompleteResult;
	}
	
}
