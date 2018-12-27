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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.csvreader.CsvWriter;
import org.apache.commons.lang3.StringUtils;
import java.time.format.DateTimeFormatter;

import com.exactpro.sf.embedded.statistics.handlers.IStatisticsReportHandler;
import com.exactpro.sf.embedded.statistics.handlers.ReportRow;
import com.exactpro.sf.embedded.statistics.handlers.ReportValue;
import com.exactpro.sf.embedded.statistics.storage.IAdditionalStatisticsLoader;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.util.DateTimeUtility;

public abstract class AbstractStatisticsReportHandler implements IStatisticsReportHandler {

    private static final DateTimeFormatter formatter = DateTimeUtility.createFormatter("dd_MM_yyyy_HH_mm_ss");
    private static final String AGGREGATED_REPORT_NAME_PART = "_aggregated_report__";
    private static final String CSV_FILE_EXTENSION = ".csv";
    private final List<String> HEADER;
    private final String PREFIX;

    public AbstractStatisticsReportHandler(List<String> header, String prefix) {
        this.HEADER = Collections.unmodifiableList(header);
        this.PREFIX = prefix;
    }

    @Override
    public List<String> getHeaderColumns() {
        return this.HEADER;
    }

    @Override
    public String getReportName(AggregateReportParameters parameters) {
        Objects.requireNonNull(parameters, "'AggregateReportParameters' parameter");
        Objects.requireNonNull(parameters.getFrom(), "'From' parameter");
        Objects.requireNonNull(parameters.getTo(), "'To' parameter");
        StringBuilder reportName = new StringBuilder(this.PREFIX)
                .append(AGGREGATED_REPORT_NAME_PART)
                .append(formatter.format(parameters.getFrom()))
                .append("__")
                .append(formatter.format(parameters.getTo()))
                .append(CSV_FILE_EXTENSION);
        return reportName.toString();
    }

    @Override
    public void finalize(IAdditionalStatisticsLoader loader) {
        // do nothing
    }

    @Override
    public void writeReport(OutputStream outputStream) throws IOException {
        CsvWriter writer = new CsvWriter(outputStream, ',', Charset.defaultCharset());
        try {
            List<String> header = getHeaderColumns();
            writer.writeRecord(header.toArray(new String[0]));
            for (ReportRow row : getReportRows()) {
                for (String columnName : header) {
                    ReportValue cellValue = row.get(columnName);
                    String toWrite = cellValue != null ? cellValue.toString() : StringUtils.EMPTY;
                    writer.write(toWrite);
                }
                writer.endRecord();
            }
        } finally {
            writer.close();
        }
    }
}
