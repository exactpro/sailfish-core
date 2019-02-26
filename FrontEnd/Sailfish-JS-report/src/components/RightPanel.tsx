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
import { connect } from 'preact-redux';
import '../styles/layout.scss';
import { Panel } from '../helpers/Panel';
import Message from '../models/Message';
import { ToggleButton } from './ToggleButton';
import { MessagesCardList } from './MessagesCardList';
import { LogsPane } from './LogsPane';
import AppState from '../state/AppState';
import { isAdmin } from '../helpers/messageType';
import { isRejected } from '../helpers/messageType';
import { setRightPane, selectRejectedMessageId, setAdminMsgEnabled } from '../actions/actionCreators';

interface RightPanelStateProps {
    panel: Panel;
    rejectedMessages: Message[];
    adminMessages: Message[];
    selectedRejectedMessageId: number;
    adminMessagesEnabled: boolean;
}

interface RightPanelDispatchProps {
    panelSelectHandler: (panel: Panel) => any;
    selectRejectedMessageHandler: (messageId: number) => any;
    adminEnabledHandler: (adminEnabled: boolean) => any;
}

interface RightPanelProps extends RightPanelStateProps, RightPanelDispatchProps {}

const RightPanelBase = ({
        panel, 
        panelSelectHandler, 
        rejectedMessages, 
        adminMessages, 
        selectedRejectedMessageId, 
        selectRejectedMessageHandler, 
        adminMessagesEnabled,
        adminEnabledHandler
    }: RightPanelProps) => {

    const currentRejectedIndex = rejectedMessages.findIndex(msg => msg.id === selectedRejectedMessageId),
        rejectedEnabled = rejectedMessages.length != 0,
        adminControlEnabled = adminMessages.length != 0,
        controlShowTitles = true;

    const adminRootClass = [
            "layout-body-panel-controls-right-admin",
            adminControlEnabled ? "" : "disabled"
        ].join(' '),
        adminIconClass = [
            "layout-body-panel-controls-right-admin-icon",
            adminMessagesEnabled ? "active" : ""
        ].join(' '),
        adminTitleClass = [
            "layout-body-panel-controls-right-admin-title",
            adminMessagesEnabled ? "active" : ""
        ].join(' '),
        rejectedRootClass = [
            "layout-body-panel-controls-right-rejected",
            rejectedEnabled ? "" : "disabled"
        ].join(' ');

    const [nextRejectedHandler, prevRejectedHandler] = getRejectedHandler(rejectedMessages, selectRejectedMessageHandler);

    return (
        <div class="layout-body-panel">
            <div class="layout-body-panel-controls">
                <div class="layout-body-panel-controls-panels">
                    <ToggleButton
                        isToggled={panel == Panel.Messages}
                        click={() => panelSelectHandler(Panel.Messages)}
                        text="Messages" />
                    <div style={{filter: "opacity(0.4)"}} title="Not implemeted">
                        <ToggleButton
                            isToggled={false}
                            text="Logs"
                            click={() => {}}/>
                    </div>
                    <div style={{filter: "opacity(0.4)"}} title="Not implemeted">
                        <ToggleButton
                            isToggled={false}
                            text="Known bugs"
                            click={() => {}}/>
                    </div>

                    {/* <ToggleButton
                        isToggled={leftPane == Pane.Logs ||
                            (rightPane == Pane.Logs && splitMode)}
                        click={() => splitMode ? rightPaneHandler(Pane.Logs) : leftPaneHandler(Pane.Logs)}
                        text="Logs" /> */}
                </div>
                <div class="layout-body-panel-controls-right">
                    <div class={adminRootClass}
                        onClick={adminControlEnabled && (() => adminEnabledHandler(!adminMessagesEnabled))}
                        title={(adminMessagesEnabled ? "Hide" : "Show") + " Admin messages"}>
                        <div class={adminIconClass} />
                        <div class={adminTitleClass}>
                            {controlShowTitles ? <p>{adminControlEnabled ? "" : "No"} Admin Messages</p> : null}
                        </div>
                    </div>
                    <div class={rejectedRootClass}>
                        <div class="layout-body-panel-controls-right-rejected-icon"
                            title="Scroll to current rejected message"
                            onClick={() => this.scrollToMessage(selectedRejectedMessageId)} />
                        <div class="layout-body-panel-controls-right-rejected-title">
                            {controlShowTitles ? <p>{rejectedEnabled ? "" : "No "}Rejected</p> : null}
                        </div>
                        {
                            rejectedEnabled ? 
                            (
                                [
                                    <div class="layout-body-panel-controls-right-rejected-btn prev"
                                        title="Scroll to previous rejected message"
                                        onClick={rejectedEnabled && (() => prevRejectedHandler(currentRejectedIndex))} />,
                                    <div class="layout-body-panel-controls-right-rejected-count">
                                        <p>{currentRejectedIndex + 1} of {rejectedMessages.length}</p>
                                    </div>,
                                    <div class="layout-body-panel-controls-right-rejected-btn next"
                                        title="Scroll to next rejected message"
                                        onClick={rejectedEnabled && (() => nextRejectedHandler(currentRejectedIndex))} />
                                ]
                            ) : null
                        }
                    </div>
                    <div class="layout-body-panel-controls-right-predictions"
                        title="Show predictions (Not implemented)">
                        <div class="layout-body-panel-controls-right-predictions-icon"/>
                        <div class="layout-body-panel-controls-right-predictions-title">
                            {controlShowTitles ? <p>Predictions</p> : null}
                        </div>
                    </div>
                </div>
            </div>
            <div class="layout-body-panel-content">
                {renderPanels(panel)}
            </div>
        </div>
    )
}

