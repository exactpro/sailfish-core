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
import { ToggleButton } from './ToggleButton';
import Action from '../models/Action';
import { ActionsList } from './action/ActionsList';
import AppState from '../state/models/AppState';
import { setLeftPane, selectCheckpointAction } from '../actions/actionCreators';
import { StatusPanel } from './StatusPanel';
import { ActionsListBase } from './action/ActionsList';
import { nextCyclicItemByIndex, prevCyclicItemByIndex } from '../helpers/array';
import { createSelector } from '../helpers/styleCreators';

interface LeftPanelProps {
    panel: Panel;
    checkpointActions: Action[]
    selectedCheckpointId: number;
    statusEnabled: boolean;
    panelSelectHandler: (panel: Panel) => any;
    setSelectedCheckpoint: (checkpointAciton: Action) => any;
}

class LeftPanelBase extends React.Component<LeftPanelProps> {

    private actionPanel = React.createRef<ActionsListBase>();
    
    scrollPanelToTop(panel: Panel) {
        switch (panel) {
            case Panel.Actions: {
                this.actionPanel.current && this.actionPanel.current.scrollToTop();
                break;
            }
            default: {
                return;
            }
        }
    }

    render() {
        const { panel, checkpointActions, selectedCheckpointId, statusEnabled } = this.props;

        const cpIndex = checkpointActions.findIndex(action => action.id == selectedCheckpointId),
            cpEnabled = checkpointActions.length != 0,
            cpRootClass = createSelector(
                "layout-control",
                cpEnabled ? null : "disabled"
            );

        return (
            <div className="layout-panel">
                <div className="layout-panel__controls">
                    <div className="layout-panel__tabs">
                        <ToggleButton
                            isToggled={panel == Panel.Actions}
                            onClick={() => this.selectPanel(Panel.Actions)}
                            text="Actions" />
                        <ToggleButton
                            isToggled={panel == Panel.Status}
                            isDisabled={!statusEnabled}
                            onClick={statusEnabled ? (() => this.selectPanel(Panel.Status)) : undefined}
                            text="Status" 
                            title={statusEnabled ? null : "No status info"}/>
                    </div>
                    <div className={cpRootClass}>
                        <div className="layout-control__icon cp"
                            onClick={() => this.currentCpHandler(cpIndex)}
                            style={{ cursor: cpEnabled ? 'pointer' : 'unset' }}
                            title={ cpEnabled ? "Scroll to current checkpoint" : null }/>
                        <div className="layout-control__title">
                            <p>{cpEnabled ? "" : "No "}Checkpoints</p>
                        </div>
                        {
                            cpEnabled ? 
                            (
                                <React.Fragment>
                                    <div className="layout-control__icon prev"
                                        onClick={cpEnabled && (() => this.prevCpHandler(cpIndex))}/>
                                    <div className="layout-control__counter">
                                        <p>{cpIndex + 1} of {checkpointActions.length}</p>
                                    </div>
                                    <div className="layout-control__icon next"
                                        onClick={cpEnabled && (() => this.nextCpHandler(cpIndex))}/>
                                </React.Fragment>
                            ) : null
                        }
                    </div>
                </div>
                <div className="layout-panel__content">
                    {this.renderPanels(panel)}
                </div>
            </div>
        )
    }

    private renderPanels(selectedPanel: Panel): React.ReactNode {
        const actionRootClass = createSelector(
                "layout-panel__content-wrapper",
                selectedPanel == Panel.Actions ? null : "disabled"
            ),
            statusRootClass = createSelector(
                "layout-panel__content-wrapper",
                selectedPanel == Panel.Status ? null : "disabled"
            );
    
        return (
            <React.Fragment>
                <div className={actionRootClass}>
                    <ActionsList
                        ref={this.actionPanel}/>
                </div>
                <div className={statusRootClass}>
                    <StatusPanel/>
                </div>
            </React.Fragment>
        )
    }

    private selectPanel(panel: Panel) {
        if (panel == this.props.panel) {
            this.scrollPanelToTop(panel);
        } else {
            this.props.panelSelectHandler(panel);
        }
    }

    private nextCpHandler(currentCpIndex: number) {
        this.props.setSelectedCheckpoint(nextCyclicItemByIndex(this.props.checkpointActions, currentCpIndex));
    }

    private prevCpHandler(currentCpIndex: number) {
        this.props.setSelectedCheckpoint(prevCyclicItemByIndex(this.props.checkpointActions, currentCpIndex));
    }

    private currentCpHandler(cpIndex: number) {
        const cp = this.props.checkpointActions[cpIndex];

        if (cp) {
            this.props.setSelectedCheckpoint(cp);
        }
    }
}

export const LeftPanel = connect(
    (state: AppState) => ({
        panel: state.view.leftPanel,
        selectedCheckpointId: state.selected.checkpointActionId,
        checkpointActions: state.selected.checkpointActions,
        statusEnabled: !!state.selected.testCase.status.cause
    }),
    dispatch => ({
        panelSelectHandler: (panel: Panel) => dispatch(setLeftPane(panel)),
        setSelectedCheckpoint: (checkpointAciton: Action) => dispatch(selectCheckpointAction(checkpointAciton))
    })
)(LeftPanelBase)
