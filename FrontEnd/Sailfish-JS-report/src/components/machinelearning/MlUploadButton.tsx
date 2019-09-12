/*
 * ****************************************************************************
 *  Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ****************************************************************************
 */

import * as React from 'react';
import '../../styles/messages.scss';
import { SubmittedData } from '../../models/MlServiceResponse'
import AppState from '../../state/models/AppState';
import { connect } from 'react-redux';
import { mlSubmitEntry, mlDeleteEntry } from '../../thunks/machineLearning';
import Action from "../../models/Action";
import { StatusType } from '../../models/Status';
import { ThunkDispatch } from 'redux-thunk';
import StateAction from '../../actions/stateActions';
import { stopPropagationHandler } from '../../helpers/react';

interface OwnProps {
    messageId: number;
    show?: boolean;
}

interface StateProps {
    token: string;
    submittedData: SubmittedData[];
    activeActionId: number;
    actionMap: Map<number, Action>;
}

interface  DispatchProps {
    submitEntry: (data: SubmittedData) => any;
    deleteEntry: (data: SubmittedData) => any;
}

interface Props extends OwnProps, StateProps, DispatchProps { }

export const MlUploadButtonBase = ({ messageId, show, token, submittedData, activeActionId, actionMap, submitEntry, deleteEntry }: Props) => {

    const activeAction = actionMap.get(activeActionId);

    let isAvailable = token !== null
        && (show == null || show)
        && activeAction != null
        && activeAction.status.status === StatusType.FAILED;

    let isSubmitted = isAvailable && submittedData.some((entry) => {
        return entry.messageId === messageId
            && entry.actionId === activeActionId
    });

    // default one (message cannot be submitted or ml servie is unavailable)
    let mlButton = <div className="ml__submit-icon inactive" />;

    if (isAvailable) {
        mlButton = isSubmitted

            ? <div className="ml__submit-icon submitted"
                    title="Revoke ML data"
                    onClick={stopPropagationHandler(deleteEntry, { messageId, actionId: activeActionId })} />

            : <div className="ml__submit-icon active"
                    title="Submit ML data"
                    onClick={stopPropagationHandler(submitEntry, { messageId, actionId: activeActionId })} />
    }

    return (
        <div className="ml__submit" title="Unable to submit ML data">
            {mlButton}
        </div>
    )
}


export const MlUploadButton = connect(
    (state: AppState): StateProps => ({
        token: state.machineLearning.token,
        submittedData: state.machineLearning.submittedData,
        activeActionId: state.selected.activeActionId,
        actionMap: state.selected.actionsMap
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateAction>): DispatchProps => ({
        submitEntry: (data: SubmittedData) => dispatch(mlSubmitEntry(data)),
        deleteEntry: (data: SubmittedData) => dispatch(mlDeleteEntry(data))
    })
)(MlUploadButtonBase);
