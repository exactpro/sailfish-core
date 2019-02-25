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
import Action from '../models/Action';
import '../styles/action.scss';
import { ActionTree } from './ActionTree';
import { StatusType } from '../models/Status';
import AppState from '../state/AppState';
import { connect } from 'preact-redux';
import { selectAction, selectMessages, selectCheckpoint } from '../actions/actionCreators';
import { isCheckpoint } from '../helpers/messageType';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { actionsHeatmap } from '../helpers/heatmapCreator';

const ACTION_CHECKPOINT_NAME = "GetCheckPoint";

interface ListProps {
    actions: Array<Action>;
    selectedActionId: number;
    selectedMessageId: number;
    selectedCheckpointId: number;
    actionsFilter: StatusType[];
    filterFields: StatusType[];
    onSelect: (messages: Action) => any;
    onMessageSelect: (id: number, status: StatusType) => any;
    setSelectedCheckpoint: (action: Action) => any;
}

export class ActionsListBase extends Component<ListProps, {}> {

    private elements: ActionTree[] = [];
    private checkpointActions: Action[] = [];

    scrollToAction(actionId: number) {
        if (this.elements[actionId]) {
            // smooth behavior is disabled here
            // base - get HTMLElement by ref
            this.elements[actionId].base.scrollIntoView({block: 'center'});
        }    
    }

    componentWillMount() {
        this.checkpointActions = this.getAllCheckpoints(this.props.actions);
    }

    componentWillUpdate(nextProps: ListProps) {
        if (nextProps.actions != this.props.actions) {
            this.checkpointActions = this.getAllCheckpoints(nextProps.actions);
        }
    }

    shouldComponentUpdate(nextProps: ListProps) {
        if (nextProps.filterFields !== this.props.filterFields) {
            return true;
        }

        if (nextProps.actionsFilter !== this.props.actionsFilter) {
            return true;
        }

        if (nextProps.selectedCheckpointId !== this.props.selectedCheckpointId) {
            return true;
        }

        return nextProps.actions !== this.props.actions ||
            nextProps.selectedActionId !== this.props.selectedActionId ||
            nextProps.selectedMessageId !== this.props.selectedMessageId;
    }

    render({ actions, selectedCheckpointId, selectedActionId, selectedMessageId, onSelect, actionsFilter, filterFields, onMessageSelect, setSelectedCheckpoint }: ListProps) {

        const cpIndex = this.checkpointActions.findIndex(action => action.id == selectedCheckpointId),
            cpEnabled = this.checkpointActions.length != 0,
            cpRootClass = [
                "actions-controls-checkpoints",
                cpEnabled ? "" : "disabled"
            ].join(' ');

        return (
            <div class="actions">
                <div class="actions-controls">
                    <div class={cpRootClass}>
                        <div class="actions-controls-checkpoints-icon"/>
                        <div class="actions-controls-checkpoints-title">
                            <p>{cpEnabled ? "" : "No "}Checkpoints</p>
                        </div>
                        <div class="actions-controls-checkpoints-btn prev"
                            onClick={cpEnabled && (() => this.prevCpHandler(cpIndex))}/>
                        <div class="actions-controls-checkpoints-count">
                            <p>{cpIndex === -1 ? 0 : cpIndex + 1} of {this.checkpointActions.length}</p>
                        </div>
                        <div class="actions-controls-checkpoints-btn next"
                            onClick={cpEnabled && (() => this.nextCpHandler(cpIndex))}/>
                    </div>
                </div>
                <div class="actions-list">
                    <HeatmapScrollbar
                        selectedElements={actionsHeatmap(actions, selectedActionId)}>
                        {actions.map(action => (
                            <ActionTree 
                                action={action}
                                selectedActionId={selectedActionId}
                                selectedMessageId={selectedMessageId}
                                selectedCheckpointId={selectedCheckpointId}
                                actionSelectHandler={onSelect}
                                messageSelectHandler={onMessageSelect}
                                actionsFilter={actionsFilter}
                                filterFields={filterFields} 
                                checkpoints={this.checkpointActions}
                                checkpointSelectHandler={action => setSelectedCheckpoint(action)} 
                                ref={ref => this.elements[action.id] = ref}/>))}
                    </HeatmapScrollbar>
                </div>
            </div> 
        )
    }

    getAllCheckpoints(actions: Action[]): Action[] {
        return actions.reduce((checkpoints, action) => [...checkpoints, ...this.getActionCheckpoints(action)], []);
    }

    private getActionCheckpoints(action: Action, checkpoints: Action[] = []): Action[]  {
        if (action.name == ACTION_CHECKPOINT_NAME) {
            return [...checkpoints, action];
        }

        return action.subNodes.reduce((checkpoints, subNode) => {
            if (subNode.actionNodeType == 'action') {
                return [...checkpoints, ...this.getActionCheckpoints(subNode as Action, checkpoints)];
            } else {
                return checkpoints;
            }
        }, [])
    }

    private nextCpHandler (currentCpIndex: number) {
        if (this.checkpointActions[currentCpIndex + 1]) {
            this.props.setSelectedCheckpoint(this.checkpointActions[currentCpIndex + 1]);
        } else {
            this.props.setSelectedCheckpoint(this.checkpointActions[0]);
        }
    }

    private prevCpHandler (currentCpIndex: number) {
        if (this.checkpointActions[currentCpIndex - 1]) {
            this.props.setSelectedCheckpoint(this.checkpointActions[currentCpIndex - 1]);
        } else {
            this.props.setSelectedCheckpoint(this.checkpointActions[this.checkpointActions.length - 1]);
        }
    }
}   

export const ActionsList = connect((state: AppState) => ({
        actions: state.testCase.actions,
        selectedActionId: state.selected.actionId,
        selectedMessageId: state.selected.actionId ? null : state.selected.messagesId[0],
        selectedCheckpointId: state.selected.checkpointActionId,
        actionsFilter: state.actionsFilter,
        filterFields: state.fieldsFilter
    }),
    dispatch => ({
        onSelect: (action: Action) => dispatch(selectAction(action)),
        onMessageSelect: (id: number, status: StatusType) => dispatch(selectMessages([id], status)),
        setSelectedCheckpoint: (checkpointAction: Action) => dispatch(selectCheckpoint(checkpointAction))
    }),
    null,
    {
        withRef: true
    }
)(ActionsListBase);