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

package com.exactpro.sf.embedded.statistics.storage;

import java.util.List;
import java.util.Map;

import com.exactpro.sf.embedded.statistics.storage.reporting.ActionInfoRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.KnownBugRow;

public interface IAdditionalStatisticsLoader {

    /**
     * Loads list {@link ActionInfoRow} of failed actions for specified test case run ID.
     * Required parameter {@link AggregateReportParameters#setTestCaseRunId(Long)} must be set
     * @param params
     * @return
     */
    List<ActionInfoRow> generateFailedActionsInfo(AggregateReportParameters params);

    /**
     * Loads list {@link ActionInfoRow} of failed actions for specified test case run IDs.
     * Required parameter {@link AggregateReportParameters#setTestCaseRunIds(List)} must be set
     * @param params
     * @return
     */
    Map<Long, List<ActionInfoRow>> generateTestCasesFailedActionsInfo(AggregateReportParameters params);

    /**
     * Loads list of {@link KnownBugRow} for specified test case run ID.
     * Required parameter {@link AggregateReportParameters#setTestCaseRunId(Long)} must be set
     * @param params
     * @return
     */
    List<KnownBugRow> generateKnownBugsReport(AggregateReportParameters params);

    /**
     * Loads list of {@link KnownBugRow} for specified test case run IDs.
     * Required parameter {@link AggregateReportParameters#setTestCaseRunIds(List)} must be set
     * @param params
     * @return
     */
    Map<Long, List<KnownBugRow>> generateTestCasesKnownBugsReports(AggregateReportParameters params);
}
