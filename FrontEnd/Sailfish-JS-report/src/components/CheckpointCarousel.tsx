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

import { connect } from 'react-redux';
import AppState from '../state/models/AppState';
import SelectionCarousel, { SelectionCarouselProps } from './SelectionCarousel';
import { selectCheckpointAction } from '../actions/actionCreators';
import Action from '../models/Action';
import { nextCyclicItemByIndex, prevCyclicItemByIndex } from '../helpers/array';
import { getCheckpointActions } from '../selectors/actions';

const CheckpointCarousel = connect(
    (state: AppState) => ({
        checkpointActions: getCheckpointActions(state),
        selectedCheckpointAction: state.selected.checkpointActionId,
        index: getCheckpointActions(state).findIndex(cp => cp.id === state.selected.checkpointActionId),
    }),
    dispatch => ({
        selectCheckpoint: (checkpoint: Action) => dispatch(selectCheckpointAction(checkpoint))
    }),
    ({ index, checkpointActions, selectedCheckpointAction, ...stateProps }, {selectCheckpoint}, ownProps): SelectionCarouselProps => ({
        ...stateProps,
        ...ownProps,
        currentIndex: index + 1,
        next: () => selectCheckpoint(nextCyclicItemByIndex(checkpointActions, index)),
        prev: () => selectCheckpoint(prevCyclicItemByIndex(checkpointActions, index)),
        itemsCount: checkpointActions.length,
        isEnabled:  checkpointActions.length > 0

    })
)(SelectionCarousel);

export default CheckpointCarousel;
