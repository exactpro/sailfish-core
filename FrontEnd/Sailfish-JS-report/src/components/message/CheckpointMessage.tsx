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

import { connect } from "react-redux";
import AppState from "../../state/models/AppState";
import Checkpoint, { CheckpointStateProps, CheckpointDispatchProps } from "../Checkpoint";
import Message from "../../models/Message";
import { isCheckpointMessage } from "../../helpers/message";
import { selectCheckpointMessage } from "../../actions/actionCreators";

interface OwnProps {
    message: Message;
}

const CheckpointMessage = connect(
    (state: AppState, ownProps: OwnProps): CheckpointStateProps => ({
        name: ownProps.message.msgName,
        isSelected: ownProps.message.id === state.selected.checkpointMessageId,
        index : state.selected.testCase.messages.filter(isCheckpointMessage).indexOf(ownProps.message) + 1,
        description : ownProps.message.content["message"] ? ownProps.message.content["message"]["Description"] : ""
    }), 
    (dispatch, ownProps: OwnProps): CheckpointDispatchProps => ({
        clickHandler: () => dispatch(selectCheckpointMessage(ownProps.message))
    })
)(Checkpoint);

export default CheckpointMessage;
