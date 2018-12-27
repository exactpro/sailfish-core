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
package com.exactpro.sf.embedded.statistics;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.csvreader.CsvWriter;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.embedded.statistics.storage.AggregatedReportRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.ActionInfoRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.KnownBugCategoryRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.KnownBugRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupDimension;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportResult;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportRow;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.util.BugCategoriesComparator;
import com.exactpro.sf.util.BugDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class StatisticsUtils {
    private static final ThreadLocal<SimpleDateFormat> creationTimestampFormat = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("ddMMyyyy-HH:mm:ss"));

    private static final String KNOWN_BUGS_COLUMN = "Known Bugs";
    public static final String[] availableScriptRunHistoryColumns = { "id", "Name", "Description", "Status",
            "Failure Reason", "Failed Actions", "Start Time", "Finish Time", "Execution Time", "User Name", "SF",
            "Environment", "Services Used", "User Status", "Comment", "Fix Revision", KNOWN_BUGS_COLUMN, "Hash", "Tagged Actions" };

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectWriter LIST_JSON_WRITER = OBJECT_MAPPER.writer();
    private static final ObjectReader LIST_JSON_READER = OBJECT_MAPPER.reader().forType(List.class);
    private static final BugCategoriesComparator CATEGORIES_COMPARATOR = new BugCategoriesComparator();

    public static TagGroupReportParameters createTagGroupReportParameters(List<TagGroupDimension> sourceDimensions) {
        TagGroupReportParameters params = new TagGroupReportParameters();

        Set<Long> tags = new HashSet<>();
        List<TagGroupDimension> selectedDimensions = new ArrayList<>();

        for (TagGroupDimension tagGroupDimension : sourceDimensions) {
            if (tagGroupDimension.isTag()) {
                selectedDimensions.add(tagGroupDimension);
                if (!tags.add(tagGroupDimension.getId())) {
                    throw new StatisticsException("Error", "Tag '" + tagGroupDimension.getName() + "' previously indicated");
                }
            } else {
                List<TagGroupDimension> listSubTags = tagGroupDimension.getSelectedSubTags();
                if (!listSubTags.isEmpty()) {
                    selectedDimensions.add(tagGroupDimension);
                    for (TagGroupDimension tag : listSubTags) {
                        if (!tags.add(tag.getId())) {
                            throw new StatisticsException("Error",
                                    "Tag '" + tag.getName() + "' of group '" + tagGroupDimension.getName() + "'  previously indicated");
                        }
                    }
                }
            }
        }

        if(selectedDimensions.isEmpty()) {
            throw new StatisticsException("No tags selected", "");
        }

        params.setTags(tags);
        params.setDimensions(selectedDimensions);
        params.setNumberOfGroups(selectedDimensions.size());

        return params;
    }

    public static List<TagGroupReportResult> generateTagGroupReportResults(StatisticsService statisticsService,
                                                                           List<TagGroupDimension> dimensions) {
        TagGroupReportParameters params = createTagGroupReportParameters(dimensions);
        List<TagGroupReportResult> results = new ArrayList<>();
        for(int i =0; i < dimensions.size(); i++) {
            params.setLoadForLevel(i);
            results.add(statisticsService.getReportingStorage().generateTagGroupReport(params));
        }
        return results;
    }

    public static void writeTagGroupReportToCsv(OutputStream outputStream, List<TagGroupReportResult> results) throws IOException {
        String[] header = new String[] {
                "Tag",
                "Total Execution Time",
                "Total Test Cases",
                "Passed Test Cases",
                "Failed Test Cases",
                "Conditionally Passed Test Cases",
                "Passed %",
                "Conditionally Passed %",
                "Failed %",
                "Total Matrices",
                "Failed Matrices",
        };

        CsvWriter writer = null;
        try {
            writer = new CsvWriter(outputStream, ',', Charset.defaultCharset());
            writer.writeRecord(header);
            writeTagGroupReportToCsv(results, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static String createStatsPerTagsName() {
        return createCsvFileName("sf_stats_per_tags");
    }

    public static String createScriptRunsHistoryName() {
        return createCsvFileName("sf_script_runs_history");
    }

    private static String createCsvFileName(String name) {

        return name + creationTimestampFormat.get().format(new Date()) + ".csv";
    }

    private static void writeTagGroupReportToCsv(List<TagGroupReportResult> results, CsvWriter writer) throws IOException {
        if(results == null || results.isEmpty()) {
            return;
        }
        DecimalFormat format = new DecimalFormat("###.00");
        writeChildNotes(null, format, results, 0, writer);
    }

    private static void writeChildNotes(String parentName, DecimalFormat format, List<TagGroupReportResult> results,
                                        int curLevelIndex, CsvWriter writer) throws IOException {
        List<TagGroupReportRow> rowsCurrentLev = results.get(curLevelIndex).getRows();
        for (TagGroupReportRow row : rowsCurrentLev) {
            if (parentName == null || row.getDimensionsPath()[curLevelIndex - 1].equals(parentName)) {
                writeTagGroupReportRow(row, format, writer);
                if(curLevelIndex < results.size() -1) {
                    writeChildNotes(row.getDimensionsPath()[curLevelIndex], format, results, curLevelIndex + 1, writer);
                }
            }
        }
    }

    private static void writeTagGroupReportRow(TagGroupReportRow row,DecimalFormat format, CsvWriter writer) throws IOException {
        writer.write(Arrays.toString(row.getDimensionsPath()));
        //writer.write(Long.toString(row.getTotalExecTime()));
        writer.write(row.getFormattedExecTime());
        writer.write(Long.toString(row.getTotalTcCount()));
        writer.write(Long.toString(row.getPassedCount()));
        writer.write(Long.toString(row.getFailedCount()));
        writer.write(Long.toString(row.getConditionallyPassedCount()));
        writer.write(format.format(row.getPassedPercent()));
        writer.write(format.format(row.getConditionallyPassedPercent()));
        writer.write(format.format(row.getFailedPercent()));
        writer.write(format.format(row.getTotalMatrices()));
        writer.write(format.format(row.getFailedMatrices()));
        writer.endRecord();
    }

    public static void writeScriptRunsHistory(ISFContext context, OutputStream out, List<String> columns,
                                              List<AggregatedReportRow> reportRows, boolean exportWithTCsInfo,
                                              boolean exportWithActionsInfo) throws IOException {
        writeScriptRunsHistory(context, out, columns, reportRows, exportWithTCsInfo, exportWithActionsInfo, null);
    }

    public static void writeScriptRunsHistory(ISFContext context, OutputStream out, List<String> columns,
                                              List<AggregatedReportRow> reportRows, boolean exportWithTCsInfo,
                                              boolean exportWithActionsInfo, MatrixInfo info) throws IOException {

        CsvWriter writer = null;

        try {
            writer = new CsvWriter(out, ',', Charset.defaultCharset());

            if (info == null) {
                info = MatrixInfo.extractMatrixInfo(reportRows);
            }

            Map<Long, List<ActionInfoRow>> taggedActions = new HashMap<>();

            if (exportWithTCsInfo && columns.contains("Tagged Actions")) {
                columns.remove("Tagged Actions");
                Set<String> tags = new TreeSet<>();

                for (AggregatedReportRow reportRow : reportRows) {
                    if (reportRow.isMatrixRow()) {
                        continue;
                    }

                    Long testCaseRunId = reportRow.getTestCaseRunId();
                    AggregateReportParameters params = new AggregateReportParameters();
                    params.setTestCaseRunId(testCaseRunId);
                    List<ActionInfoRow> rows = context.getStatisticsService()
                                                      .getReportingStorage()
                                                      .generateTaggedActionsInfo(params);

                    for (ActionInfoRow infoRow : rows) {
                        String tag = infoRow.getTag();

                        if (tag != null) {
                            tags.add(tag);
                        }
                    }

                    taggedActions.put(testCaseRunId, rows);
                }

                for (String tag : tags) {
                    columns.add(tag + " Tagged Actions");
                }

            }

            if (exportWithTCsInfo && columns.contains(KNOWN_BUGS_COLUMN)) {
                int prevIndex = columns.indexOf(KNOWN_BUGS_COLUMN);
                columns.add(prevIndex, "Non-reproduced Known Bugs");
                columns.add(prevIndex, "Reproduced Known Bugs");
                columns.remove(KNOWN_BUGS_COLUMN);
                Map<Long, AggregatedReportRow> testCasesToLoad = new HashMap<>();
                for (AggregatedReportRow row : reportRows) {
                    if (row.isMatrixRow() || row.isActionRow()) {
                        continue;
                    }
                    if (row.getReproducedKnownBugsCount() + row.getNonReproducedKnownBugsCount() > 0) {
                        testCasesToLoad.put(row.getTestCaseRunId(), row);
                    }
                }
                AggregateReportParameters parameters = new AggregateReportParameters();
                parameters.setTestCaseRunIds(new ArrayList<>(testCasesToLoad.keySet()));
                Map<Long, List<KnownBugRow>> testCasesKnownBugs = context.getStatisticsService().getReportingStorage().generateTestCasesKnownBugsReports(parameters);
                for (Map.Entry<Long, List<KnownBugRow>> entry : testCasesKnownBugs.entrySet()) {
                    AggregatedReportRow row = testCasesToLoad.get(entry.getKey());
                    row.setCategorisedKnownBugs(groupKnownBugsByCategory(entry.getValue()));
                }


            }
            List<AggregatedReportRow> rowsToWrite = reportRows;

            if (exportWithActionsInfo) {
                int index = 0;

                if (columns.contains("Name")) {
                    index = columns.indexOf("Name");
                } else if (columns.contains("id")) {
                    index = columns.indexOf("id");
                }

                columns.add(index + 1, "Message Type");

                rowsToWrite = new ArrayList<>();

                for (AggregatedReportRow row : reportRows) {
                    if (row.isMatrixRow()) {
                        rowsToWrite.add(row);
                        continue;
                    } else if (exportWithTCsInfo) {
                        rowsToWrite.add(row);
                    }

                    AggregateReportParameters params = new AggregateReportParameters();
                    params.setTestCaseRunId(new Long(row.getTestCaseRunId()));

                    rowsToWrite.addAll(context.getStatisticsService()
                                              .getReportingStorage()
                                              .generateActionsAggregatedReportRow(params));
                }
            }

            if (columns.contains("Status")) {
                int index = columns.indexOf("Status");
                columns.add(index + 1, "Failed");
                columns.add(index + 1, "CondPassed");
                columns.add(index + 1, "Passed");
            }

            String header[] = columns.toArray(new String[columns.size()]);

            writeRowsToCsv(writer, header, taggedActions, rowsToWrite, exportWithTCsInfo, exportWithActionsInfo, info);

        } finally {
            writer.close();
        }

    }

    private static void writeRowsToCsv(CsvWriter writer, String[] headers, Map<Long, List<ActionInfoRow>> taggedActions,
                                       List<AggregatedReportRow> aggregatedRows, boolean exportWithTCsInfo,
                                       boolean exportWithActionsInfo, MatrixInfo info) throws IOException {

        writer.writeRecord(headers);

        for (AggregatedReportRow row : aggregatedRows) {

            boolean matrixRow = row.isMatrixRow();
            boolean actionRow = row.isActionRow();

            if (!matrixRow && !exportWithTCsInfo && !exportWithActionsInfo) {
                continue;
            }

            for (int i = 0; i < headers.length; i++) {
                String toWrite;
                switch (headers[i]) {
                case "id":
                    toWrite = matrixRow ? Long.toString(row.getSfId()) : row.getTestCaseId();
                    break;
                case "Name":
                    toWrite = matrixRow ? row.getMatrixName() : actionRow ? row.getActionName() : row.getTestCaseName();
                    break;
                case "Status":
                    toWrite = matrixRow
                              ? extractMatrixStatus(row)
                              : actionRow ? row.getStatus().name() : row.getStatus().toString().replace('_', ' ');
                    break;
                case "Failed":
                    toWrite = matrixRow ? Long.toString(row.getFailedCount()) : "";
                    break;
                case "Passed":
                    toWrite = matrixRow ? Long.toString(row.getPassedCount()) : "";
                    break;
                case "CondPassed":
                    toWrite = matrixRow ? Long.toString(row.getConditionallyPassedCount()) : "";
                    break;
                case "Failure Reason":
                    toWrite = matrixRow ? row.getMatrixFailReason() : row.getFailReason();
                    break;
                case "Start Time":
                    toWrite = matrixRow
                              ? row.getMatrixStartTime().toString()
                              : actionRow ? "" : row.getStartTime().toString();
                    break;
                case "Finish Time":
                    toWrite = matrixRow
                              ? row.getMatrixFinishTime().toString()
                              : actionRow ? "" : row.getFinishTime().toString();
                    break;
                case "Execution Time":
                    toWrite = actionRow ? "" : DurationFormatUtils.formatDuration(row.getExecutionTime(), "mm:ss");
                    break;
                case "SF":
                    toWrite = matrixRow ? String.format("%s:%s%s", row.getHost(), row.getPort(), row.getSfName()) : "";
                    break;
                case "User Status":
                    toWrite = row.getUserComments().getStatus() != null
                              ? row.getUserComments().getStatus().getName()
                              : "";
                    break;
                case "Comment":
                    toWrite = row.getUserComments().getComment();
                    break;
                case "Fix Revision":
                    toWrite = row.getUserComments().getFixedVersion();
                    break;
                case "Hash":
                    toWrite = matrixRow ? "" : Integer.toString(row.getHash());
                    break;
                case "Message Type":
                    toWrite = actionRow ? row.getMessageType() : "";
                    break;
                case "Reproduced Known Bugs":
                    toWrite = matrixRow || actionRow ? "" :
                            createCategorisedKnownBugsCell(row.getCategorisedKnownBugs(), true);
                    break;
                case "Non-reproduced Known Bugs":
                    toWrite = matrixRow || actionRow ? "" :
                            createCategorisedKnownBugsCell(row.getCategorisedKnownBugs(), false);
                    break;
                default:
                    if (headers[i].endsWith("Tagged Actions")) {
                        if (matrixRow) {
                            toWrite = "";
                        } else {
                            String tag = headers[i].split("\\s", 3)[0];
                            List<ActionInfoRow> infoRows = taggedActions.get(row.getTestCaseRunId());

                            if (infoRows != null) {
                                toWrite = getActionDescriptionsByTag(infoRows, tag);
                            } else {
                                toWrite = "";
                            }
                        }
                    } else if (headers[i].equals("Message Type")) {
                        toWrite = "";
                    } else {
                        toWrite = row.get(headers[i], "");
                    }
                }

                writer.write(toWrite);
            }

            writer.endRecord();
        }

        boolean totalPrinted = false;

        if (!headers[0].equals("Execution Time")) {
            headers[0] = "TOTAL";
            totalPrinted = true;
        }

        for (int j = 0; j < headers.length; j++) {
            switch (headers[j]) {
            case "TOTAL":
                // Do nothing
                break;
            case "Passed":
                headers[j] = Long.toString(info.getAllCasesPassed());
                break;
            case "Failed":
                headers[j] = Long.toString(info.getAllCasesFailed());
                break;
            case "CondPassed":
                headers[j] = Long.toString(info.getAllCasesConditionallyPassed());
                break;
            case "Execution Time":
                headers[j] = totalPrinted
                             ? getLastResultExecutionTime(aggregatedRows)
                             : "TOTAL: " + getLastResultExecutionTime(aggregatedRows);
                break;
            default:
                headers[j] = "";
            }
        }

        writer.endRecord();
        writer.endRecord();
        writer.writeRecord(headers);
    }

    public static String createCategorisedKnownBugsCell(List<KnownBugCategoryRow> categorisedKnownBugs, boolean reproduced) {
        if (categorisedKnownBugs == null) {
            return StringUtils.EMPTY;
        }
        StringBuilder cellText = new StringBuilder();
        String categoryDelimiter = ": ";
        for (KnownBugCategoryRow entry : categorisedKnownBugs) {
            if ((entry.getReproducedBugs().isEmpty() && reproduced)
                    || (entry.getNonReproducedBugs().isEmpty() && !reproduced)) {
                continue;
            }
            if (StringUtils.isBlank(entry.getCategoryString())) {
                cellText.append("No category");
            } else {
                cellText.append(entry.getCategoryString());
            }
            cellText.append(categoryDelimiter);
            if (reproduced) {
                cellText.append(entry.getReproducedBugsString());
            } else {
                cellText.append(entry.getNonReproducedBugsString());
            }
            cellText.append("\n");
        }
        return cellText.toString().trim();
    }

    public static String getLastResultExecutionTime(List<AggregatedReportRow> reportRows) {
        long spentTime = 0;

        for (AggregatedReportRow row : reportRows) {
            if (row.isMatrixRow()) {
                spentTime += row.getExecutionTime();
            }
        }

        return DurationFormatUtils.formatDuration(spentTime, "HH:mm:ss");
    }

    private static String extractMatrixStatus(AggregatedReportRow row) {
        if (!row.isMatrixRow()) {
            throw new UnsupportedOperationException("Status can be extracted only from matrix");
        }

        if (row.getMatrixFailReason() != null || row.getFailedCount() > 0) {
            return StatusType.FAILED.name();
        } else if (row.getConditionallyPassedCount() > 0) {
            return StatusType.CONDITIONALLY_PASSED.name();
        } else {
            return StatusType.PASSED.name();
        }

    }

    private static String getActionDescriptionsByTag(List<ActionInfoRow> infoRows, String tag) {
        return infoRows.stream()
                       .filter(infoRow -> tag.equals(infoRow.getTag()))
                       .map(ActionInfoRow::getDescription)
                       .collect(Collectors.joining(System.lineSeparator()));
    }

    public static String buildKnownBugJson(String subject, List<String> categories) {
        Objects.requireNonNull(subject, "'KnownBug subject' parameter");
        Objects.requireNonNull(categories, "'KnownBug categories' parameter");
        List<String> accumulateList = new ArrayList<>(categories);
        accumulateList.add(subject);
        try (Writer listToJsonWriter = new StringWriter()) {
            LIST_JSON_WRITER.writeValue(listToJsonWriter, accumulateList);
            return listToJsonWriter.toString();
        } catch (IOException ex) {
            throw new EPSCommonException("Can't generate KnownBug id", ex);
        }
    }

    public static BugDescription restoreKnownBugFromJson(String jsonArray) {
        Objects.requireNonNull(jsonArray, "'KnownBug JSON' parameter");
        try {
            List<String> accumulateList = LIST_JSON_READER.readValue(jsonArray);
            if (accumulateList.isEmpty()) {
                throw new EPSCommonException(String.format("Wrong format %s. Json array is empty", jsonArray));
            }
            int lastElement = accumulateList.size() - 1;
            String subject = accumulateList.get(lastElement);
            String[] categories = accumulateList.subList(0, lastElement).toArray(new String[0]);
            return new BugDescription(subject, categories);
        } catch (IOException e) {
            throw new EPSCommonException(String.format("Can't restore KnownBug from %s", jsonArray), e);
        }
    }

    public static List<KnownBugCategoryRow> groupKnownBugsByCategory(List<KnownBugRow> knownBugRows) {
        TreeMap<String[], KnownBugCategoryRow> bugs = new TreeMap<>(CATEGORIES_COMPARATOR);
        for (KnownBugRow knownBugRow : knownBugRows) {
            String[] categories = knownBugRow.getCategories();
            KnownBugCategoryRow bugCategoryRow = bugs.computeIfAbsent(categories, KnownBugCategoryRow::new);

            if (BooleanUtils.isTrue(knownBugRow.getReproduced())) {
                bugCategoryRow.getReproducedBugs().add(knownBugRow.getSubject());
            } else {
                bugCategoryRow.getNonReproducedBugs().add(knownBugRow.getSubject());
            }
        }
        return new ArrayList<>(bugs.values());
    }

    public static void extractMatrixInfoRows(List<AggregatedReportRow> rows) {

        long matrixRunId = -999L;

        for(int i =0; i < rows.size(); i++) {

            AggregatedReportRow row = rows.get(i);

            if(row.getMatrixRunId() != matrixRunId) {

                AggregatedReportRow matrixInfoRow = new AggregatedReportRow();

                matrixInfoRow.setSfId(row.getSfId());
                matrixInfoRow.setStartTime(row.getMatrixStartTime());
                matrixInfoRow.setFinishTime(row.getMatrixFinishTime());
                matrixInfoRow.setMatrixName(row.getMatrixName());
                matrixInfoRow.setUserName(row.getUserName());
                matrixInfoRow.setHost(row.getHost());
                matrixInfoRow.setPort(row.getPort());
                matrixInfoRow.setSfName(row.getSfName());
                matrixInfoRow.setServicesUsed(row.getServicesUsed());
                matrixInfoRow.setEnvironmentName(row.getEnvironmentName());

                matrixInfoRow.setMatrixRow(true);

                rows.add(i, matrixInfoRow);
                matrixRunId = row.getMatrixRunId();
                i++;
            }
        }
    }
}
