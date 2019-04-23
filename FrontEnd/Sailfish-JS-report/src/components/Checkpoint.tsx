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

import { h } from "preact";
import "../styles/checkpoint.scss";
import { createSelector } from '../helpers/styleCreators';
import { connect } from "preact-redux";
import Message from "../models/Message";
import AppState from '../state/models/AppState';
import { isCheckpoint } from "../helpers/messageType";
import Action from '../models/Action';
import { selectCheckpoint } from '../actions/actionCreators';

interface CheckpointMessageOwnProps {
    message: Message;
}

interface CheckpointActionOwnProps {
    action: Action;
}

interface CheckpointStateProps {
    name: string;
    count: number;
    description?: string;
    isSelected: boolean;
}

interface CheckpointDispatchProps {
    clickHandler?: () => any;
}

interface CheckpointProps extends CheckpointStateProps, CheckpointDispatchProps {}

export const Checkpoint = ({ name, count, isSelected, clickHandler, description }: CheckpointProps) => {

    const rootClassName = createSelector(
        "checkpoint", 
        isSelected ? "selected" : ""
    );

    return (
        <div class={rootClassName}
            onClick={() => clickHandler && clickHandler()}>
            <div class="checkpoint-icon" />
            <div class="checkpoint-count">Checkpoint {count}</div>
            <div class="checkpoint-name">{name}</div>
            <div class="checkpoint-description">{description}</div>
        </div>
    )
}

export const CheckpointMessage = connect(
    (state: AppState, ownProps: CheckpointMessageOwnProps): CheckpointStateProps => ({
        name: ownProps.message.msgName,
        isSelected: ownProps.message.id === state.selected.checkpointMessageId,
        count : state.selected.testCase.messages.filter(isCheckpoint).indexOf(ownProps.message) + 1,
        description : ownProps.message.content["message"] ? ownProps.message.content["message"]["Description"] : ""
    }), 
    dispatch => ({})
)(Checkpoint);

export const CheckpointAction = connect(
    (state: AppState, ownProps: CheckpointActionOwnProps): CheckpointStateProps => ({
        name: ownProps.action.name,
        isSelected: ownProps.action.id === state.selected.checkpointActionId,
        count: state.selected.checkpointActions.indexOf(ownProps.action) + 1
    }),
    (dispatch, ownProps: CheckpointActionOwnProps): CheckpointDispatchProps => ({
        clickHandler: () => dispatch(selectCheckpoint(ownProps.action))
    })
)(Checkpoint);
