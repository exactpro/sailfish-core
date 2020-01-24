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
import { isAdmin, isRejected } from '../helpers/messageType';
import { selectRejectedMessageId, setAdminMsgEnabled, setRightPane, togglePredictions, uglifyAllMessages } from '../actions/actionCreators';
import { createSelector, createTriStateControlClassName } from '../helpers/styleCreators';
import { isAction } from "../models/Action";
import { KnownBugPanel } from "./knownbugs/KnownBugPanel";
import { StatusType } from '../models/Status';
import RejectedMessagesCarousel from './RejectedMessagesCarousel';
import ResizeObserver from 'react-resize-observer';

const MIN_CONTROLS_WIDTH = 850,
    MIN_CONTROLS_WIDTH_WITH_REJECTED = 900;

interface StateProps {
    panel: Panel;
    adminMessages: Message[];
    adminMessagesEnabled: boolean;
    rejectedMessagesEnabled: boolean;
    predictionsEnabled: boolean;
    predictionsAvailable: boolean;
    hasKnownBugs: boolean;
    beautifiedMessagesEnabled: boolean;
}

interface DispatchProps {
    panelSelectHandler: (panel: Panel) => any;
    selectCurrentRejectedMessage: () => any;
    adminEnabledHandler: (adminEnabled: boolean) => any;
    togglePredictions: () => any;
    uglifyAllHandler: () => any;
}

interface Props extends StateProps, DispatchProps { }

interface State {
    showTitles: boolean;
}

class RightPanelBase extends React.Component<Props, State> {

    private messagesPanel = React.createRef<MessagesCardListBase>();

    constructor(props) {
        super(props);

        this.state = {
            showTitles: true
        }
    }

    scrollPanelToTop(panel: Panel) {
        switch (panel) {
            case (Panel.Messages): {
                this.messagesPanel.current?.scrollToTop();
                break;
            }

            default: {
                return;
            }
        }
    }

    render() {
        const { panel, rejectedMessagesEnabled, adminMessages, adminMessagesEnabled, adminEnabledHandler, selectCurrentRejectedMessage,
            predictionsEnabled, predictionsAvailable, beautifiedMessagesEnabled, uglifyAllHandler } = this.props;

        const adminControlEnabled = adminMessages.length != 0;

        const adminRootClass = createTriStateControlClassName("layout-control", adminMessagesEnabled, adminControlEnabled),
            adminIconClass = createTriStateControlClassName("layout-control__icon admin", adminMessagesEnabled, adminControlEnabled),
            adminTitleClass = createTriStateControlClassName("layout-control__title selectable", adminMessagesEnabled, adminControlEnabled),

            rejectedRootClass = createTriStateControlClassName("layout-control", true, rejectedMessagesEnabled),
            rejectedIconClass = createTriStateControlClassName("layout-control__icon rejected", true, rejectedMessagesEnabled),
            rejectedTitleClass = createTriStateControlClassName("layout-control__title", true, rejectedMessagesEnabled),

            predictionRootClass = createTriStateControlClassName("layout-control", predictionsEnabled, predictionsAvailable),
            predictionIconClass = createTriStateControlClassName("layout-control__icon prediction", predictionsEnabled, predictionsAvailable),
            predictionTitleClass = createTriStateControlClassName("layout-control__title prediction selectable", predictionsEnabled, predictionsAvailable);


        return (
            <div className="layout-panel">
                <div className="layout-panel__controls">
                    <ResizeObserver onResize={this.onResize}/>
                    <div className="layout-panel__tabs">
                        <ToggleButton
                            isToggled={panel == Panel.Messages}
                            onClick={() => this.selectPanel(Panel.Messages)}
                            text="Messages" />
                        <ToggleButton
                            isToggled={panel == Panel.KnownBugs}
                            isDisabled={!this.props.hasKnownBugs}
                            onClick={() => this.selectPanel(Panel.KnownBugs)}
                            text="Known bugs" />
                        <ToggleButton
                            isToggled={false}
                            isDisabled={true}
                            title="Not implemeted"
                            text="Logs" />
                    </div>
                    {
                        beautifiedMessagesEnabled ? (
                            <div className="layout-control"
                                title="Back to plain text"
                                onClick={() => uglifyAllHandler()}>
                                <div className="layout-control__icon beautifier" />
                            </div>
                        ) : null
                    }
                    <div className={adminRootClass}
                        onClick={adminControlEnabled ? (() => adminEnabledHandler(!adminMessagesEnabled)) : undefined}
                        title={(adminMessagesEnabled ? "Hide" : "Show") + " Admin messages"}>
                        <div className={adminIconClass} />
                        {
                            this.state.showTitles ?
                                <div className={adminTitleClass}>
                                    <p>{adminControlEnabled ? "" : "No"} Admin Messages</p>
                                </div> :
                                null
                        }
                    </div>
                    <div className={rejectedRootClass}>
                        <div className={rejectedIconClass}
                            onClick={() => rejectedMessagesEnabled && selectCurrentRejectedMessage()}
                            style={{ cursor: rejectedMessagesEnabled ? 'pointer' : 'unset' }}
                            title={rejectedMessagesEnabled ? "Scroll to current rejected message" : null} />
                        {
                            this.state.showTitles ?
                                <div className={rejectedTitleClass}>
                                    <p>{rejectedMessagesEnabled ? "" : "No "}Rejected</p>
                                </div> :
                                null
                        }
                        {
                            rejectedMessagesEnabled ?
                                <RejectedMessagesCarousel /> :
                                null
                        }
                    </div>
                    <div className={predictionRootClass}
                        title={predictionsEnabled ? "Hide predictions" : "Show predictions"}
                        onClick={this.onPredictionClick}>
                        <div className={predictionIconClass} />
                        <div className={predictionTitleClass}>
                            {
                                this.state.showTitles ?
                                    <p>{predictionsAvailable ? "Predictions" : "No predictions"}</p> :
                                    null
                            }
                        </div>
                    </div>
                </div>
                <div className="layout-panel__content">
                    {this.renderPanels(panel)}
                </div>
            </div>
        )
    }

