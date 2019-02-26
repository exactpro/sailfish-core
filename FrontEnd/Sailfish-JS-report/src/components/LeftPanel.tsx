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
import { ToggleButton } from './ToggleButton';
import Action from '../models/Action';
import { ActionsList } from './ActionsList';
import AppState from '../state/AppState';
import { setLeftPane, selectCheckpoint } from '../actions/actionCreators';
import { StatusPanel } from './StatusPane';

interface LeftPanelProps {
    panel: Panel;
    checkpointActions: Action[]
    selectedCheckpointId: number;
    panelSelectHandler: (panel: Panel) => any;
    setSelectedCheckpoint: (checkpointAciton: Action) => any;
}

const LeftPanelBase = ({panel, panelSelectHandler, checkpointActions, setSelectedCheckpoint, selectedCheckpointId}: LeftPanelProps) => {

    const cpIndex = checkpointActions.findIndex(action => action.id == selectedCheckpointId),
        cpEnabled = checkpointActions.length != 0,
        cpRootClass = [
            "layout-body-panel-controls-left-checkpoints",
            cpEnabled ? "" : "disabled"
        ].join(' ');

    const [nextCpHandler, prevCpHandler] = getCpHanler(setSelectedCheckpoint, checkpointActions);

    return (
        <div class="layout-body-panel">
            <div class="layout-body-panel-controls">
                <div class="layout-body-panel-controls-panels">
                    <ToggleButton
                        isToggled={panel == Panel.Actions}
                        click={() => panelSelectHandler(Panel.Actions)}
                        text="Actions" />
                    <ToggleButton
                        isToggled={panel == Panel.Status}
                        click={() => panelSelectHandler(Panel.Status)}
                        text="Status" />
                </div>
                <div class="layout-body-panel-controls-left">
                    <div class="layout-body-panel-controls-left-checkpoints">
                        <div class={cpRootClass}>
                            <div class="layout-body-panel-controls-left-checkpoints-icon"/>
                            <div class="layout-body-panel-controls-left-checkpoints-title">
                                <p>{cpEnabled ? "" : "No "}Checkpoints</p>
                            </div>
                            {
                                cpEnabled ? 
                                (
                                    [
                                        <div class="layout-body-panel-controls-left-checkpoints-btn prev"
                                            onClick={cpEnabled && (() => prevCpHandler(cpIndex))}/>,
                                        <div class="layout-body-panel-controls-left-checkpoints-count">
                                            <p>{cpIndex + 1} of {checkpointActions.length}</p>
                                        </div>,
                                        <div class="layout-body-panel-controls-left-checkpoints-btn next"
                                            onClick={cpEnabled && (() => nextCpHandler(cpIndex))}/>
                                    ]
                                ) : null
                            }
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
    const actionRootClass = [
            "layout-body-panel-content-wrapper",
            selectedPanel == Panel.Actions ? "" : "disabled"
        ].join(' '),
        statusRootClass = [
            "layout-body-panel-content-wrapper",
            selectedPanel == Panel.Status ? "" : "disabled"
        ].join(' ');

    return [
        <div class={actionRootClass}>
            <ActionsList/>
        </div>,
        <div class={statusRootClass}>
            <StatusPanel/>
        </div>
    ]
}

function getCpHanler(setSelectedCheckpoint: (cp: Action) => any, checkpointActions: Action[]) {
    return [
        function nextCpHandler (currentCpIndex: number) {
            const idx = (currentCpIndex + 1) % checkpointActions.length;
            setSelectedCheckpoint(checkpointActions[idx]);
        },
        function prevCpHandler (currentCpIndex: number) {
            const idx = (checkpointActions.length + currentCpIndex - 1) % checkpointActions.length;
            setSelectedCheckpoint(checkpointActions[idx]);
        }
    ]
}

export const LeftPanel = connect(
    (state: AppState) => ({
        panel: state.leftPane,
        actions: state.testCase.actions,
        selectedCheckpointId: state.selected.checkpointActionId,
        checkpointActions: state.checkpointActions
    }),
    dispatch => ({
        panelSelectHandler: (panel: Panel) => dispatch(setLeftPane(panel)),
        setSelectedCheckpoint: (checkpointAciton: Action) => dispatch(selectCheckpoint(checkpointAciton))
    })
)(LeftPanelBase)