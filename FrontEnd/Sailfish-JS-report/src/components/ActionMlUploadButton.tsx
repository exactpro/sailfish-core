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
import { addSubmittedMlData, removeSubmittedMlData } from "../actions/actionCreators";
import { connect } from 'react-redux';
import { submitEntry, deleteEntry, EMPTY_MESSAGE_ID, } from '../helpers/machineLearning';
import Action from "../models/Action";

interface ActionMlUploadButtonProps {
    actionId: number;
    token: string;
    submittedData: SubmittedData[];
    actionMap: Map<number, Action>;
    addSubmittedMlData: (data: SubmittedData) => any;
    removeSubmittedMlData: (data: SubmittedData) => any;
}

export class ActionMlUploadButtonBase extends React.Component<ActionMlUploadButtonProps, {}> {
    render() {

        const isAvailable = this.props.token !== null
            && this.props.actionMap.get(this.props.actionId).status.status === "FAILED";

        const submittedWithThisAction = this.props.submittedData.filter((entry) => {
            return entry.actionId === this.props.actionId
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
                            deleteEntry(this.props.token, { actionId: this.props.actionId, messageId: entry.messageId }, this.props.removeSubmittedMlData);
                        });
                        e.stopPropagation();
                    }} />

                : <div className="ml-action__submit-icon active"
                    title="Submit ML data without cause message"
                    onClick={(e) => {
                        submitEntry(this.props.token, { actionId: this.props.actionId, messageId: EMPTY_MESSAGE_ID }, this.props.removeSubmittedMlData, this.props.addSubmittedMlData);
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
}

export const ActionMlUploadButton = connect(
    (state: AppState) => ({
        token: state.machineLearning.token,
        submittedData: state.machineLearning.submittedData,
        actionMap: state.selected.actionsMap
    }),
    (dispatch) => ({
        addSubmittedMlData: (data: SubmittedData) => dispatch(addSubmittedMlData(data)),
        removeSubmittedMlData: (data: SubmittedData) => dispatch(removeSubmittedMlData(data))
    })
)(ActionMlUploadButtonBase);