    private renderPanels(selectedPanel: Panel): React.ReactNode {
        const messagesRootClass = createSelector(
            "layout-panel__content-wrapper",
            selectedPanel == Panel.Messages ? null : "disabled"
        ),
            knownBugsRootClass = createSelector(
                "layout-panel__content-wrapper",
                selectedPanel == Panel.KnownBugs ? null : "disabled"
            ),
            logsRootClass = createSelector(
                "layout-panel__content-wrapper",
                selectedPanel == Panel.Logs ? null : "disabled"
            );

        return (
            <React.Fragment>
                <div className={messagesRootClass}>
                    <MessagesCardList
                        ref={this.messagesPanel} />
                </div>
                <div className={logsRootClass}>
                    <LogsPane />
                </div>
                <div className={knownBugsRootClass}>
                    <KnownBugPanel />
                </div>
            </React.Fragment>
        );
    }

    private selectPanel(panel: Panel) {
        if (panel == this.props.panel) {
            this.scrollPanelToTop(panel);
        } else {
            this.props.panelSelectHandler(panel);
        }
    }

    private onResize = (rect: DOMRect) => {
        const minWidth = this.props.rejectedMessagesEnabled ? MIN_CONTROLS_WIDTH_WITH_REJECTED : MIN_CONTROLS_WIDTH,
            showTitles = rect.width > minWidth;
        
        if (this.state.showTitles !== showTitles) {
            this.setState({ showTitles });
        }
    }

    private onPredictionClick = () => {
        if (this.props.predictionsAvailable) {
            this.props.togglePredictions();
        }
    }
}

export const RightPanel = connect(
    (state: AppState) => ({
        adminMessages: state.selected.testCase.messages.filter(isAdmin),
        rejectedMessagesEnabled: state.selected.testCase.messages.filter(isRejected).length > 0,
        selectedRejectedMessageId: state.selected.rejectedMessageId,
        adminMessagesEnabled: state.view.adminMessagesEnabled.valueOf(),
        panel: state.view.rightPanel,
        beautifiedMessagesEnabled: state.view.beautifiedMessages.length > 0,

        predictionsAvailable:
            state.machineLearning.token != null
            && state.selected.testCase.messages.length > 0
            && state.selected.testCase.actions.some((action) => {
                return isAction(action) && action.status.status == StatusType.FAILED;
            }),

        predictionsEnabled: state.machineLearning.predictionsEnabled,
        hasKnownBugs: state.selected.testCase.bugs.length > 0
    }),
    (dispatch) => ({
        panelSelectHandler: (panel: Panel) => dispatch(setRightPane(panel)),
        selectRejectedMessage: (messageId: number) => dispatch(selectRejectedMessageId(messageId)),
        adminEnabledHandler: (adminEnabled: boolean) => dispatch(setAdminMsgEnabled(adminEnabled)),
        togglePredictions: () => dispatch(togglePredictions()),
        uglifyAllHandler: () => dispatch(uglifyAllMessages())
    }),
    ({ selectedRejectedMessageId, ...stateProps }, { selectRejectedMessage, ...dispatchProps }, ownProps): Props => ({
        ...stateProps,
        ...dispatchProps,
        ...ownProps,
        selectCurrentRejectedMessage: () => selectRejectedMessage(selectedRejectedMessageId)
    })
)(RightPanelBase);
