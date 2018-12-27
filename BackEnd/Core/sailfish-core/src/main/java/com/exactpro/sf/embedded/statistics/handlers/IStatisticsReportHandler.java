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

package com.exactpro.sf.embedded.statistics.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.exactpro.sf.embedded.statistics.storage.AggregatedReportRow;
import com.exactpro.sf.embedded.statistics.storage.IAdditionalStatisticsLoader;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;

public interface IStatisticsReportHandler {

    List<String> getHeaderColumns();

    List<ReportRow> getReportRows();

    /**
     * Generates report name. Required parameters for {@link AggregateReportParameters}:<br/>
     * {@link AggregateReportParameters#getFrom()} start date of loaded data<br/>
     * {@link AggregateReportParameters#getTo()} end date of loaded data
     * @param parameters
     * @return
     */
    String getReportName(AggregateReportParameters parameters);

    void reset();

    void handleMatrixRunTestCases(List<AggregatedReportRow> testcaseList, IAdditionalStatisticsLoader loader);

    void finalize(IAdditionalStatisticsLoader loader);

    void writeReport(OutputStream outputStream) throws IOException;
}
