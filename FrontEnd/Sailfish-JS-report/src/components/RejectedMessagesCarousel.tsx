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
import { isRejected } from '../helpers/message';
import { selectRejectedMessageId } from '../actions/actionCreators';
import { nextCyclicItemByIndex, prevCyclicItemByIndex } from '../helpers/array';

const RejectedMessagesCarousel = connect(
    (state: AppState) => {
        const rejectedMessages = state.selected.testCase.messages
            .filter(isRejected)
            .map(msg => msg.id);

        return {
            index: rejectedMessages.indexOf(state.selected.rejectedMessageId),
            itemsCount: rejectedMessages.length,
            isEnabled: rejectedMessages.length > 0,
            rejectedMessages
        }   
    },
    dispatch => ({
        selectRejectedMessage: (id: number) => dispatch(selectRejectedMessageId(id))
    }),
    ({ index, rejectedMessages, ...stateProps }, { selectRejectedMessage, ...dispatchProps }, ownProps): SelectionCarouselProps => ({
        ...stateProps,
        ...dispatchProps, 
        ...ownProps,
        currentIndex: index + 1,
        next: () => selectRejectedMessage(nextCyclicItemByIndex(rejectedMessages, index)),
        prev: () => selectRejectedMessage(prevCyclicItemByIndex(rejectedMessages, index))
    })
)(SelectionCarousel);

export default RejectedMessagesCarousel;
