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

import { h, Component } from 'preact';
import { connect } from 'preact-redux';
import { Header } from './Header';
import { SplitView } from './SplitView'
import { ActionsList, ActionsListBase } from './ActionsList';
import TestCase from '../models/TestCase';
import { ToggleButton } from './ToggleButton';
import { MessagesCardList, MessagesCardListBase } from './MessagesCardList';
import { StatusPane } from './StatusPane';
import { LogsPane } from './LogsPane';
import AppState from '../state/AppState';
import { setLeftPane, setRightPane } from '../actions/actionCreators';
import { Pane } from '../helpers/Pane';
import '../styles/layout.scss';

interface LayoutProps {
    testCase: TestCase;
    splitMode: boolean;
    showFilter: boolean;
    leftPane: Pane;
    rightPane: Pane;
    leftPaneHandler: (pane: Pane) => any;
    rightPaneHandler: (pane: Pane) => any;
}

class TestCaseLayoutBase extends Component<LayoutProps, any> {

    private actionsListRef: ActionsListBase;
    private messagesListRef: MessagesCardListBase;

    constructor(props: LayoutProps) {
        super(props);
    }

    // FIXME : need to move this logic to redux
    scrollToTopHandler() {
        if (this.actionsListRef) {
            this.actionsListRef.scrollToAction(this.props.testCase.actions[0].id);
        }

        if (this.messagesListRef) {
            this.messagesListRef.scrollToMessage(this.props.testCase.messages[0].id);
        }
    }

    render({ testCase, splitMode, showFilter, leftPane, rightPane, leftPaneHandler, rightPaneHandler }: LayoutProps) {

        const leftButtons = (
            <div class="layout-buttons">
                <ToggleButton
                    isToggled={leftPane == Pane.Actions}
                    click={() => leftPaneHandler(Pane.Actions)}
                    text="Actions" />
                <ToggleButton
                    isToggled={leftPane == Pane.Status}
                    click={() => leftPaneHandler(Pane.Status)}
                    text="Status" />
            </div>
        )

        // DISABLED known bugs and logs
        const rightButtons = (
            <div class="layout-buttons">
                <ToggleButton
                    isToggled={leftPane == Pane.Messages ||
                        (rightPane == Pane.Messages && splitMode)}
                    click={() => splitMode ? rightPaneHandler(Pane.Messages) : leftPaneHandler(Pane.Messages)}
                    text="Messages" />
                <div style={{filter: "opacity(0.4)"}} title="Not implemeted">
                    <ToggleButton
                        isToggled={false}
                        text="Logs"
                        click={() => alert("Not implemented...")}/>
                </div>
                <div style={{filter: "opacity(0.4)"}} title="Not implemeted">
                    <ToggleButton
                        isToggled={false}
                        text="Known bugs"
                        click={() => alert("Not implemented...")}/>
                </div>

                {/* <ToggleButton
                    isToggled={leftPane == Pane.Logs ||
                        (rightPane == Pane.Logs && splitMode)}
                    click={() => splitMode ? rightPaneHandler(Pane.Logs) : leftPaneHandler(Pane.Logs)}
                    text="Logs" /> */}
            </div>
        )
        
        const actionsPanelClass = ["layout-content-pane", (leftPane == Pane.Actions ? "" : "disabled")].join(' '),
            actionsPanel = (
                <div class={actionsPanelClass}>
                    {leftButtons}
                    <ActionsList 
                        ref={ref => this.actionsListRef = ref ? ref.getWrappedInstance() : null} />
                </div>
            );

        const messagesPanelClass = ["layout-content-pane", (rightPane == Pane.Messages ? "" : "disabled")].join(' '),
            messagesPane = (
                <div class={messagesPanelClass}>
                    {rightButtons}
                    <MessagesCardList ref={ref => this.messagesListRef = ref ? ref.getWrappedInstance() : null} />
                </div>
            );

        const statusPanelClass = ["layout-content-pane", (leftPane == Pane.Status ? "" : "disabled")].join(' '),
            statusPane = (
                <div class={statusPanelClass}>
                    {leftButtons}
                    <StatusPane status={testCase.status} />
                </div>
            );

        const logsPanelClass = ["layout-content-pane", (rightPane == Pane.Logs ? "" : "disabled")].join(' '),
            logsPane = (
                <div class={logsPanelClass}>
                    {rightButtons}
                    <LogsPane logs={testCase.logs} />
                </div>
            );

        const primaryPaneElement = (
            <div class="layout-content-wrapper">
                {[
                    actionsPanel,
                    statusPane
                ]}
            </div>
        );

        const secondaryPaneElement = (
            <div class="layout-content-wrapper">
                {[
                    messagesPane,
                    logsPane
                ]}
            </div>
        );

        const rootClassName = ["layout", (showFilter ? "filter" : "")].join(' ');

        return (
            <div class={rootClassName}>
                <div class="layout-header">
                    <Header goTopHandler={() => this.scrollToTopHandler()} />
                </div>
                {splitMode ?
                    (<div class="layout-content split">
                        <SplitView
                            minPanelPercentageWidth={30}>
                            {primaryPaneElement}
                            {secondaryPaneElement}
                        </SplitView>
                    </div>) :
                    (<div class="layout-content">
                        {primaryPaneElement}
                    </div>)}
            </div>
        )
    }
}

const TestCaseLayout = connect(
    (state: AppState) => ({
        testCase: state.testCase,
        splitMode: state.splitMode,
        showFilter: state.showFilter,
        leftPane: state.leftPane,
        rightPane: state.rightPane
    }),
    dispatch => ({
        leftPaneHandler: (pane: Pane) => dispatch(setLeftPane(pane)),
        rightPaneHandler: (pane: Pane) => dispatch(setRightPane(pane))
    })
)(TestCaseLayoutBase);

export default TestCaseLayout;