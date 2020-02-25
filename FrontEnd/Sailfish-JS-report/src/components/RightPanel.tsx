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
import '../styles/layout.scss';
import Panel from '../util/Panel';
import Message from '../models/Message';
import { ToggleButton } from './ToggleButton';
import { MessagesCardList, MessagesCardListBase } from './message/MessagesCardList';
import LogsList from './log/LogsList';
import AppState from '../state/models/AppState';
import { isAdmin, isRejected } from '../helpers/messageType';
import {
    selectRejectedMessageId,
    setAdminMsgEnabled,
    setRightPane,
    togglePredictions,
    uglifyAllMessages
} from '../actions/actionCreators';
import { createStyleSelector, createTriStateControlClassName, createBemElement } from '../helpers/styleCreators';
import { isAction } from "../models/Action";
import { KnownBugPanel } from "./knownbugs/KnownBugPanel";
import { StatusType } from '../models/Status';
import RejectedMessagesCarousel from './RejectedMessagesCarousel';
import ResizeObserver from "resize-observer-polyfill";

const MIN_CONTROLS_WIDTH = 850,
    MIN_CONTROLS_WIDTH_WITH_REJECTED = 900;

interface StateProps {
    panel: Panel.MESSAGES | Panel.KNOWN_BUGS | Panel.LOGS;
    adminMessages: Message[];
    adminMessagesEnabled: boolean;
    rejectedMessagesEnabled: boolean;
    predictionsEnabled: boolean;
    predictionsAvailable: boolean;
    hasKnownBugs: boolean;
    beautifiedMessagesEnabled: boolean;
    hasLogs: boolean;
    hasErrorLogs: boolean;
    hasWarningLogs: boolean;
}

interface DispatchProps {
    panelSelectHandler: (panel: Panel.MESSAGES | Panel.KNOWN_BUGS | Panel.LOGS) => void;
    selectCurrentRejectedMessage: () => void;
    adminEnabledHandler: (adminEnabled: boolean) => void;
    togglePredictions: () => void;
    uglifyAllHandler: () => void;
}

interface Props extends StateProps, DispatchProps {}

interface State {
    showTitles: boolean;
}

class RightPanelBase extends React.Component<Props, State> {

    private messagesPanel = React.createRef<MessagesCardListBase>();
    private root = React.createRef<HTMLDivElement>();

    private rootResizeObserver = new ResizeObserver(elements => {
        const minWidth = this.props.rejectedMessagesEnabled ? MIN_CONTROLS_WIDTH_WITH_REJECTED : MIN_CONTROLS_WIDTH,
            showTitles = elements[0]?.contentRect.width > minWidth;

        if (this.state.showTitles !== showTitles) {
            this.setState({ showTitles });
        }
    });

    constructor(props) {
        super(props);

        this.state = {
            showTitles: true
        }
    }

    scrollPanelToTop(panel: Panel) {
        switch (panel) {
            case (Panel.MESSAGES): {
                this.messagesPanel.current?.scrollToTop();
                break;
            }

            default: {
                return;
            }
        }
    }

    componentDidMount() {
        this.rootResizeObserver.observe(this.root.current);
    }

    componentWillUnmount() {
        this.rootResizeObserver.unobserve(this.root.current);
    }

