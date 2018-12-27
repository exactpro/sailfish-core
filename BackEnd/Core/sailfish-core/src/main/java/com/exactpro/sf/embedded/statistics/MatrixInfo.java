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

package com.exactpro.sf.embedded.statistics;

import com.exactpro.sf.embedded.statistics.storage.AggregatedReportRow;
import com.exactpro.sf.scriptrunner.StatusType;

import java.util.List;

public class MatrixInfo {
    private final long allMatricesPassed;
    private final long allMatricesFailed;
    private final long allMatricesConditionallyPassed;
    private final long allCasesPassed;
    private final long allCasesFailed;
    private final long allCasesConditionallyPassed;

    public MatrixInfo(long allMatricesPassed, long allMatricesFailed, long allMatricesConditionallyPassed,
                      long allCasesPassed, long allCasesFailed, long allCasesConditionallyPassed) {
        this.allMatricesPassed = allMatricesPassed;
        this.allMatricesFailed = allMatricesFailed;
        this.allMatricesConditionallyPassed = allMatricesConditionallyPassed;
        this.allCasesPassed = allCasesPassed;
        this.allCasesFailed = allCasesFailed;
        this.allCasesConditionallyPassed = allCasesConditionallyPassed;
    }

    public long getAllMatricesPassed() {
        return allMatricesPassed;
    }

    public long getAllMatricesFailed() {
        return allMatricesFailed;
    }

    public long getAllMatricesConditionallyPassed() {
        return allMatricesConditionallyPassed;
    }

    public long getAllMatrices() {
        return allMatricesPassed + allMatricesFailed + allMatricesConditionallyPassed;
    }

    public long getAllCasesPassed() {
        return allCasesPassed;
    }

    public long getAllCasesFailed() {
        return allCasesFailed;
    }

    public long getAllCasesConditionallyPassed() {
        return allCasesConditionallyPassed;
    }

    public long getAllCases() {
        return allCasesFailed + allCasesPassed + allCasesConditionallyPassed;
    }

    public static MatrixInfo extractMatrixInfo(List<AggregatedReportRow> rows) {

        long lastMatrixRunId = -997l;

        AggregatedReportRow lastInfoRow = null;

        long allMatricesPassed = 0l;
        long allMatricesFailed = 0l;
        long allMatricesConditionallyPassed = 0l;

        long allCasesPassed = 0l;
        long allCasesFailed = 0l;
        long allCasesConditionallyPassed = 0l;

        long numPassed = 0l;
        long numFailed = 0l;
        long numConditionallyPassed = 0l;

        for (int i = 0; i < rows.size(); i++) {

            AggregatedReportRow row = rows.get(i);

            if (row.getMatrixRunId() != lastMatrixRunId) {

                lastMatrixRunId = row.getMatrixRunId();

                if (lastInfoRow != null) {

                    lastInfoRow.setPassedCount(numPassed);
                    lastInfoRow.setFailedCount(numFailed);
                    lastInfoRow.setConditionallyPassedCount(numConditionallyPassed);

                    if (numFailed > 0) {
                        allMatricesFailed++;
                    } else if (numConditionallyPassed > 0) {
                        allMatricesConditionallyPassed++;
                    } else {
                        allMatricesPassed++;
                    }
                }

                numPassed = 0l;
                numFailed = 0l;
                numConditionallyPassed = 0l;

                if (isMatrixFailedRow(row)) {
                    row.setReportFile("report");
                    row.setMatrixRow(true);

                    lastInfoRow = null;
                } else {

                    AggregatedReportRow matrixInfoRow = new AggregatedReportRow();

                    matrixInfoRow.setSfId(row.getSfId());
                    matrixInfoRow.setMatrixStartTime(row.getMatrixStartTime());
                    matrixInfoRow.setMatrixFinishTime(row.getMatrixFinishTime());
                    matrixInfoRow.setMatrixName(row.getMatrixName());
                    matrixInfoRow.setUserName(row.getUserName());
                    matrixInfoRow.setHost(row.getHost());
                    matrixInfoRow.setPort(row.getPort());
                    matrixInfoRow.setSfName(row.getSfName());
                    matrixInfoRow.setServicesUsed(row.getServicesUsed());
                    matrixInfoRow.setEnvironmentName(row.getEnvironmentName());
                    matrixInfoRow.setTags(row.getTags());

                    matrixInfoRow.setMatrixRow(true);

                    matrixInfoRow.setReportFolder(row.getReportFolder());
                    matrixInfoRow.setReportFile("report");

                    matrixInfoRow.setMatrixRunId(row.getMatrixRunId());
                    matrixInfoRow.setSfCurrentInstance(row.getSfCurrentInstance());

                    rows.add(i, matrixInfoRow);
                    i++;

                    lastInfoRow = matrixInfoRow;
                }
            }

            if (!isMatrixFailedRow(row)) {
                if (row.getStatus() == StatusType.PASSED) {
                    numPassed++;
                    allCasesPassed++;
                } else if (row.getStatus() == StatusType.FAILED) {
                    numFailed++;
                    allCasesFailed++;
                } else if (row.getStatus() == StatusType.CONDITIONALLY_PASSED) {
                    numConditionallyPassed++;
                    allCasesConditionallyPassed++;
                }
            } else {
                allMatricesFailed++;
            }
        }

        if (lastInfoRow != null) {
            lastInfoRow.setPassedCount(numPassed);
            lastInfoRow.setFailedCount(numFailed);
            lastInfoRow.setConditionallyPassedCount(numConditionallyPassed);

            if (numFailed > 0) {
                allMatricesFailed++;
            } else if (numConditionallyPassed > 0) {
                allMatricesConditionallyPassed++;
            } else {
                allMatricesPassed++;
            }
        }

        return new MatrixInfo(allMatricesPassed, allMatricesFailed, allMatricesConditionallyPassed, allCasesPassed,
                              allCasesFailed, allCasesConditionallyPassed);
    }

    private static boolean isMatrixFailedRow(AggregatedReportRow row) {
        return row.getMatrixFailReason() != null && row.getTestCaseId() == null && row.getFinishTime() == null
                && row.getStartTime() == null;
    }

}