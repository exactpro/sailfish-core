/******************************************************************************
* Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import { h } from 'preact';
import Report from '../models/Report';
import { connect } from 'preact-redux';
import AppState from '../state/AppState';
import { setTestCasePath } from '../actions/actionCreators';
import { getSecondsPeriod, formatTime } from '../helpers/dateFormatter';
import { ReportMetadata } from '../models/ReportMetadata';
import "../styles/report.scss";
import { StatusType, statusValues } from '../models/Status';
import TestCaseCard from './TestCaseCard';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { testCasesHeatmap } from '../helpers/heatmapCreator';
import { createSelector } from '../helpers/styleCreators';

const OLD_REPORT_PATH = 'report.html';

interface ReportLayoutProps {
    report: Report;
    onTestCaseSelect: (testCaseName: string) => void;
}

const ReportLayoutBase = ({ report, onTestCaseSelect }: ReportLayoutProps) => {

    const executionTime = getSecondsPeriod(report.startTime, report.finishTime),
        plugins = report.plugins ? Object.entries(report.plugins) : [];

    return (
        <div class="report">
            <div class="report__header   report-header">
                <div class="report-header__title">{report.name}</div>
                <a class="report-header__old-report-link" href={OLD_REPORT_PATH}>
                    <p>Old Version Report</p>
                </a>
            </div>
            <div class="report__summary-title   report__title">
                <p>Report Summary</p>
            </div>
            <div class="report__controls">
                <div class="report__title">Test Cases</div>
            </div>
            <div class="report__summary   report-summary">
                <div class="report-summary__card">
                    <div class="report-summary__logo" />
                    <div class="report-summary__element">
                        <div class="report-summary__element-title">Version</div>
                        <div class="report-summary__element-value">{report.version}</div>
                    </div>
                    <div class="report-summary__divider" />
                    <div class="report-summary__element">
                        <div class="report-summary__element-title">Host</div>
                        <div class="report-summary__element-value">{report.hostName}</div>
                    </div>
                    <div class="report-summary__element">
                        <div class="report-summary__element-title">User</div>
                        <div class="report-summary__element-value">{report.userName}</div>
                    </div>
                    <div class="report-summary__divider"/>
                    <div class="report-summary__element">
                        <div class="report-summary__element-title">ScriptRun ID</div>
                        <div class="report-summary__element-value">{report.scriptRunId}</div>
                    </div>
                    <div class="report-summary__element">
                        <div class="report-summary__element-title">Report Date</div>
                        <div class="report-summary__element-value">{formatTime(report.startTime)}</div>
                    </div>
                    <div class="report-summary__element">
                        <div class="report-summary__element-title">Execution time</div>
                        <div class="report-summary__element-value">{executionTime}</div>
                    </div>
                    <div class="report-summary__divider" />
                    <div class="report-summary__element">
                        <div class="report-summary__element-title">Test Cases</div>
                        <div class="report-summary__element-value">{report.metadata.length}</div>
                    </div>
                    {
                        statusValues.map(statusValue => renderStatusInfo(statusValue, report.metadata))
                    }
                    <div class="report-summary__divider" />
                    {
                        plugins.length ?
                            (
                                <div class="report-summary__element">
                                    <div class="report-summary__element-title">Plugins</div>
                                    <div class="report-summary__element-value">
                                        {plugins.map(([name, version]) => <p>{name}: {version}</p>)}
                                    </div>
                                </div>
                            ) : (
                                <div class="report-summary__element">
                                    <div class="report-summary__element-title">No plugins</div>
                                </div>
                            )
                    }
                </div>
            </div>
            <div class="report__testcases">
                <HeatmapScrollbar
                    selectedElements={testCasesHeatmap(report.metadata)}>
                    {
                        report.metadata.map((metadata, index) => (
                            <TestCaseCard
                                metadata={metadata}
                                index={index + 1}
                                handleClick={metadata => onTestCaseSelect(metadata.jsonpFileName)}/>
                        ))
                    }
                </HeatmapScrollbar>
            </div>
        </div>
    )
}

function renderStatusInfo(status: StatusType, metadata: ReportMetadata[]): JSX.Element {
    const testCasesCount = metadata.filter(metadata => metadata.status.status == status).length,
        valueClassName = createSelector(
            "report-summary__element-value",
            "bold",
            status.toLowerCase()
        );

    if (!testCasesCount) {
        return null;
    }

    return (
        <div class="report-summary__element">
            <div class={valueClassName}>{status.toUpperCase()}</div>
            <div class={valueClassName}>{testCasesCount}</div>
        </div>
    )
}

const ReportLayout = connect(
    (state: AppState) => ({
        report: state.report
    }),
    dispatch => ({
        onTestCaseSelect: (testCaseLink: string) => dispatch(setTestCasePath(testCaseLink))
    })
)(ReportLayoutBase);

export default ReportLayout;