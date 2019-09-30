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

import LiveUpdateState from '../state/models/LiveUpdateState';
import initialLiveUpdateState from '../state/initial/initialLiveUpdateState';
import StateAction, { StateActionTypes } from '../actions/stateActions';
import Message from '../models/Message';
import { isAction } from '../models/Action';

export function liveUpdateReducer(state: LiveUpdateState = initialLiveUpdateState, stateAction: StateAction): LiveUpdateState {
    switch (stateAction.type) {

        case StateActionTypes.UPDATE_LIVE_TEST_CASE: {
            return {
                ...state,
                testCase: stateAction.testCase,
                actions: [],
                messages: []
            };
        }

        case StateActionTypes.UPDATE_LIVE_ACTIONS: {
            // get ids of updated Actions
            const updatedActionsIds = stateAction.actions
                .filter(isAction)
                .map(action => action.id);

            // we need filter out Actions that was updated
            const filtredActions = state.actions.
                filter(node => isAction(node) ? !updatedActionsIds.includes(node.id) : false);

            return {
                ...state,
                actions: [...filtredActions, ...stateAction.actions]
            }
        }

        case StateActionTypes.UPDATE_LIVE_MESSAGES: {
            const updatedMessagesIds = stateAction.messages
                .map(message => message.id);

            const filtredMessages = state.messages
                .filter(message => !updatedMessagesIds.includes(message.id));

            return {
                ...state,
                messages: [...filtredMessages, ...stateAction.messages]
            }
        }

        default: {
            return state;
        }
    }
}
