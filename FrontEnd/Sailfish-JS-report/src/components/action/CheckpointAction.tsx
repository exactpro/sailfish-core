/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
import Action from "../../models/Action";
import Checkpoint, { CheckpointStateProps, CheckpointDispatchProps } from "../Checkpoint";
import { getActionCheckpointName } from '../../helpers/action';
import { selectCheckpointAction } from "../../actions/actionCreators";
import { getCheckpointActions } from '../../selectors/actions';

interface OwnProps {
    action: Action;
}

const CheckpointAction = connect(
    (state: AppState, ownProps: OwnProps): CheckpointStateProps => ({
        name: getActionCheckpointName(ownProps.action),
        isSelected: ownProps.action.id === state.selected.checkpointActionId,
        index: getCheckpointActions(state).indexOf(ownProps.action) + 1,
        description: ownProps.action.description
    }),
    (dispatch, ownProps: OwnProps): CheckpointDispatchProps => ({
        clickHandler: () => dispatch(selectCheckpointAction(ownProps.action))
    })
)(Checkpoint);

export default CheckpointAction;
