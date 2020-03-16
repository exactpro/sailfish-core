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
import { ToggleButton } from './ToggleButton';
import { MessagesCardList, MessagesCardListBase } from './message/MessagesCardList';
import LogsList, { LogListActions } from './log/LogsList';
import AppState from '../state/models/AppState';
import { setRightPane } from '../actions/actionCreators';
import { createBemElement, createStyleSelector } from '../helpers/styleCreators';
import { KnownBugPanel } from "./knownbugs/KnownBugPanel";
import ResizeObserver from "resize-observer-polyfill";
import { getRejectedMessages } from "../selectors/messages";
import MessagePanelControl from "./message/MessagePanelControls";

const MIN_CONTROLS_WIDTH = 850,
    MIN_CONTROLS_WIDTH_WITH_REJECTED = 900;

type RightPanelType = Panel.MESSAGES | Panel.KNOWN_BUGS | Panel.LOGS;

interface StateProps {
    panel: RightPanelType;
    rejectedMessagesEnabled: boolean;
    hasLogs: boolean;
    hasErrorLogs: boolean;
    hasWarningLogs: boolean;
    hasKnownBugs: boolean;
}

interface DispatchProps {
    panelSelectHandler: (panel: RightPanelType) => void;
}

interface Props extends StateProps, DispatchProps {}

interface State {
    showTitles: boolean;
}

class RightPanelBase extends React.Component<Props, State> {

    private messagesPanel = React.createRef<MessagesCardListBase>();
    private logsPanel = React.createRef<LogListActions>();
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
            case (Panel.LOGS): {
                this.logsPanel.current?.scrollToTop();
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
        const { panel, hasLogs, hasErrorLogs, hasWarningLogs, hasKnownBugs } = this.props;

        const logChipClassName = createBemElement(
                'log-button',
                'chip',
                hasErrorLogs ? 'error' : hasWarningLogs ? 'warning' : 'hidden'
            ),
            messagesRootClass = createStyleSelector(
                "layout-panel__content-wrapper",
                panel == Panel.MESSAGES ? null : "disabled"
            ),
            knownBugsRootClass = createStyleSelector(
                "layout-panel__content-wrapper",
                panel == Panel.KNOWN_BUGS ? null : "disabled"
            ),
            logsRootClass = createStyleSelector(
                "layout-panel__content-wrapper",
                panel == Panel.LOGS ? null : "disabled"
            );

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
                    {this.getCurrentPanelControls()}
                </div>
                <div className="layout-panel__content">
                    <div className={messagesRootClass}>
                        <MessagesCardList
                            ref={this.messagesPanel}/>
                    </div>
                    <div className={logsRootClass}>
                        <LogsList
                            ref={this.logsPanel}
                            isActive={panel === Panel.LOGS}/>
                    </div>
                    <div className={knownBugsRootClass}>
                        <KnownBugPanel/>
                    </div>
                </div>
            </div>
        )
    }

    private selectPanel(panel: RightPanelType) {
        if (panel == this.props.panel) {
            this.scrollPanelToTop(panel);
        } else {
            this.props.panelSelectHandler(panel);
        }
    }
    
    private getCurrentPanelControls = () => {
        switch (this.props.panel) {
            case Panel.MESSAGES: {
                return <MessagePanelControl showTitles={this.state.showTitles}/>;
            }

            default: {
                return null;
            }
        }
    }
}

export const RightPanel = connect(
    (state: AppState): StateProps => ({
        rejectedMessagesEnabled: getRejectedMessages(state).length > 0,
        panel: state.view.rightPanel,
        hasLogs: state.selected.testCase.files.logentry.count > 0,
        hasErrorLogs: state.selected.testCase.hasErrorLogs,
        hasWarningLogs: state.selected.testCase.hasWarnLogs,
        hasKnownBugs: state.selected.testCase.bugs.length > 0
    }),
    (dispatch) => ({
        panelSelectHandler: (panel: RightPanelType) => dispatch(setRightPane(panel))
    })
)(RightPanelBase);
