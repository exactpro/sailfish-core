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
import { selectAction, selectCheckpoint, selectVerification, saveMlData } from '../actions/actionCreators';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { actionsHeatmap } from '../helpers/heatmapCreator';
import { getActions } from '../helpers/actionType';
import { PredictionData } from '../models/MlServiceResponse';
import { fetchPredictions } from '../helpers/machineLearning';

interface ListProps {
    actions: Array<Action>;
    checkpointActions: Array<Action>;
    selectedActionId: number[];
    scrolledActionId: Number;
    selectedMessageId: number;
    selectedCheckpointId: number;
    actionsFilter: StatusType[];
    filterFields: StatusType[];
    activeActionId: number;
    token: string;
    mlDataActionIds: Set<number>;
    onSelect: (messages: Action) => any;
    onVerificationSelect: (messageId: number, actionId: number, status: StatusType) => any;
    setSelectedCheckpoint: (action: Action) => any;
    saveMlData: (data: PredictionData[]) => any;
}

export class ActionsListBase extends Component<ListProps, {}> {

    private elements: any[] = [];
    private scrollbar: HeatmapScrollbar;

    scrollToTop() {
        this.scrollbar && this.scrollbar.scrollToTop();
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

        if (nextProps.token !== this.props.token) {
            return true;
        }

        // size check is enough since there is no way ml prediction data could be removed from the storage
        if (nextProps.mlDataActionIds.size !== this.props.mlDataActionIds.size) {
            return true;
        }

        return nextProps.actions !== this.props.actions ||
            nextProps.selectedActionId !== this.props.selectedActionId ||
            nextProps.selectedMessageId !== this.props.selectedMessageId;
    }

    render({ mlDataActionIds, saveMlData, token, actions, selectedCheckpointId, selectedActionId, scrolledActionId, selectedMessageId, onSelect, actionsFilter, filterFields, onVerificationSelect, setSelectedCheckpoint, checkpointActions }: ListProps) {

        return (
            <div class="actions">
                <div class="actions__list">
                    <HeatmapScrollbar
                        selectedElements={actionsHeatmap(getActions(actions), selectedActionId)}
                        ref={ref => this.scrollbar = ref}>
                        {actions.map(action => (
                            <ActionTree
                                rootActionId={action.id}
                                action={action}
                                selectedActionsId={selectedActionId}
                                selectedMessageId={selectedMessageId}
                                selectedCheckpointId={selectedCheckpointId}
                                scrolledActionId={scrolledActionId}
                                actionSelectHandler={(action) => {
                                    if (action.status.status == 'FAILED' && !mlDataActionIds.has(action.id)) {
                                        fetchPredictions(token, saveMlData, action.id);
                                    }
                                    onSelect(action);
                                }}
                                verificationSelectHandler={ (messageId, actionId, status) => {
                                    if (status == 'FAILED' && !mlDataActionIds.has(actionId)) {
                                        fetchPredictions(token, saveMlData, actionId);
                                    }
                                    onVerificationSelect(messageId, actionId, status);

                                }}
                                actionsFilter={actionsFilter}
                                filterFields={filterFields}
                                checkpoints={checkpointActions}
                                checkpointSelectHandler={action => setSelectedCheckpoint(action)}
                                ref={ref => this.elements[action.id] = ref} />))}
                    </HeatmapScrollbar>
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
    checkpointActions: state.selected.checkpointActions,
    activeActionId: state.selected.activeActionId,
    token: state.machineLearning.token,
    mlDataActionIds: new Set<number>(state.machineLearning.predictionData.map((item) => { return item.actionId }))
}),
    dispatch => ({
        onSelect: (action: Action) => dispatch(selectAction(action)),
        saveMlData: (data: PredictionData[]) => dispatch(saveMlData(data)),
        onVerificationSelect: (messageId: number, actionId: number, status: StatusType) => dispatch(selectVerification(messageId, actionId, status)),
        setSelectedCheckpoint: (checkpointAction: Action) => dispatch(selectCheckpoint(checkpointAction))
    }),
    null,
    {
        withRef: true
    }
)(ActionsListBase);