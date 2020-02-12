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
import { getSecondsPeriod } from '../helpers/date';
import { isTestCaseMetadata, TestCaseMetadata } from '../models/TestcaseMetadata';
import "../styles/report.scss";
import { StatusType } from '../models/Status';
import HeatmapScrollbar from './heatmap/HeatmapScrollbar';
import { testCasesHeatmap } from '../helpers/heatmapCreator';
import { createStyleSelector, createBemElement } from '../helpers/styleCreators';
import { ExceptionChain } from './ExceptionChain';
import TestCaseCard from './TestCaseCard';
import { FailedTestCaseCarousel } from './FailedTestCaseCarousel';
import ReportState from '../state/models/ReportState';
import { ToggleButton } from './ToggleButton';
import RunInformation from './RunInformation';
import ReportSummary from './ReportSummary';
import Tag from './Tag';

const OLD_REPORT_PATH = 'report.html';

interface StateProps {
    report: ReportState;
    selectedTestCaseId: string;
}

interface Props extends StateProps { }

enum Panel {ReportSummary, RunInfo}

interface State {
    showKnownBugs: boolean;
    panel: Panel;
}

export class ReportLayoutBase extends React.Component<Props, State> {

    constructor(props) {
        super(props);

        this.state = {
            showKnownBugs: true,
            panel: Panel.ReportSummary
        };
    }

    toggleKnownBugs() {
        this.setState({
            showKnownBugs: !this.state.showKnownBugs
        })
    }

    getPanel(): React.ReactNode {
        return this.state.panel === Panel.RunInfo &&
            <RunInformation report={this.props.report} /> ||
            this.state.panel === Panel.ReportSummary &&
            <ReportSummary report={this.props.report} />;
    }

    changePanel(panel: Panel) {
        this.setState({
            panel: panel
        })
    }

    render() {
        const changePanel= (panel: Panel) => {
            return () => {
                this.changePanel(panel)
            }
        }

        const { report, selectedTestCaseId } = this.props,
            { showKnownBugs } = this.state;

        const filteredMetadata = report.metadata.filter(isTestCaseMetadata),
            knownBugsPresent = filteredMetadata.some(item => item.bugs != null && item.bugs.length > 0),
            knownBugsClass = showKnownBugs ? "active" : "enabled",
            failedTestCasesEnabled = filteredMetadata.some(({status}) => status.status === StatusType.FAILED),
            failedTcTitleClass = createBemElement('report', 'title', failedTestCasesEnabled ? 'failed': 'disabled'),
            isLive = report.finishTime == null;

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
                            <div className="report-summary__element">
                                <div className="report-summary__logo"/>
                                <div className="report-summary__vertical_element">
                                    <div className="report-summary__element-title">Version</div>
                                    <div className="report-summary__element-value">{report.version}</div>
                                </div>
                            </div>
                            <div className="layout-panel__tabs">
                                <ToggleButton
                                    textClass="report-summary__button_text"
                                    isToggled={this.state.panel == Panel.ReportSummary}
                                    onClick={changePanel(Panel.ReportSummary)}
                                    text="Report Summary"/>
                                <ToggleButton
                                    textClass="report-summary__button_text"
                                    isToggled={this.state.panel == Panel.RunInfo}
                                    onClick={changePanel(Panel.RunInfo)}
                                    text="Run Information"/>
                            </div>
                            {
                                this.getPanel()
                            }
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
}

const ReportLayout = connect(
    (state: AppState): StateProps => ({
        report: state.report,
        selectedTestCaseId: state.selected.selectedTestCaseId
    })
)(ReportLayoutBase);

export default ReportLayout;
