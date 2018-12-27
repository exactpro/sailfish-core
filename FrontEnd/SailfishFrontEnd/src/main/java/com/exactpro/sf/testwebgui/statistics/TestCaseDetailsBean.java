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
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.StatisticsUtils;
import com.exactpro.sf.embedded.statistics.storage.reporting.ActionInfoRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.KnownBugCategoryRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.KnownBugRow;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="tcDetailsBean")
@ViewScoped
@SuppressWarnings("serial")
public class TestCaseDetailsBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(TestCaseDetailsBean.class);

	private long tcrId;

	private List<ActionInfoRow> lastFailedResult;

    private List<ActionInfoRow> lastTaggedResult;

    private List<KnownBugCategoryRow> lastKnownBugsResult;

    private String knownBugsHeader;

	public void generateFailedReport() {

		AggregateReportParameters params = new AggregateReportParameters();

		params.setTestCaseRunId(tcrId);

		try {

			this.lastFailedResult = BeanUtil.getSfContext().getStatisticsService().getReportingStorage().generateFailedActionsInfo(params);

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			BeanUtil.addErrorMessage(e.getMessage(), "");

		}

	}

    public void generateTaggedReport() {
        AggregateReportParameters params = new AggregateReportParameters();
        params.setTestCaseRunId(tcrId);

        try {
            this.lastTaggedResult = BeanUtil.getSfContext().getStatisticsService().getReportingStorage().generateTaggedActionsInfo(params);
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            BeanUtil.addErrorMessage(e.getMessage(), "");
        }
    }

    public void initKnownBugsReport(long tcrId, long reproducedCount, long nonReproducedCount) {
        this.tcrId = tcrId;
        this.knownBugsHeader = String.format("Reproduced: %d / Non-reproduced: %d / Total: %d", reproducedCount, nonReproducedCount, reproducedCount + nonReproducedCount);
    }

    public void generateKnownBugsReport() {
        AggregateReportParameters parameters = new AggregateReportParameters();
        parameters.setTestCaseRunId(tcrId);
        try {
            List<KnownBugRow> knownBugRows = BeanUtil.getSfContext().getStatisticsService().getReportingStorage().generateKnownBugsReport(parameters);
            this.lastKnownBugsResult = StatisticsUtils.groupKnownBugsByCategory(knownBugRows);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            BeanUtil.addErrorMessage(e.getMessage(), "");
        }
    }

	@PostConstruct
    public void created() {
        logger.debug("TestCaseDetailsBean [{}] constructed", hashCode());
    }

    @PreDestroy
    public void destroy() {
        logger.debug("TestCaseDetailsBean [{}] destroy", hashCode());
    }

	public long getTcrId() {
		return tcrId;
	}

	public void setTcrId(long tcrId) {
		this.tcrId = tcrId;
	}

	public List<ActionInfoRow> getLastFailedResult() {
		return lastFailedResult;
	}

	public void setLastFailedResult(List<ActionInfoRow> lastResult) {
		this.lastFailedResult = lastResult;
	}

    public List<ActionInfoRow> getLastTaggedResult() {
        return lastTaggedResult;
    }

    public void setLastTaggedResult(List<ActionInfoRow> lastTaggedResult) {
        this.lastTaggedResult = lastTaggedResult;
    }

    public List<KnownBugCategoryRow> getLastKnownBugsResult() {
        return lastKnownBugsResult;
    }

    public void setLastKnownBugsResult(List<KnownBugCategoryRow> lastKnownBugsResult) {
        this.lastKnownBugsResult = lastKnownBugsResult;
    }

    public String getKnownBugsHeader() {
        return knownBugsHeader;
    }

    public void setKnownBugsHeader(String knownBugsHeader) {
        this.knownBugsHeader = knownBugsHeader;
    }
}