    render() {
        const {
            panel, rejectedMessagesEnabled, adminMessages, adminMessagesEnabled, adminEnabledHandler, selectCurrentRejectedMessage,
            predictionsEnabled, predictionsAvailable, beautifiedMessagesEnabled, uglifyAllHandler, hasLogs, hasKnownBugs,
            hasErrorLogs, hasWarningLogs
        } = this.props;

        const adminControlEnabled = adminMessages.length != 0;

        const logChipClassName = createBemElement('log-button', 'chip', hasErrorLogs? 'error': hasWarningLogs? 'warning': 'hiden')

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
            <div className="layout-panel" ref={this.root}>
                <div className="layout-panel__controls">
                    <div className="layout-panel__tabs">
                        <ToggleButton
                            isToggled={panel == Panel.MESSAGES}
                            onClick={() => this.selectPanel(Panel.MESSAGES)}>
                                Messages
                        </ToggleButton>
                        <ToggleButton
                            isToggled={panel == Panel.KNOWN_BUGS}
                            isDisabled={!hasKnownBugs}
                            onClick={() => this.selectPanel(Panel.KNOWN_BUGS)}>
                                Known bugs
                        </ToggleButton>
                        <ToggleButton
                            isDisabled={!hasLogs}
                            isToggled={panel == Panel.LOGS}
                            onClick={() => this.selectPanel(Panel.LOGS)}>
                                <div className="log-button">
                                    <p>Logs</p>
                                    <div className={logChipClassName}/>
                                </div>
                        </ToggleButton>
                    </div>
                    {
                        beautifiedMessagesEnabled ? (
                            <div className="layout-control"
                                 title="Back to plain text"
                                 onClick={() => uglifyAllHandler()}>
                                <div className="layout-control__icon beautifier"/>
                            </div>
                        ) : null
                    }
                    <div className={adminRootClass}
                         onClick={adminControlEnabled ? (() => adminEnabledHandler(!adminMessagesEnabled)) : undefined}
                         title={(adminMessagesEnabled ? "Hide" : "Show") + " Admin messages"}>
                        <div className={adminIconClass}/>
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
                             title={rejectedMessagesEnabled ? "Scroll to current rejected message" : null}/>
                        {
                            this.state.showTitles ?
                                <div className={rejectedTitleClass}>
                                    <p>{rejectedMessagesEnabled ? "" : "No "}Rejected</p>
                                </div> :
                                null
                        }
                        {
                            rejectedMessagesEnabled ?
                                <RejectedMessagesCarousel/> :
                                null
                        }
                    </div>
                    <div className={predictionRootClass}
                         title={predictionsEnabled ? "Hide predictions" : "Show predictions"}
                         onClick={this.onPredictionClick}>
                        <div className={predictionIconClass}/>
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
        const messagesRootClass = createStyleSelector(
            "layout-panel__content-wrapper",
            selectedPanel == Panel.MESSAGES ? null : "disabled"
            ),
            knownBugsRootClass = createStyleSelector(
                "layout-panel__content-wrapper",
                selectedPanel == Panel.KNOWN_BUGS ? null : "disabled"
            ),
            logsRootClass = createStyleSelector(
                "layout-panel__content-wrapper",
                selectedPanel == Panel.LOGS ? null : "disabled"
            );

        return (
            <React.Fragment>
                <div className={messagesRootClass}>
                    <MessagesCardList
                        ref={this.messagesPanel}/>
                </div>
                <div className={logsRootClass}>
                    <LogsList
                        isActive={this.props.panel === Panel.LOGS} />
                </div>
                <div className={knownBugsRootClass}>
                    <KnownBugPanel/>
                </div>
            </React.Fragment>
        );
    }

    private selectPanel(panel: Panel.MESSAGES | Panel.KNOWN_BUGS | Panel.LOGS) {
        if (panel == this.props.panel) {
            this.scrollPanelToTop(panel);
        } else {
            this.props.panelSelectHandler(panel);
        }
    }

    private onPredictionClick = () => {
        if (this.props.predictionsAvailable) {
            this.props.togglePredictions();
        }
    }
}

export const RightPanel = connect(
    (state: AppState): StateProps & { selectedRejectedMessageId: number } => ({
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
        hasKnownBugs: state.selected.testCase.bugs.length > 0,
        hasLogs: state.selected.testCase.files.logentry.count > 0,
        hasErrorLogs: state.selected.testCase.hasErrorLogs,
        hasWarningLogs: state.selected.testCase.hasWarnLogs
    }),
    (dispatch) => ({
        panelSelectHandler: (panel: Panel.MESSAGES | Panel.KNOWN_BUGS | Panel.LOGS) => dispatch(setRightPane(panel)),
        selectRejectedMessage: (messageId: number) => dispatch(selectRejectedMessageId(messageId)),
        adminEnabledHandler: (adminEnabled: boolean) => dispatch(setAdminMsgEnabled(adminEnabled)),
        togglePredictions: () => dispatch(togglePredictions()),
        uglifyAllHandler: () => dispatch(uglifyAllMessages()),
    }),
    ({ selectedRejectedMessageId, ...stateProps }, { selectRejectedMessage, ...dispatchProps }, ownProps): Props => ({
        ...stateProps,
        ...dispatchProps,
        ...ownProps,
        selectCurrentRejectedMessage: () => selectRejectedMessage(selectedRejectedMessageId)
    })
)(RightPanelBase);
