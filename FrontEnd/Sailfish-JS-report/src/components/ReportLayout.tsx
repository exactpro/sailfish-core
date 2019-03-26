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
            <div class="header">
                <div class="title">{report.name}</div>
                <a href={OLD_REPORT_PATH} class="old-report-link">
                    <p>Old Version Report</p>
                </a>
            </div>
            <div class="summary-title">
                <p>Report Summary</p>
            </div>
            <div class="controls">
                <div class="title">Test Cases</div>
            </div>
            <div class="summary">
                <div class="card">
                    <div class="logo" />
                    <div class="info-list">
                        <div class="item">
                            <div class="title">Version</div>
                            <div class="value">{report.version}</div>
                        </div>
                        <div class="divider" />
                        <div class="item">
                            <div class="title">Host</div>
                            <div class="value">{report.hostName}</div>
                        </div>
                        <div class="item">
                            <div class="title">User</div>
                            <div class="value">{report.userName}</div>
                        </div>
                        <div class="divider"/>
                        <div class="item">
                            <div class="title">ScriptRun ID</div>
                            <div class="value">{report.scriptRunId}</div>
                        </div>
                        <div class="item">
                            <div class="title">Report Date</div>
                            <div class="value">{formatTime(report.startTime)}</div>
                        </div>
                        <div class="item">
                            <div class="title">Execution time</div>
                            <div class="value">{executionTime}</div>
                        </div>
                        <div class="divider" />
                        <div class="item">
                            <div class="title">Test Cases</div>
                            <div class="value">{report.metadata.length}</div>
                        </div>
                        {
                            statusValues.map(statusValue => renderStatusInfo(statusValue, report.metadata))
                        }
                        <div class="divider" />
                        {
                            plugins.length ?
                                (
                                    <div class="item">
                                        <div class="title">Plugins</div>
                                        <div class="value">
                                            {plugins.map(([name, version]) => <p>{name}: {version}</p>)}
                                        </div>
                                    </div>
                                ) : (
                                    <div class="item">
                                        <div class="title">No plugins</div>
                                    </div>
                                )
                        }
                    </div>
                </div>
            </div>
            <div class="testcases">
                <div class="list">  
                    <HeatmapScrollbar
                        selectedElements={testCasesHeatmap(report.metadata)}>
                        {
                            report.metadata.map((metadata, index) => (
                                <div class="item">
                                    <TestCaseCard
                                        metadata={metadata}
                                        index={index + 1}
                                        selectHandler={metadata => onTestCaseSelect(metadata.jsonpFileName)}/>
                                </div>
                            ))
                        }
                    </HeatmapScrollbar>
                </div>
            </div>
        </div>
    )
}

function renderStatusInfo(status: StatusType, metadata: ReportMetadata[]): JSX.Element {
    const testCasesCount = metadata.filter(metadata => metadata.status.status == status).length,
        valueClassName = [
            "value",
            status.toLowerCase()
        ].join(' ');

    if (!testCasesCount) {
        return null;
    }

    return (
        <div class="item bold">
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