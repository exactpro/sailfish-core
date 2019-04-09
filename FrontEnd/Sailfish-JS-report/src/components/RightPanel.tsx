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
import '../styles/layout.scss';
import { Panel } from '../helpers/Panel';
import Message from '../models/Message';
import { ToggleButton } from './ToggleButton';
import { MessagesCardList, MessagesCardListBase } from './MessagesCardList';
import { LogsPane } from './LogsPane';
import AppState from '../state/AppState';
import { isAdmin } from '../helpers/messageType';
import { isRejected } from '../helpers/messageType';
import { setRightPane, selectRejectedMessageId, setAdminMsgEnabled } from '../actions/actionCreators';
import { prevCyclicItemByIndex, nextCyclicItemByIndex } from '../helpers/array';
import { createSelector } from '../helpers/styleCreators';

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

class RightPanelBase extends Component<RightPanelProps> {

    private messagesPanel: MessagesCardListBase;

    scrollPanelToTop(panel: Panel) {
        switch(panel) {
            case(Panel.Messages): {
                this.messagesPanel.scrollToTop();
                break;
            }

            default: {
                return;
            }
        }
    }

    render({panel, rejectedMessages, adminMessages, selectedRejectedMessageId, adminMessagesEnabled, adminEnabledHandler}: RightPanelProps) {

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
            <div class="layout-panel">
                <div class="layout-panel__controls">
                    <div class="layout-panel__tabs">
                        <ToggleButton
                            isToggled={panel == Panel.Messages}
                            onClick={() => this.selectPanel(Panel.Messages)}
                            text="Messages" />
                        <ToggleButton
                            isToggled={false}
                            isDisabled={true}
                            title="Not implemeted"
                            text="Logs"/>
                        <ToggleButton
                            isToggled={false}
                            isDisabled={true}
                            title="Not implemeted"
                            text="Known bugs"/>
                    </div>
                    <div class={adminRootClass}
                        onClick={adminControlEnabled && (() => adminEnabledHandler(!adminMessagesEnabled))}
                        title={(adminMessagesEnabled ? "Hide" : "Show") + " Admin messages"}>
                        <div class={adminIconClass} />
                        <div class={adminTitleClass}>
                            {controlShowTitles ? <p>{adminControlEnabled ? "" : "No"} Admin Messages</p> : null}
                        </div>
                    </div>
                    <div class={rejectedRootClass}>
                        <div class="layout-control__icon rejected"
                            onClick={() => this.currentRejectedHandler(currentRejectedIndex)}
                            style={{ cursor: rejectedEnabled ? 'pointer' : 'unset' }}
                            title={ rejectedEnabled ? "Scroll to current rejected message" : null }/>
                        <div class="layout-control__title">
                            {controlShowTitles ? <p>{rejectedEnabled ? "" : "No "}Rejected</p> : null}
                        </div>
                        {
                            rejectedEnabled ? 
                            (
                                [
                                    <div class="layout-control__icon prev"
                                        title="Scroll to previous rejected message"
                                        onClick={rejectedEnabled && (() => this.prevRejectedHandler(currentRejectedIndex))} />,
                                    <div class="layout-control__counter">
                                        <p>{currentRejectedIndex + 1} of {rejectedMessages.length}</p>
                                    </div>,
                                    <div class="layout-control__icon next"
                                        title="Scroll to next rejected message"
                                        onClick={rejectedEnabled && (() => this.nextRejectedHandler(currentRejectedIndex))} />
                                ]
                            ) : null
                        }
                    </div>
                    <div class="layout-control disabled"
                        title="Show predictions (Not implemented)">
                        <div class="layout-control__icon ml"/>
                        <div class="layout-control__title">
                            {controlShowTitles ? <p>Predictions</p> : null}
                        </div>
                    </div>
                </div>
                <div class="layout-panel__content">
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
            <div class={messagesRootClass}>
                <MessagesCardList
                    ref={ref => this.messagesPanel = ref ? ref.wrappedInstance : null}/>
            </div>,
            <div class={logsRootClass}>
                <LogsPane/>
            </div>,
            <div class={knownBugsRootClass}>
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
