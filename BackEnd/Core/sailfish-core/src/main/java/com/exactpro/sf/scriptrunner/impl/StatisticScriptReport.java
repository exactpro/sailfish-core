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
package com.exactpro.sf.scriptrunner.impl;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.scriptrunner.ReportUtils;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.StatusDescription;

public class StatisticScriptReport extends DefaultScriptReport {

	private static final Logger logger = LoggerFactory.getLogger(StatisticScriptReport.class);

	private final StatisticsService statisticsService;

	private final List<Tag> tags;

	private final String reportFolder;

	private String matrixName;

	private long actionCouter = 1;

	public StatisticScriptReport(StatisticsService statisticsService, String reportFilePath, List<Tag> tags) {
		this.statisticsService = statisticsService;
		this.tags = tags;
		this.reportFolder = reportFilePath; //new File(reportFilePath).getParentFile().getName();
	}

	@Override
	public void createReport(ScriptContext context, String name, String description, long scriptRunId, String environmentName, String userName) {

		logger.debug("Create report {}, {}, {}, {}", name, description, scriptRunId, environmentName);

		this.matrixName = name;

		//this.scriptRunId = scriptRunId;

		//this.environmentName = environmentName;

		if(this.statisticsService != null) {

			this.statisticsService.matrixStarted(matrixName, reportFolder, scriptRunId, environmentName, userName, tags,
                    context.getScriptDescriptionId());

		}

	}

	@Override
	public void createException(Throwable cause) {
        if(this.statisticsService != null && actionCouter == 1) {
            statisticsService.matrixEception(matrixName, cause);
        }
	}

	@Override
	public void closeReport() {
		if(this.statisticsService != null) {
			statisticsService.matrixFinished(matrixName);
		}
	}

	@Override
    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {
        String name = ReportUtils.generateTestCaseName(reference, matrixOrder, type);
        logger.debug("Create test case {}", name);

		if(this.statisticsService != null) {
            this.statisticsService.testCaseStarted(this.matrixName, tcId, name.replaceAll("\\W", "_"), description, order, tcHash);

		}

	}

	@Override
	public void closeTestCase(StatusDescription status) {

		if(this.statisticsService != null) {

			String failReason = null;

			if(status.getCause() != null) {

				failReason = extractFailReason(status.getCause());

			}

			this.statisticsService.testCaseFinished(this.matrixName, status.getStatus(), failReason, status.getKnownBugs());

		}

		actionCouter = 1;

	}

	@Override
    public void createAction(String name, String serviceName, String action, String msg, String description, Object inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {

		if(this.statisticsService != null) {

            this.statisticsService.actionStarted(matrixName, serviceName, action, msg, description, actionCouter++, tag,
                                                 hash);

		}

	}

	@Override
    public void createAction(String name, String serviceName, String action, String msg, String description, List<Object> inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {

		if(this.statisticsService != null) {

            this.statisticsService.actionStarted(matrixName, serviceName, action, msg, description, actionCouter++, tag,
                                                 hash);

		}

	}

	@Override
    public void closeAction(StatusDescription status, Object actionResult) {

		if(this.statisticsService != null) {

			String failReason = null;

			if(status.getCause() != null) {

				failReason = extractFailReason(status.getCause());

			}

			this.statisticsService.actionFinished(matrixName, status.getStatus(), failReason);

		}

	}

    @Override
    public void createVerification(String name, String description, StatusDescription status, ComparisonResult result) {
        if (this.statisticsService != null) {
            this.statisticsService.actionVerification(matrixName, result);
        }
    }

    private String extractFailReason(Throwable t) {
        Throwable root = ExceptionUtils.getRootCause(t);
        t = ObjectUtils.defaultIfNull(root, t);
        return t != null ? t.getMessage() : null;
    }
}