function renderPanels(selectedPanel: Panel): JSX.Element[] {
    const messagesRootClass = [
            "layout-body-panel-content-wrapper",
            selectedPanel == Panel.Messages ? "" : "disabled"
        ].join(' '), 
        knownBugsRootClass = [
            "layout-body-panel-content-wrapper",
            selectedPanel == Panel.KnownBugs ? "" : "disabled"
        ].join(' '),
        logsRootClass = [
            "layout-body-panel-content-wrapper",
            selectedPanel == Panel.Logs ? "" : "disabled"
        ].join(' ');

    return [
        <div class={messagesRootClass}>
            <MessagesCardList/>
        </div>,
        <div class={logsRootClass}>
            <LogsPane/>
        </div>,
        <div class={knownBugsRootClass}>
            {/* Known bugs panel */}
        </div>
    ];
}

function getRejectedHandler(rejectedMessages: Message[], selectRejectedMessage: (messageId: Number) => any) {
    return [
        function nextRejectedHandler(messageIdx: number) {
            const idx = (messageIdx + 1) % rejectedMessages.length;
            selectRejectedMessage(rejectedMessages[idx].id);
        },
        function prevRejectedHandler(messageIdx: number) {
            const idx = (rejectedMessages.length + messageIdx - 1) % rejectedMessages.length;
            selectRejectedMessage(rejectedMessages[idx].id);
        }
    ]
}

export const RightPanel = connect(
    (state: AppState) : RightPanelStateProps => ({
        adminMessages: state.testCase.messages.filter(isAdmin),
        rejectedMessages: state.testCase.messages.filter(isRejected),
        adminMessagesEnabled: state.adminMessagesEnabled,
        selectedRejectedMessageId: state.selected.rejectedMessageId,
        panel: state.rightPane
    }),
    (dispatch) : RightPanelDispatchProps => ({
        panelSelectHandler: (panel: Panel) => dispatch(setRightPane(panel)),
        selectRejectedMessageHandler: (messageId: number) => dispatch(selectRejectedMessageId(messageId)),
        adminEnabledHandler: (adminEnabled: boolean) => dispatch(setAdminMsgEnabled(adminEnabled))
    })
)(RightPanelBase);
