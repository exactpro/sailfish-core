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
import { connect } from 'react-redux';
import '../styles/layout.scss';
import { Panel } from '../util/Panel';
import Message from '../models/Message';
import { ToggleButton } from './ToggleButton';
import { MessagesCardList, MessagesCardListBase } from './message/MessagesCardList';
import { LogsPane } from './LogsPane';
import AppState from '../state/models/AppState';
import { isAdmin } from '../helpers/messageType';
import { isRejected } from '../helpers/messageType';
import { setRightPane, selectRejectedMessageId, setAdminMsgEnabled } from '../actions/actionCreators';
import { prevCyclicItemByIndex, nextCyclicItemByIndex } from '../helpers/array';
import { createSelector } from '../helpers/styleCreators';
import { AutoSizer } from 'react-virtualized';

const MIN_CONTROLS_WIDTH = 800,
    MIN_CONTROLS_WIDTH_WITH_REJECTED = 850;

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

interface RightPanelProps extends RightPanelStateProps, RightPanelDispatchProps { }

class RightPanelBase extends React.Component<RightPanelProps> {

    private messagesPanel = React.createRef<MessagesCardListBase>();

    scrollPanelToTop(panel: Panel) {
        switch (panel) {
            case (Panel.Messages): {
                this.messagesPanel.current && this.messagesPanel.current.scrollToTop();
                break;
            }

            default: {
                return;
            }
        }
    }

    render() {
        const { panel, rejectedMessages, adminMessages, selectedRejectedMessageId, adminMessagesEnabled, adminEnabledHandler } = this.props;

        const currentRejectedIndex = rejectedMessages.findIndex(msg => msg.id === selectedRejectedMessageId),
            rejectedEnabled = rejectedMessages.length != 0,
            adminControlEnabled = adminMessages.length != 0,
            controlShowTitles = true;

        const adminRootClass = createSelector(
            "layout-control",
            "selectable",
            adminControlEnabled ? null : "disabled"
        ),
            adminIconClass = createSelector(
                "layout-control__icon",
                "admin",
                adminMessagesEnabled ? "active" : null
            ),
            adminTitleClass = createSelector(
                "layout-control__title",
                adminMessagesEnabled ? "admin" : null
            ),
            rejectedRootClass = createSelector(
                "layout-control",
                rejectedEnabled ? null : "disabled"
            );

        return (
            <div className="layout-panel">
                <AutoSizer disableHeight>
                    {({ width }) => {
                        const showTitles = width > (rejectedEnabled ? MIN_CONTROLS_WIDTH_WITH_REJECTED : MIN_CONTROLS_WIDTH);

                        return (
                            <div className="layout-panel__controls" style={{ width }}>
                                <div className="layout-panel__tabs">
                                    <ToggleButton
                                        isToggled={panel == Panel.Messages}
                                        onClick={() => this.selectPanel(Panel.Messages)}
                                        text="Messages" />
                                    <ToggleButton
                                        isToggled={false}
                                        isDisabled={true}
                                        title="Not implemeted"
                                        text="Logs" />
                                    <ToggleButton
                                        isToggled={false}
                                        isDisabled={true}
                                        title="Not implemeted"
                                        text="Known bugs" />
                                </div>
                                <div className={adminRootClass}
                                    onClick={adminControlEnabled ? (() => adminEnabledHandler(!adminMessagesEnabled)) : undefined}
                                    title={(adminMessagesEnabled ? "Hide" : "Show") + " Admin messages"}>
                                    <div className={adminIconClass} />
                                    {
                                        showTitles ?
                                            <div className={adminTitleClass}>
                                                {controlShowTitles ? <p>{adminControlEnabled ? "" : "No"} Admin Messages</p> : null}
                                            </div> :
                                            null
                                    }
                                </div>
                                <div className={rejectedRootClass}>
                                    <div className="layout-control__icon rejected"
                                        onClick={() => this.currentRejectedHandler(currentRejectedIndex)}
                                        style={{ cursor: rejectedEnabled ? 'pointer' : 'unset' }}
                                        title={rejectedEnabled ? "Scroll to current rejected message" : null} />
                                    {showTitles ?
                                        <div className="layout-control__title">
                                            {controlShowTitles ? <p>{rejectedEnabled ? "" : "No "}Rejected</p> : null}
                                        </div> :
                                        null
                                    }
                                    {
                                        rejectedEnabled ?
                                            (
                                                [
                                                    <div className="layout-control__icon prev"
                                                        title="Scroll to previous rejected message"
                                                        onClick={rejectedEnabled && (() => this.prevRejectedHandler(currentRejectedIndex))} />,
                                                    <div className="layout-control__counter">
                                                        <p>{currentRejectedIndex + 1} of {rejectedMessages.length}</p>
                                                    </div>,
                                                    <div className="layout-control__icon next"
                                                        title="Scroll to next rejected message"
                                                        onClick={rejectedEnabled && (() => this.nextRejectedHandler(currentRejectedIndex))} />
                                                ]
                                            ) : null
                                    }
                                </div>
                                <div className="layout-control disabled"
                                    title="Show predictions (Not implemented)">
                                    <div className="layout-control__icon ml" />
                                    {
                                        showTitles ?
                                            <div className="layout-control__title">
                                                {controlShowTitles ? <p>Predictions</p> : null}
                                            </div> :
                                            null
                                    }
                                </div>
                            </div>
                        )
                    }}
                </AutoSizer>
                <div className="layout-panel__content">
                    {this.renderPanels(panel)}
                </div>
            </div>
        )
    }

