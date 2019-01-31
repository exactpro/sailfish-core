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
import TestCase from '../models/TestCase';
import { connect } from 'preact-redux';
import AppState from '../state/AppState';
import { setTestCasePath } from '../actions/actionCreators';
import "../styles/report.scss";
import { getSecondsPeriod } from '../helpers/dateFormatter';

interface ReportLayoutProps {
    report: Report;
    onTestCaseSelect: (testCaseName: string) => void;
}

const ReportLayoutBase = ({ report, onTestCaseSelect }: ReportLayoutProps) => {

    const executionTime = getSecondsPeriod(report.startTime, report.finishTime);
        
    // use *1000 to get correct time
    const date = new Date(report.startTime * 1000).toString();
    
    let testCaseCount = 1;

    return (
        <div class="report">
            <div class="report-header">
                <p>Report script: {report.name}</p>
            </div>
            <div class="report-info">
                <div class="report-info-logo"></div>
                <div class="report-info-table">
                    <table>
                        <thead>
                            <th class="report-info-table-name" />
                            <th class="report-info-table-value" />
                        </thead>
                        <tbody>
                            <tr>
                                <td class="report-info-table-name">Host:</td>
                                <td>{report.hostName}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">User:</td>
                                <td>{report.userName}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Execution time:</td>
                                <td>{executionTime}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">ScriptRun Id:</td>
                                <td>{report.scriptRunId}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Date:</td>
                                <td>{date}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Version:</td>
                                <td>{report.version}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Plugins:</td>
                                <td>
                                    {
                                        Object.entries(report.plugins).map(([name, version]) => (
                                            <p>{name}: {version}</p>
                                        ))
                                    }
                                </td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Test cases:</td>
                                <td>{report.testCaseLinks.length}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="report-testcases">
                <div class="report-testcases-list">
                    {report.testCaseLinks.map(testCase => (<div
                        style={{ cursor: "pointer" }}
                        onClick={() => onTestCaseSelect(testCase)}>
                        Test Case {testCaseCount++} : {testCase}
                    </div>))}
                </div>
            </div>
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