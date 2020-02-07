/******************************************************************************
* Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
import { connect } from 'react-redux';
import AppState from '../state/models/AppState';
import { getSecondsPeriod, formatTime } from '../helpers/date';
import { TestCaseMetadata, isTestCaseMetadata } from '../models/TestcaseMetadata';
import "../styles/report.scss";
import { StatusType, statusValues } from '../models/Status';
import HeatmapScrollbar from './heatmap/HeatmapScrollbar';
import { testCasesHeatmap } from '../helpers/heatmapCreator';
import { createStyleSelector, createBemElement } from '../helpers/styleCreators';
import { ExceptionChain } from './ExceptionChain';
import TestCaseCard from './TestCaseCard';
import { FailedTestCaseCarousel } from './FailedTestCaseCarousel';
import ReportState from '../state/models/ReportState';
import AlertCard from './AlertCard';
import Tag from './Tag';

const OLD_REPORT_PATH = 'report.html';

interface StateProps {
    report: ReportState;
    selectedTestCaseId: string;
}

interface Props extends StateProps { }

interface State {
    showKnownBugs: boolean;
}

export class ReportLayoutBase extends React.Component<Props, State> {

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

        const { report, selectedTestCaseId } = this.props,
            { showKnownBugs } = this.state;

        const filteredMetadata = report.metadata.filter(isTestCaseMetadata),
            knownBugsPresent = filteredMetadata.some(item => item.bugs != null && item.bugs.length > 0),
            knownBugsClass = showKnownBugs ? "active" : "enabled",
            failedTestCasesEnabled = filteredMetadata.some(({status}) => status.status === StatusType.FAILED),
            failedTcTitleClass = createBemElement('report', 'title', failedTestCasesEnabled ? 'failed': 'disabled'),
            isLive = report.finishTime == null,
            alerts = report.alerts || [],
            tags = report.tags || [];

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
        );

        const executionTime = getSecondsPeriod(report.startTime, report.finishTime),
            plugins = report.plugins ? Object.entries(report.plugins) : [];

        return (
            <div className="report">
                <div className="report__header   report-header">
                    {
                        isLive ?
                            <div className="report-header__live-loader"
                                title="Report executing in progress"/> :
                            null
                    }
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
                    <div className={failedTcTitleClass}>
                        { failedTestCasesEnabled ? 'Failed' : 'No Failed' }
                    </div>
                    <FailedTestCaseCarousel/>
                    {knownBugsButton}
                </div>
                <div className="report__summary">
                    <div className="report-summary">
                        <div className="report-summary__card">
                            <div className="report-summary__logo"/>
                            <div className="report-summary__element">
                                <div className="report-summary__element-title">Version</div>
                                <div className="report-summary__element-value">{report.version}</div>
                            </div>
                            <div className="report-summary__divider"/>
                            <div className="report-summary__element">
                                <div className="report-summary__element-title">Host</div>
                                <div className="report-summary__element-value">{report.hostName}</div>
                            </div>
                            <div className="report-summary__element">
                                <div className="report-summary__element-title">User</div>
                                <div className="report-summary__element-value">{report.userName}</div>
                            </div>
                            <div className="report-summary__divider"/>
                            <div className="report-summary__element">
                                <div className="report-summary__element-title">ScriptRun ID</div>
                                <div className="report-summary__element-value">{report.scriptRunId}</div>
                            </div>
                            <div className="report-summary__element">
                                <div className="report-summary__element-title">Report Date</div>
                                <div className="report-summary__element-value">{formatTime(report.startTime)}</div>
                            </div>
                            <div className="report-summary__element">
                                <div className="report-summary__element-title">Execution time</div>
                                <div className="report-summary__element-value">{executionTime}</div>
                            </div>
                            <div className="report-summary__divider"/>
                            <div className="report-summary__element">
                                <div className="report-summary__element-title">Test Cases</div>
                                <div className="report-summary__element-value">{report.metadata.length}</div>
                            </div>
                            {
                                statusValues.map(statusValue => this.renderStatusInfo(statusValue, filteredMetadata))
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
                            <div className="report-summary__divider"/>
                            {
                                tags.length ?
                                    (
                                        <div className="report-summary__tags">
                                            <div className="report-summary__element report-summary__tags-header">
                                                <div className="report-summary__element-title">Tags</div>
                                                <div className="report-summary__element-value">{tags.length}</div>
                                            </div>
                                            <div className="report-summary__tags-list">
                                                {tags.map((tag, i) => <Tag tag={tag} key={i}/>)}
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="report-summary__element">
                                            <div className="report-summary__element-title">No tags</div>
                                        </div>
                                    )
                            }
                        </div>
                        <div className="report-summary__alerts">
                            {alerts.map((alert, i) => <AlertCard {...alert} key={i}/>)}
                        </div>
                    </div>
                </div>
                <div className="report__testcases">
                    {
                        report.metadata.length > 0 || report.exception == null ? (
                            <HeatmapScrollbar
                                selectedElements={testCasesHeatmap(report.metadata)}
                                elementsCount={report.metadata.length}>
                                {
                                    report.metadata.map((metadata, index) => (
                                        <TestCaseCard
                                            knownBugsEnabled={showKnownBugs}
                                            isSelected={metadata.id === selectedTestCaseId}
                                            key={index}
                                            metadata={metadata}
                                            index={index + 1}/>
                                    ))
                                }
                            </HeatmapScrollbar>
                        ) : (
                            <ExceptionChain
                                exception={report.exception}/>
                        )
                    }
                </div>
            </div>
        )
    }

    private renderStatusInfo(status: StatusType, metadata: TestCaseMetadata[]): React.ReactNode {
        const testCasesCount = metadata.filter(metadata => metadata.status.status == status).length,
            valueClassName = createStyleSelector(
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
}

const ReportLayout = connect(
    (state: AppState): StateProps => ({
        report: state.report,
        selectedTestCaseId: state.selected.selectedTestCaseId
    })
)(ReportLayoutBase);

export default ReportLayout;
