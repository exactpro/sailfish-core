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

import SelectedState from '../state/models/SelectedState';
import { initialSelectedState } from '../state/initial/initialSelectedState';
import StateActionType, { StateActionTypes } from '../actions/stateActions';
import { getScrolledId } from '../helpers/array';
import { generateActionsMap } from '../helpers/mapGenerator';
import { getActions } from '../helpers/action';
import getScrolledIndex from '../helpers/search/getScrolledIndex';
import searchReducer from "./searchReducer";
import { appendRawContent } from "../helpers/message";

export function selectedReducer(state: SelectedState = initialSelectedState, stateAction: StateActionType): SelectedState {
    switch (stateAction.type) {

        case StateActionTypes.SET_TEST_CASE: {
            return {
                ...state,
                testCase: {
                    ...stateAction.testCase,
                    actions: [],
                    messages: [],
                    logs: [],
                    status: stateAction.testCase.status || {
                        status: null,
                        cause: null,
                        description: null
                    }
                }
            }
        }

        case StateActionTypes.UPDATE_TEST_CASE: {
            if (state.testCase !== null && stateAction.testCase.order === state.testCase.order) {
                return {
                    ...state,
                    testCase: {
                        ...state.testCase,
                        ...stateAction.testCase,
                        status: stateAction.testCase.status || {
                            status: null,
                            cause: null,
                            description: null
                        }
                    }
                }
            }
            return state;
        }
    
        case StateActionTypes.RESET_TEST_CASE: {
            return {
                ...initialSelectedState
            }
        }

        case StateActionTypes.SELECT_ACTION: {

            // We must use Number object to handle situation when some message was selected by different actions 
            // and Messages list component can't understand that message was selected again, therefore scroll doesn't work.
            // Using reference comparison with Number objects, component can understand that message with the same id was selected again

            return {
                ...state,
                actionsId: [stateAction.action.id],
                selectedActionStatus: stateAction.action.status.status,
                messagesId: stateAction.action.relatedMessages,
                verificationId: initialSelectedState.verificationId,
                scrolledActionId: stateAction.shouldScrollIntoView ? new Number(stateAction.action.id) : initialSelectedState.scrolledActionId,
                scrolledMessageId: getScrolledId(stateAction.action.relatedMessages, +state.messagesId),
                activeActionId: stateAction.action.id,
                checkpointActionId: initialSelectedState.checkpointActionId,
                checkpointMessageId: initialSelectedState.checkpointMessageId,
                actionsScrollHintsIds: initialSelectedState.actionsScrollHintsIds,
                messagesScrollHintsIds: initialSelectedState.messagesScrollHintsIds,
            }
        }

        case StateActionTypes.SELECT_ACTION_BY_ID: {
            return {
                ...state,
                actionsId: [stateAction.actionId],
                selectedActionStatus: initialSelectedState.selectedActionStatus,
                scrolledActionId: new Number(stateAction.actionId),
                activeActionId: stateAction.actionId,
            }
        }

        case StateActionTypes.SELECT_MESSAGE: {

            // We must use Number object to handle situation when some action was selected by different messages 
            // and Actions list component can't understand that action was selected again, therefore scroll doesn't work.
            // Using reference comparison with Number objects, component can understand that action with the same id was selected again.
            
            const relatedActions = stateAction.message.relatedActions
                .filter(actionId => !stateAction.status || (state.actionsMap.get(actionId)?.status.status === stateAction.status));

            return {
                ...state,
                messagesId: [stateAction.message.id],
                selectedActionStatus: stateAction.status,
                actionsId: relatedActions,
                verificationId: stateAction.message.id,
                scrolledActionId: getScrolledId(relatedActions, +state.scrolledActionId),
                scrolledMessageId: stateAction.shouldScrollIntoView ? new Number(stateAction.message.id) : initialSelectedState.scrolledMessageId,
                activeActionId: relatedActions.length === 1 ? relatedActions[0] : initialSelectedState.activeActionId,
                checkpointActionId: initialSelectedState.checkpointActionId,
                checkpointMessageId: initialSelectedState.checkpointMessageId,
                actionsScrollHintsIds: initialSelectedState.actionsScrollHintsIds,
                messagesScrollHintsIds: initialSelectedState.messagesScrollHintsIds,
            }
        }

        case StateActionTypes.SELECT_VERIFICATION: {
            return {
                ...state,
                verificationId: stateAction.messageId,
                messagesId: [stateAction.messageId],
                selectedActionStatus: stateAction.status,
                actionsId: initialSelectedState.actionsId,
                scrolledMessageId: new Number(stateAction.messageId),
                activeActionId: stateAction.rootActionId,
                checkpointActionId: initialSelectedState.checkpointActionId,
                checkpointMessageId: initialSelectedState.checkpointMessageId,
                actionsScrollHintsIds: initialSelectedState.actionsScrollHintsIds,
                messagesScrollHintsIds: initialSelectedState.messagesScrollHintsIds,
            }
        }

        case StateActionTypes.SELECT_CHECKPOINT_ACTION: {
            const { relatedMessages, id } = stateAction.action;

            return {
                ...state,
                checkpointMessageId: relatedMessages[0] || null,
                scrolledMessageId: relatedMessages[0] != null ? new Number(relatedMessages[0]) : null,
                checkpointActionId: id,
                scrolledActionId: new Number(stateAction.action.id),
                activeActionId: initialSelectedState.activeActionId,
                actionsId:initialSelectedState.actionsId,
                messagesId:initialSelectedState.messagesId,
                verificationId: initialSelectedState.verificationId,
                actionsScrollHintsIds: initialSelectedState.actionsScrollHintsIds,
                messagesScrollHintsIds: initialSelectedState.messagesScrollHintsIds,
            }
        }

        case StateActionTypes.SELECT_CHECKPOINT_MESSAGE: {
            const { relatedActions, id } = stateAction.message;

            return {
                ...state,
                checkpointMessageId: id,
                scrolledMessageId: new Number(id),
                checkpointActionId: relatedActions[0] != null ? relatedActions[0] : null,
                scrolledActionId: relatedActions[0] != null ? new Number(relatedActions[0]) : null,
                messagesId: initialSelectedState.messagesId,
                activeActionId: initialSelectedState.activeActionId,
                actionsId: initialSelectedState.actionsId,
                verificationId: initialSelectedState.verificationId,
                actionsScrollHintsIds: initialSelectedState.actionsScrollHintsIds,
                messagesScrollHintsIds: initialSelectedState.messagesScrollHintsIds,
            }
        }

        case StateActionTypes.SELECT_REJECTED_MESSAGE: {
            return {
                ...state,
                rejectedMessageId: stateAction.messageId,
                scrolledMessageId: new Number(stateAction.messageId)
            }
        }

        case StateActionTypes.SET_SEARCH_RIGHT_PANEL_ENABLED:
        case StateActionTypes.SET_SEARCH_LEFT_PANEL_ENABLED:
        case StateActionTypes.SET_SEARCH_RESULTS:
        case StateActionTypes.SET_SEARCH_TOKENS: {
            return {
                ...state,
                search: searchReducer(state.search, stateAction)
            }
        }

        case StateActionTypes.CLEAR_SEARCH: {
            return {
                ...state,
                search: searchReducer(state.search, stateAction)
            }
        }

        case StateActionTypes.NEXT_SEARCH_RESULT: {
            if (state.search.resultsCount < 1) {
                return state;
            } 

            const targetIndex = (state.search.index + 1) % state.search.resultsCount,
                [
                    actionId = state.scrolledActionId,
                    msgId = state.scrolledMessageId,
                    logIndex = state.scrolledLogIndex
                ] = getScrolledIndex(state.search.results, targetIndex);

            return {
                ...state,
                search: searchReducer(state.search, stateAction),
                scrolledMessageId: msgId,
                scrolledActionId: actionId,
                scrolledLogIndex: logIndex
            }
        }

        case StateActionTypes.PREV_SEARCH_RESULT: { 
            if (state.search.resultsCount < 1) {
                return state;
            }
            
            const targetIndex = (state.search.resultsCount + state.search.index - 1) % state.search.resultsCount,
                [
                    actionId = state.scrolledActionId,
                    msgId = state.scrolledMessageId,
                    logIndex = state.scrolledLogIndex
                ] = getScrolledIndex(state.search.results, targetIndex);

            return {
                ...state,
                search: searchReducer(state.search, stateAction),
                scrolledMessageId: msgId,
                scrolledActionId: actionId,
                scrolledLogIndex: logIndex
            }
        }

        case StateActionTypes.SET_SHOULD_SCROLL_TO_SEARCH_ITEM: {
            return {
                ...state,
                search: searchReducer(state.search, stateAction)
            }
        }
        
        case StateActionTypes.SET_SELECTED_TESTCASE: {
            return  {
                ...state,
                selectedTestCaseId: stateAction.testCaseId
            }
        }

        case StateActionTypes.SELECT_KNOWN_BUG: {
            const actionsId = stateAction.status != null ? 
                stateAction.knownBug.relatedActionIds
                    .filter(id => state.actionsMap.get(id)?.status.status === stateAction.status) : 
                stateAction.knownBug.relatedActionIds;

            return {
                ...state,
                selectedActionStatus: stateAction.status,
                actionsId,
                scrolledActionId: getScrolledId(actionsId, +state.scrolledActionId),
                actionsScrollHintsIds: initialSelectedState.actionsScrollHintsIds,
                messagesScrollHintsIds: initialSelectedState.messagesScrollHintsIds,
            }
        }

        case StateActionTypes.ADD_TEST_CASE_ACTIONS: {
            if (state.testCase !== null && state.testCase.order === stateAction.testCaseOrder) {
                const actions = [...state.testCase.actions, ...stateAction.actions];

                return {
                    ...state,
                    actionsMap: generateActionsMap(getActions(actions)),
                    testCase: {
                        ...state.testCase,
                        actions
                    }
                }
            }

            return state;
        }

        case StateActionTypes.ADD_TEST_CASE_LOGS: {
            if (state.testCase !== null && state.testCase.order === stateAction.testCaseOrder) {
                return {
                    ...state,
                    testCase: {
                        ...state.testCase,
                        logs: [...state.testCase.logs, ...stateAction.logs]
                    }
                }
            }

            return state;
        }

        case StateActionTypes.ADD_TEST_CASE_MESSAGES: {
            if (state.testCase !== null && state.testCase.order === stateAction.testCaseOrder) {
                return {
                    ...state,
                    testCase: {
                        ...state.testCase,
                        messages: [
                            ...state.testCase.messages,
                            ...stateAction.messages.map(appendRawContent)
                        ],
                    }
                }
            }
            
            return state;
        }

        case StateActionTypes.SELECT_ACTIONS_SCROLL_HINT_IDS: {
            return {
                ...state,
                actionsScrollHintsIds: [stateAction.actionId]
            }
        }

        case StateActionTypes.SELECT_MESSAGES_SCROLL_HINT_IDS: {
            return {
                ...state,
                messagesScrollHintsIds: [stateAction.messageId]
            }
        }

        default: {
            return state;
        }
    }
}
