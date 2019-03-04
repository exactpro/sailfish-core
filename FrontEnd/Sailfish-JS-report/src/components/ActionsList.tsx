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
import AppState from '../state/models/AppState';
import { selectAction, selectCheckpoint, selectVerification } from '../actions/actionCreators';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { actionsHeatmap } from '../helpers/heatmapCreator';
import { getActions } from '../helpers/actionType';
import { VirtualizedList } from './VirtualizedList';

interface ListProps {
    actions: Array<Action>;
    checkpointActions: Array<Action>;
    selectedActionId: number[];
    scrolledActionId: Number;
    selectedMessageId: number;
    selectedCheckpointId: number;
    actionsFilter: StatusType[];
    filterFields: StatusType[];
    onSelect: (messages: Action) => any;
    onMessageSelect: (id: number, status: StatusType) => any;
    setSelectedCheckpoint: (action: Action) => any;
}

export class ActionsListBase extends Component<ListProps, {}> {

    //private elements: ActionTree[] = [];
    private scrollbar: HeatmapScrollbar;

    scrollToTop() {
        this.scrollbar && this.scrollbar.scrollToTop();
    }
    private elements: any[] = [];

    scrollToAction(actionId: number) {
        // if (this.elements[actionId]) {
        //     // smooth behavior is disabled here
        //     // base - get HTMLElement by ref
        //     this.elements[actionId].base.scrollIntoView({block: 'center'});
        // }    
    }

    shouldComponentUpdate(nextProps: ListProps) {

        if (nextProps.scrolledActionId !== this.props.scrolledActionId) {
            return true;
        }

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

    render({ actions, selectedCheckpointId, selectedActionId, scrolledActionId, selectedMessageId, onSelect, actionsFilter, filterFields, onMessageSelect, setSelectedCheckpoint, checkpointActions }: ListProps) {

        return (
            <div class="actions">
                <div class="actions__list">
                    <VirtualizedList
                        rowCount={actions.length}
                        elementRenderer={idx => (
                            <ActionTree 
                                action={actions[idx]}
                                actionSelectHandler={onSelect}
                                messageSelectHandler={onMessageSelect}
                                actionsFilter={actionsFilter}
                                filterFields={filterFields} 
                                checkpoints={checkpointActions}
                                checkpointSelectHandler={action => setSelectedCheckpoint(action)} 
                                ref={ref => this.elements[actions[idx].id] = ref}/>
                        )}/>

                    {/* <HeatmapScrollbar
                        selectedElements={actionsHeatmap(actions, selectedActionId)}>
                        {actions.map(action => (
                            <ActionTree 
                                action={action}
                                selectedActionsId={selectedActionId}
                                selectedMessageId={selectedMessageId}
                                selectedCheckpointId={selectedCheckpointId}
                                scrolledActionId={scrolledActionId}
                                actionSelectHandler={onSelect}
                                messageSelectHandler={onMessageSelect}
                                actionsFilter={actionsFilter}
                                filterFields={filterFields} 
                                checkpoints={checkpointActions}
                                checkpointSelectHandler={action => setSelectedCheckpoint(action)} 
                                ref={ref => this.elements[action.id] = ref}/>))}
                    </HeatmapScrollbar> */}
                </div>
            </div> 
        )
    }
}   

export const ActionsList = connect((state: AppState) => ({
        actions: state.selected.testCase.actions,
        selectedActionId: state.selected.actionsId,
        scrolledActionId: state.selected.scrolledActionId,
        selectedMessageId: state.selected.actionsId.length == 0 ? state.selected.messagesId[0] : null,
        selectedCheckpointId: state.selected.checkpointActionId,
        actionsFilter: state.filter.actionsFilter,
        filterFields: state.filter.fieldsFilter,
        checkpointActions: state.selected.checkpointActions
    }),
    dispatch => ({
        onSelect: (action: Action) => dispatch(selectAction(action)),
        onMessageSelect: (id: number, status: StatusType) => dispatch(selectVerification(id, status)),
        setSelectedCheckpoint: (checkpointAction: Action) => dispatch(selectCheckpoint(checkpointAction))
    }),
    null,
    {
        withRef: true
    }
)(ActionsListBase);