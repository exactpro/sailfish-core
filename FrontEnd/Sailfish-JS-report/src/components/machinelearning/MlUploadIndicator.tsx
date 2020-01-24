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
import TestCase from '../../models/TestCase';
import { ActionNode, isAction } from '../../models/Action';
import { StatusType } from '../../models/Status';

interface MlUploadIndicatorProps {
    submittedData: SubmittedData[];
    testCase: TestCase;
    token: string;
}

export class MlUploadIndicatorBase extends React.Component<MlUploadIndicatorProps, {}> {
    render() {
        const failedActionIds: number[] = [];

        function addSubActions(action: ActionNode) {
            if (isAction(action) && action.status.status === StatusType.FAILED) {
                failedActionIds.push(action.id);
                action.subNodes.forEach((item) => { addSubActions(item) });
            }
        }

        this.props.testCase.actions.forEach((item) => { addSubActions(item) });

        const mlEnabled = this.props.token != null;

        const submittedActionIds = new Set(this.props.submittedData
            .filter((item) => { return failedActionIds.includes(item.actionId) })
            .map((item) => { return item.actionId }));

        if (!mlEnabled) {
            return (
                <div className="ml__submit-indicator">
                    <p className="ml__submit-indicator-text unavailable">ML unavailable</p>
                </div>
            )
        }

        if (submittedActionIds.size === failedActionIds.length && failedActionIds.length > 0) {
            return (
                <div className="ml__submit-indicator" >
                    <p className="ml__submit-indicator-text submitted">Submitted {submittedActionIds.size} of {failedActionIds.length}</p>
                </div>
            )
        }

        if (submittedActionIds.size > 0) {
            return (
                <div className="ml__submit-indicator">
                    <p className="ml__submit-indicator-text ready">Submitted {submittedActionIds.size} of {failedActionIds.length}</p>
                </div>
            )
        }

        if (failedActionIds.length > 0 && this.props.testCase.messages.length > 0) {
            return (
                <div className="ml__submit-indicator">
                    <p className="ml__submit-indicator-text ready">Ready to submit</p>
                </div>
            )
        }

        if (failedActionIds.length === 0 || this.props.testCase.messages.length === 0) {
            return (
                <div className="ml__submit-indicator">
                    <p className="ml__submit-indicator-text not-required">Nothing to submit</p>
                </div>
            )
        }
    }
}

export const MlUploadIndicator = connect(
    (state: AppState) => ({
        token: state.machineLearning.token,
        submittedData: state.machineLearning.submittedData,
        testCase: state.selected.testCase,
    })
)(MlUploadIndicatorBase);
