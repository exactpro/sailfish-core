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

import SelectedState from '../state/models/SelectedState';
import { initialSelectedState } from '../state/initial/initialSelectedState';
import { StateActionType, StateActionTypes } from '../actions/stateActions';
import { nextCyclicItem } from '../helpers/array';
import { getCheckpointActions } from '../helpers/checkpointFilter';
import { generateActionsMap } from '../helpers/mapGenerator';
import { getActions } from '../helpers/actionType';

export function selectedReducer(state: SelectedState = initialSelectedState, stateAction: StateActionType): SelectedState {
    switch (stateAction.type) {

        case StateActionTypes.SET_REPORT: {
            return {
                ...state,
                testCase: initialSelectedState.testCase
            }
        }

        case StateActionTypes.SET_TEST_CASE: {
            return {
                ...state,
                testCase: stateAction.testCase,
                checkpointActions: getCheckpointActions(stateAction.testCase.actions),
                actionsMap: generateActionsMap(getActions(stateAction.testCase.actions))
            }
        }

        case StateActionTypes.SET_TEST_CASE_PATH: {
            return {
                ...state,
                testCase: initialSelectedState.testCase
            }
        }
    
        case StateActionTypes.RESET_TEST_CASE: 
        case StateActionTypes.PREV_TEST_CASE:
        case StateActionTypes.NEXT_TEST_CASE: {
            return {
                ...initialSelectedState,
            }
        }

        case StateActionTypes.SELECT_ACTION: {

            // We must use Number object to handle situation when some message was selected by different actions 
            // and Messages list component can't understand that message was selected again, therefore scroll doesn't work.
            // Using reference comparison with Number objects, component can understand that message with the same id was selected again

            const scrolledMessageId = stateAction.action.relatedMessages.includes(+state.scrolledMessageId) ?
                nextCyclicItem(stateAction.action.relatedMessages, +state.scrolledMessageId) :
                stateAction.action.relatedMessages[0]

            return {
                ...state,
                actionsId: [stateAction.action.id],
                status: stateAction.action.status.status,
                messagesId: stateAction.action.relatedMessages,
                scrolledActionId: initialSelectedState.scrolledActionId,
                scrolledMessageId: new Number(scrolledMessageId)
            }
        }

        case StateActionTypes.SELECT_ACTION_BY_ID: {
            return {
                ...state,
                actionsId: [stateAction.actionId],
                status: initialSelectedState.status,
                scrolledActionId: new Number(stateAction.actionId)
            }
        }

        case StateActionTypes.SELECT_MESSAGE: {

            // We must use Number object to handle situation when some action was selected by different messages 
            // and Actions list component can't understand that action was selected again, therefore scroll doesn't work.
            // Using reference comparison with Number objects, component can understand that action with the same id was selected again.

            const relatedActions = stateAction.message.relatedActions
                .filter(actionId => !stateAction.status || (state.actionsMap.get(actionId) && state.actionsMap.get(actionId).status.status == stateAction.status));

            // re-select handling
            const scrolledAction = relatedActions.includes(+state.scrolledActionId) ?
                nextCyclicItem(relatedActions, +state.scrolledActionId) :
                relatedActions[0];

            return {
                ...state,
                messagesId: [stateAction.message.id],
                status: stateAction.status,
                actionsId: relatedActions,
                scrolledActionId: new Number(scrolledAction),
                scrolledMessageId: initialSelectedState.scrolledMessageId
            }
        }

        case StateActionTypes.SELECT_VERIFICATION: {
            return {
                ...state,
                messagesId: [stateAction.messageId],
                status: stateAction.status,
                actionsId: initialSelectedState.actionsId,
                scrolledMessageId: new Number(stateAction.messageId)
            }
        }

        case StateActionTypes.SELECT_CHECKPOINT: {

            const checkpointMessageId = stateAction.checkpointAction.relatedMessages[0] || null;

            return {
                ...state,
                checkpointMessageId: checkpointMessageId,
                scrolledMessageId: new Number(checkpointMessageId),
                checkpointActionId: stateAction.checkpointAction.id,
                scrolledActionId: new Number(stateAction.checkpointAction.id)
            }
        }

        case StateActionTypes.SELECT_REJECTED_MESSAGE: {
            return {
                ...state,
                rejectedMessageId: stateAction.messageId,
                scrolledMessageId: new Number(stateAction.messageId)
            }
        }

        default: {
            return state;
        }
    }
}
