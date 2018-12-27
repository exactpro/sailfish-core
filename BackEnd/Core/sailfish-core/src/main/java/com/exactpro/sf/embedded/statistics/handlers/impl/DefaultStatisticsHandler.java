/*******************************************************************************
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

package com.exactpro.sf.embedded.statistics.handlers.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import com.exactpro.sf.embedded.statistics.StatisticsUtils;
import com.exactpro.sf.embedded.statistics.handlers.ReportRow;
import com.exactpro.sf.embedded.statistics.storage.AggregatedReportRow;
import com.exactpro.sf.embedded.statistics.storage.IAdditionalStatisticsLoader;

import static com.exactpro.sf.embedded.statistics.handlers.ReportRow.createMatrixRow;
import static com.exactpro.sf.embedded.statistics.handlers.ReportRow.createTestCaseRow;
import static com.exactpro.sf.embedded.statistics.handlers.ReportValue.createRunStatusValue;
import static com.exactpro.sf.embedded.statistics.handlers.ReportValue.createSimpleValue;
import static com.exactpro.sf.embedded.statistics.handlers.ReportValue.createTestCaseIdValue;

public class DefaultStatisticsHandler extends AbstractStatisticsReportHandler {

    private static final String ID_COLUMN = "id";
    private static final String NAME_COLUMN = "Name";
    private static final String DESCRIPTION_COLUMN = "Description";
    private static final String FAILURE_REASON_COLUMN = "Failure Reason";
    private static final String START_TIME_COLUMN = "Start Time";
    private static final String FINISH_TIME_COLUMN = "Finish Time";
    private static final String USER_COLUMN = "User Name";
    private static final String SF_COLUMN = "SF";
    private static final String ENVIRONMENT_COLUMN = "Environment";
    private static final String SERVICES_USED_COLUMN = "Services Used";
    private static final String STATUS_COLUMN = "Status";
    private static final String HASH_COLUMN = "Hash";
    private static final String FAILED_ACTIONS_COLUMN = "Failed Actions";

    private final List<ReportRow> rows = new ArrayList<>();

    public DefaultStatisticsHandler() {
        super(generateHeader(), "Default");
    }

    private static List<String> generateHeader() {
        return Lists.newArrayList(ID_COLUMN, NAME_COLUMN, DESCRIPTION_COLUMN,
                STATUS_COLUMN, FAILURE_REASON_COLUMN, FAILED_ACTIONS_COLUMN, START_TIME_COLUMN, FINISH_TIME_COLUMN,
                USER_COLUMN, SF_COLUMN, ENVIRONMENT_COLUMN, SERVICES_USED_COLUMN, HASH_COLUMN);
    }

    @Override
    public List<ReportRow> getReportRows() {
        return Collections.unmodifiableList(rows);
    }

    @Override
    public void reset() {
        rows.clear();
    }

    @Override
    public void handleMatrixRunTestCases(List<AggregatedReportRow> testcaseList, IAdditionalStatisticsLoader loader) {
        StatisticsUtils.extractMatrixInfoRows(testcaseList);
        for (AggregatedReportRow row : testcaseList) {
            if (row.isMatrixRow()) {
                processMatrixRecord(row);
            } else {
                processTestCaseRecord(row);
            }
        }
    }

    private void processMatrixRecord(AggregatedReportRow row) {
        ReportRow rowMap = createMatrixRow(getHeaderColumns());

        rowMap.put(ID_COLUMN, createSimpleValue(row.getSfId()));
        rowMap.put(NAME_COLUMN, createSimpleValue(row.getMatrixName()));
        rowMap.put(DESCRIPTION_COLUMN, createSimpleValue(row.getDescription()));
        rowMap.put(FAILURE_REASON_COLUMN, createSimpleValue(row.getFailReason()));
        rowMap.put(START_TIME_COLUMN, createSimpleValue(row.getStartTime().toLocalTime()));
        rowMap.put(FINISH_TIME_COLUMN, createSimpleValue(row.getFinishTime().toLocalTime()));
        rowMap.put(USER_COLUMN, createSimpleValue(row.getUserName()));
        rowMap.put(SF_COLUMN, createSimpleValue(String.format("%s:%s%s", row.getHost(), row.getPort(), row.getSfName())));
        rowMap.put(ENVIRONMENT_COLUMN, createSimpleValue(row.getEnvironmentName()));
        rowMap.put(SERVICES_USED_COLUMN, createSimpleValue(row.getServicesUsed()));
        rows.add(rowMap);
    }

    private void processTestCaseRecord(AggregatedReportRow row) {
        ReportRow rowMap = createTestCaseRow(getHeaderColumns());

        rowMap.put(ID_COLUMN, createTestCaseIdValue(row.getTestCaseId()));
        rowMap.put(NAME_COLUMN, createSimpleValue(row.getTestCaseName()));
        rowMap.put(DESCRIPTION_COLUMN, createSimpleValue(row.getDescription()));
        rowMap.put(STATUS_COLUMN, createRunStatusValue(row.getStatus()));
        rowMap.put(FAILURE_REASON_COLUMN, createSimpleValue(row.getFailReason()));
        rowMap.put(FAILED_ACTIONS_COLUMN, createSimpleValue(row.getFailedActions()));
        rowMap.put(START_TIME_COLUMN, createSimpleValue(row.getStartTime().toLocalTime()));
        rowMap.put(FINISH_TIME_COLUMN, createSimpleValue(row.getFinishTime().toLocalTime()));
        rowMap.put(HASH_COLUMN, createSimpleValue(row.getHash()));
        rows.add(rowMap);
    }
}
