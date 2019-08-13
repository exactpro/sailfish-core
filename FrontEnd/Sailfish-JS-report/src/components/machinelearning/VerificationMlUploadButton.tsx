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
import AppState from '../../state/models/AppState';
import {connect} from 'react-redux';
import {MlUploadButton} from "./MlUploadButton";

interface VerificationMlUploadButtonProps {
    activeActionId: number;
    targetActionId: number;
    targetMessageId: number;
}

export class VerificationMlUploadButtonBase extends React.Component<VerificationMlUploadButtonProps, {}> {
    render() {
        return <MlUploadButton messageId={this.props.targetMessageId} show={this.props.targetActionId === this.props.activeActionId}/>
    }
}

export const VerificationMlUploadButton = connect(
    (state: AppState) => ({
        activeActionId: state.selected.activeActionId
    }),
    () => ({})
)(VerificationMlUploadButtonBase);
