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
import '../styles/action.scss';
import Action from '../models/Action';
import { ActionTree } from './ActionTree';
import { StatusType } from '../models/Status';
import AppState from '../state/AppState';
import { selectAction, selectMessages, selectCheckpoint } from '../actions/actionCreators';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { actionsHeatmap } from '../helpers/heatmapCreator';
import { getActions } from '../helpers/actionType';

interface ListProps {
    actions: Array<Action>;
    checkpointActions: Array<Action>;
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
    private scrollbar: HeatmapScrollbar;
    private root: HTMLElement;

    scrollToTop() {
        this.scrollbar && this.scrollbar.scrollToTop();
    }

    scrollToAction(actionId: number) {
        if (this.elements[actionId]) {
            // smooth behavior is disabled here
            // base - get HTMLElement by ref
            this.elements[actionId].base.scrollIntoView({block: 'center'});
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

    render({ actions, selectedCheckpointId, selectedActionId, selectedMessageId, onSelect, actionsFilter, filterFields, onMessageSelect, setSelectedCheckpoint, checkpointActions }: ListProps) {

        return (
            <div class="actions">
                <div class="actions-list"
                    ref={ref => this.root = ref}>
                    <HeatmapScrollbar
                        selectedElements={actionsHeatmap(getActions(actions), selectedActionId)}
                        ref={ref => this.scrollbar = ref}>
                        {actions.map(action => (
                            <ActionTree 
                                action={action}
                                selectedActionsId={[selectedActionId]}
                                selectedMessageId={selectedMessageId}
                                selectedCheckpointId={selectedCheckpointId}
                                actionSelectHandler={onSelect}
                                messageSelectHandler={onMessageSelect}
                                actionsFilter={actionsFilter}
                                filterFields={filterFields} 
                                checkpoints={checkpointActions}
                                checkpointSelectHandler={action => setSelectedCheckpoint(action)} 
                                ref={ref => this.elements[action.id] = ref}/>))}
                    </HeatmapScrollbar>
                </div>
            </div> 
        )
    }
}   

export const ActionsList = connect((state: AppState) => ({
        actions: state.testCase.actions,
        selectedActionId: state.selected.actionId,
        selectedMessageId: state.selected.actionId ? null : state.selected.messagesId[0],
        selectedCheckpointId: state.selected.checkpointActionId,
        actionsFilter: state.actionsFilter,
        filterFields: state.fieldsFilter,
        checkpointActions: state.checkpointActions
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