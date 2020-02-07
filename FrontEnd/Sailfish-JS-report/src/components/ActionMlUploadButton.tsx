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
import '../styles/messages.scss';
import { SubmittedData } from '../models/MlServiceResponse'
import AppState from '../state/models/AppState';
import { connect } from 'react-redux';
import { mlSubmitEntry, mlDeleteEntry, EMPTY_MESSAGE_ID, } from '../thunks/machineLearning';
import Action from "../models/Action";
import { StatusType } from '../models/Status';
import { ThunkDispatch } from 'redux-thunk';
import StateAction from '../actions/stateActions';

interface OwnProps {
    actionId: number;
}

interface StateProps {
    token: string;
    submittedData: SubmittedData[];
    actionMap: Map<number, Action>;
}

interface DispatchProps {
    submitEntry: (data: SubmittedData) => any;
    deleteEntry: (data: SubmittedData) => any;
}

interface Props extends OwnProps, StateProps, DispatchProps {}

export const ActionMlUploadButtonBase = ({ actionId, token, submittedData, actionMap, submitEntry, deleteEntry }: Props) => {

    const isAvailable = token !== null
        && actionMap.get(actionId)?.status.status === StatusType.FAILED;

    const submittedWithThisAction = submittedData.filter((entry) => {
        return entry.actionId === actionId
    });

    const submittedMessagesCount = submittedWithThisAction.filter((entry) => {
        return entry.messageId !== EMPTY_MESSAGE_ID
    }).length;

    const isSubmitted = submittedWithThisAction.length > 0;

    if (isAvailable) {
        const mlButton = isSubmitted

            ? <div className="ml-action__submit-icon submitted"
                title="Revoke all ML data related to this action"
                onClick={(e) => {
                    submittedWithThisAction.forEach((entry) => {
                        deleteEntry({ actionId: actionId, messageId: entry.messageId });
                    });
                    e.stopPropagation();
                }} />

            : <div className="ml-action__submit-icon active"
                title="Submit ML data without cause message"
                onClick={(e) => {
                    submitEntry({ actionId: actionId, messageId: EMPTY_MESSAGE_ID });
                    e.stopPropagation();
                }} />;

        return (
            <div className="ml-action__submit" title="Unable to submit ML data">
                {mlButton}
                <div className={`ml-action__submit-counter ${isSubmitted ? "submitted" : "active"}`}>
                    {submittedMessagesCount}
                </div>
            </div>
        )
    }

    return null;
}

export const ActionMlUploadButton = connect(
    (state: AppState): StateProps => ({
        token: state.machineLearning.token,
        submittedData: state.machineLearning.submittedData,
        actionMap: state.selected.actionsMap
    }),
    (dispatch: ThunkDispatch<AppState, never, StateAction>): DispatchProps => ({
        submitEntry: (data: SubmittedData) => dispatch(mlSubmitEntry(data)),
        deleteEntry: (data: SubmittedData) => dispatch(mlDeleteEntry(data))
    })
)(ActionMlUploadButtonBase);