    private renderPanels(selectedPanel: Panel): JSX.Element[] {
        const messagesRootClass = createSelector(
            "layout-panel__content-wrapper",
            selectedPanel == Panel.Messages ? "" : "disabled"
        ),
            knownBugsRootClass = createSelector(
                "layout-panel__content-wrapper",
                selectedPanel == Panel.KnownBugs ? "" : "disabled"
            ),
            logsRootClass = createSelector(
                "layout-panel__content-wrapper",
                selectedPanel == Panel.Logs ? "" : "disabled"
            );

        return [
            <div className={messagesRootClass}>
                <MessagesCardList
                    ref={this.messagesPanel} />
            </div>,
            <div className={logsRootClass}>
                <LogsPane />
            </div>,
            <div className={knownBugsRootClass}>
                {/* Known bugs panel */}
            </div>
        ];
    }

    private selectPanel(panel: Panel) {
        if (panel == this.props.panel) {
            this.scrollPanelToTop(panel);
        } else {
            this.props.panelSelectHandler(panel);
        }
    }

    private nextRejectedHandler(messageIdx: number) {
        this.props.selectRejectedMessageHandler(nextCyclicItemByIndex(this.props.rejectedMessages, messageIdx).id);
    }

    private prevRejectedHandler(messageIdx: number) {
        this.props.selectRejectedMessageHandler(prevCyclicItemByIndex(this.props.rejectedMessages, messageIdx).id);
    }

    private currentRejectedHandler(messageIdx: number) {
        const message = this.props.rejectedMessages[messageIdx];

        if (message) {
            this.props.selectRejectedMessageHandler(message.id);
        }
    }
}

export const RightPanel = connect(
    (state: AppState): RightPanelStateProps => ({
        adminMessages: state.selected.testCase.messages.filter(isAdmin),
        rejectedMessages: state.selected.testCase.messages.filter(isRejected),
        adminMessagesEnabled: state.view.adminMessagesEnabled,
        selectedRejectedMessageId: state.selected.rejectedMessageId,
        panel: state.view.rightPanel
    }),
    (dispatch): RightPanelDispatchProps => ({
        panelSelectHandler: (panel: Panel) => dispatch(setRightPane(panel)),
        selectRejectedMessageHandler: (messageId: number) => dispatch(selectRejectedMessageId(messageId)),
        adminEnabledHandler: (adminEnabled: boolean) => dispatch(setAdminMsgEnabled(adminEnabled))
    })
)(RightPanelBase);
