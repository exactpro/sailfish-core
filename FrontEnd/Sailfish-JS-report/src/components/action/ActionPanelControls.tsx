/*******************************************************************************
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
 *  limitations under the License.
 ******************************************************************************/

import * as React from 'react';
import '../../styles/layout.scss'
import { connect } from "react-redux";
import CheckpointCarousel from "../CheckpointCarousel";
import AppState from "../../state/models/AppState";
import Action from "../../models/Action";
import { getCheckpointActions, getSelectedCheckpoint } from "../../selectors/actions";
import { selectCheckpointAction } from "../../actions/actionCreators";

interface StateProps {
    isCheckpointsEnabled: boolean;
    currentCheckpoint: Action;
}

interface DispatchProps {
    selectCheckpoint: (cp: Action) => void;
}

interface Props extends StateProps, DispatchProps {
}

function ActionPanelControlBase({ isCheckpointsEnabled, currentCheckpoint, selectCheckpoint }: Props) {
    return isCheckpointsEnabled ? (
        <div className="layout-control">
            <div className="layout-control__icon cp"
                 onClick={() => selectCheckpoint(currentCheckpoint)}
                 title="Scroll to current checkpoint"/>
            <div className="layout-control__title">Checkpoints</div>
            <CheckpointCarousel/>
        </div>
    ) : (
        <div className="layout-control disabled">
            <div className="layout-control__icon cp"/>
            <div className="layout-control__title">No Checkpoints</div>
        </div>
    )
}

const ActionPanelControl = connect(
    (state: AppState): StateProps => ({
        isCheckpointsEnabled: getCheckpointActions(state).length > 0,
        currentCheckpoint: getSelectedCheckpoint(state)
    }),
    (dispatch): DispatchProps => ({
        selectCheckpoint: cp => dispatch(selectCheckpointAction(cp))
    })
)(ActionPanelControlBase);

export default ActionPanelControl;
