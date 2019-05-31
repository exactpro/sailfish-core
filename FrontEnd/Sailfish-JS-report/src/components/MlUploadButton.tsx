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
import '../styles/messages.scss';
import { SubmittedData } from '../models/MlServiceResponse'
import AppState from '../state/models/AppState';
import { addSubmittedMlData, removeSubmittedMlData } from "../actions/actionCreators";
import { connect } from 'preact-redux';
import { submitEntry, deleteEntry } from '../helpers/machineLearning';
import Action from "../models/Action";

interface MessageMlUploadButtonProps {
    messageId: number;
    show?: boolean;
    token: string;
    submittedData: SubmittedData[];
    activeActionId: number;
    actionMap: Map<number, Action>;
    addSubmittedMlData: (data: SubmittedData) => any;
    removeSubmittedMlData: (data: SubmittedData) => any;
}

export class MessageMlUploadButtonBase extends Component<MessageMlUploadButtonProps, {}> {

    render({ submittedData, messageId, token, activeActionId, actionMap, show }: MessageMlUploadButtonProps) {

        let isAvailable = token !== null
            && (show == null || show)
            && activeActionId != null
            && actionMap.get(activeActionId).status.status === "FAILED";

        let isSubmitted = isAvailable && submittedData.some((entry) => {
            return entry.messageId === messageId
                && entry.actionId === activeActionId
        });

        // default one (message cannot be submitted or ml servie is unavailable)
        let mlButton = <div class="ml__submit-icon inactive" />;

        if (isAvailable) {
            mlButton = isSubmitted

                ? <div class="ml__submit-icon submitted"
                    title="Revoke ML data"
                    onClick={(e) => {
                        deleteEntry(token, { messageId: messageId, actionId: activeActionId }, this.props.removeSubmittedMlData);
                        e.cancelBubble = true;
                    }} />

                : <div class="ml__submit-icon active"
                    title="Submit ML data"
                    onClick={(e) => {
                        submitEntry(token, { messageId: messageId, actionId: activeActionId }, this.props.removeSubmittedMlData, this.props.addSubmittedMlData);
                        e.cancelBubble = true;
                    }} />
        }

        return (
            <div class="ml__submit" title="Unable to submit ML data">
                {mlButton}
            </div>
        )
    }
}

export const MessageMlUploadButton = connect(
    (state: AppState) => ({
        token: state.machineLearning.token,
        submittedData: state.machineLearning.submittedData,
        activeActionId: state.selected.activeActionId,
        actionMap: state.selected.actionsMap
    }),
    (dispatch) => ({
        addSubmittedMlData: (data: SubmittedData) => dispatch(addSubmittedMlData(data)),
        removeSubmittedMlData: (data: SubmittedData) => dispatch(removeSubmittedMlData(data))
    })
)(MessageMlUploadButtonBase);
