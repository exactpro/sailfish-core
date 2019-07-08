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

import * as React from 'react';
import Report from '../models/Report';
import { connect } from 'react-redux';
import AppState from '../state/models/AppState';
import { getSecondsPeriod, formatTime } from '../helpers/dateFormatter';
import { TestcaseMetadata } from '../models/TestcaseMetadata';
import "../styles/report.scss";
import { StatusType, statusValues } from '../models/Status';
import TestCaseCard from './TestCaseCard';
import { HeatmapScrollbar } from './heatmap/HeatmapScrollbar';
import { testCasesHeatmap } from '../helpers/heatmapCreator';
import { createSelector } from '../helpers/styleCreators';
import { loadTestCase } from '../thunks/loadTestCase';
import { ThunkDispatch } from 'redux-thunk';
import StateActionType from '../actions/stateActions';

const OLD_REPORT_PATH = 'report.html';

interface ReportLayoutProps {
    report: Report;
    onTestCaseSelect: (testCaseName: string) => void;
}

interface ReportLayoutState {
    showKnownBugs: boolean;
}

export class ReportLayoutBase extends React.Component<ReportLayoutProps, ReportLayoutState> {

    constructor(props) {
        super(props);

        this.state = {
            showKnownBugs: true
        };
    }

    toggleKnownBugs() {
        this.setState({
            showKnownBugs: !this.state.showKnownBugs
        })
    }

    render() {
        const knownBugsPresent = this.props.report.metadata.some(item => item.bugs != null && item.bugs.length > 0);

        const knownBugsClass = this.state.showKnownBugs ? "active" : "enabled";

        const knownBugsButton = (
            knownBugsPresent ?
                (
                    <div className={"report__known-bugs-button " + knownBugsClass} onClick={() => this.toggleKnownBugs()}>
                        <div className={"report__known-bugs-button__icon " + knownBugsClass} />
                        <div className={"report__known-bugs-button__text " + knownBugsClass}>Known bugs</div>
                    </div>
                ) : (
                    <div className="report__known-bugs-button disabled">
                        <div className="report__known-bugs-button__icon disabled" />
                        <div className="report__known-bugs-button__text disabled">No known bugs</div>
                    </div>
                )
        )

        const executionTime = getSecondsPeriod(this.props.report.startTime, this.props.report.finishTime),
            plugins = this.props.report.plugins ? Object.entries(this.props.report.plugins) : [];

        return (
            <div className="report">
                <div className="report__header   report-header">
                    <div className="report-header__title">{this.props.report.name}</div>
                    <a className="report-header__old-report-link" href={OLD_REPORT_PATH}>
                        <p>Old Version Report</p>
                    </a>
                </div>
                <div className="report__summary-title   report__title">
                    <p>Report Summary</p>
                </div>
                <div className="report__controls">
                    <div className="report__title">Test Cases</div>
                    {knownBugsButton}
                </div>
                <div className="report__summary   report-summary">
                    <div className="report-summary__card">
                        <div className="report-summary__logo"/>
                        <div className="report-summary__element">
                            <div className="report-summary__element-title">Version</div>
                            <div className="report-summary__element-value">{this.props.report.version}</div>
                        </div>
                        <div className="report-summary__divider"/>
                        <div className="report-summary__element">
                            <div className="report-summary__element-title">Host</div>
                            <div className="report-summary__element-value">{this.props.report.hostName}</div>
                        </div>
                        <div className="report-summary__element">
                            <div className="report-summary__element-title">User</div>
                            <div className="report-summary__element-value">{this.props.report.userName}</div>
                        </div>
                        <div className="report-summary__divider"/>
                        <div className="report-summary__element">
                            <div className="report-summary__element-title">ScriptRun ID</div>
                            <div className="report-summary__element-value">{this.props.report.scriptRunId}</div>
                        </div>
                        <div className="report-summary__element">
                            <div className="report-summary__element-title">Report Date</div>
                            <div className="report-summary__element-value">{formatTime(this.props.report.startTime)}</div>
                        </div>
                        <div className="report-summary__element">
                            <div className="report-summary__element-title">Execution time</div>
                            <div className="report-summary__element-value">{executionTime}</div>
                        </div>
                        <div className="report-summary__divider"/>
                        <div className="report-summary__element">
                            <div className="report-summary__element-title">Test Cases</div>
                            <div className="report-summary__element-value">{this.props.report.metadata.length}</div>
                        </div>
                        {
                            statusValues.map(statusValue => renderStatusInfo(statusValue, this.props.report.metadata))
                        }
                        <div className="report-summary__divider"/>
                        {
                            plugins.length ?
                                (
                                    <div className="report-summary__element">
                                        <div className="report-summary__element-title">Plugins</div>
                                        <div className="report-summary__element-value">
                                            {plugins.map(([name, version], index) => <p key={index}>{name}: {version}</p>)}
                                        </div>
                                    </div>
                                ) : (
                                    <div className="report-summary__element">
                                        <div className="report-summary__element-title">No plugins</div>
                                    </div>
                                )
                        }
                    </div>
                </div>
                <div className="report__testcases">
                    <HeatmapScrollbar
                        selectedElements={testCasesHeatmap(this.props.report.metadata)}
                        elementsCount={this.props.report.metadata.length}>
                        {
                            this.props.report.metadata.map((metadata, index) => (
                                <TestCaseCard
                                    knownBugsEnabled={this.state.showKnownBugs}
                                    key={index}
                                    metadata={metadata}
                                    index={index + 1}
                                    handleClick={metadata => this.props.onTestCaseSelect(metadata.jsonpFileName)}/>
                            ))
                        }
                    </HeatmapScrollbar>
                </div>
            </div>
        )
    }
}

function renderStatusInfo(status: StatusType, metadata: TestcaseMetadata[]): React.ReactNode {
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
        <div className="report-summary__element" key={status}>
            <div className={valueClassName}>{status.toUpperCase()}</div>
            <div className={valueClassName}>{testCasesCount}</div>
        </div>
    )
}

const ReportLayout = connect(
    (state: AppState) => ({
        report: state.report
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateActionType>) => ({
        onTestCaseSelect: (testCasePath: string) => dispatch(loadTestCase(testCasePath))
    })
)(ReportLayoutBase);

export default ReportLayout;
