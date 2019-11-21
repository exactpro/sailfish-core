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
import StateActionType, { StateActionTypes } from '../actions/stateActions';
import { nextCyclicItem, getScrolledId } from '../helpers/array';
import { getCheckpointActions } from '../helpers/checkpointFilter';
import { generateActionsMap } from '../helpers/mapGenerator';
import { getActions, removeNonexistingRelatedMessages } from '../helpers/action';
import SearchResult from '../helpers/search/SearchResult';
import getScrolledIndex from '../helpers/search/getScrolledIndex';
import { liveUpdateReducer } from './liveUpdateReduceer';

export function selectedReducer(state: SelectedState = initialSelectedState, stateAction: StateActionType): SelectedState {
    switch (stateAction.type) {

        case StateActionTypes.SET_TEST_CASE: {
            const messagesIds = stateAction.testCase.messages.map(message => message.id),
                actions = stateAction.testCase.actions.map(action => removeNonexistingRelatedMessages(action, messagesIds));

            return {
                ...state,
                checkpointActions: getCheckpointActions(actions),
                actionsMap: generateActionsMap(getActions(actions)),
                testCase: {
                    ...stateAction.testCase,
                    actions
                },
                searchString: initialSelectedState.searchString,
                searchResults: initialSelectedState.searchResults,
                searchIndex: initialSelectedState.searchIndex,
                searchResultsCount: initialSelectedState.searchResultsCount
            }
        }
    
        case StateActionTypes.RESET_TEST_CASE: {
            return {
                ...initialSelectedState,
                live: state.live
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
                scrolledActionId: initialSelectedState.scrolledActionId,
                scrolledMessageId: getScrolledId(stateAction.action.relatedMessages, +state.messagesId),
                activeActionId: stateAction.action.id
            }
        }

        case StateActionTypes.SELECT_ACTION_BY_ID: {
            return {
                ...state,
                actionsId: [stateAction.actionId],
                selectedActionStatus: initialSelectedState.selectedActionStatus,
                scrolledActionId: new Number(stateAction.actionId),
                activeActionId: stateAction.actionId
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
                scrolledMessageId: initialSelectedState.scrolledMessageId,
                activeActionId: relatedActions.length === 1 ? relatedActions[0] : initialSelectedState.activeActionId
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
                activeActionId: stateAction.rootActionId
            }
        }

        case StateActionTypes.SELECT_CHECKPOINT_ACTION: {
            const { relatedMessages, id } = stateAction.action;

            return {
                ...state,
                checkpointMessageId: relatedMessages[0] || null,
                scrolledMessageId: relatedMessages[0] != null ? new Number(relatedMessages[0]) : null,
                checkpointActionId: id,
                scrolledActionId: new Number(stateAction.action.id)
            }
        }

        case StateActionTypes.SELECT_CHECKPOINT_MESSAGE: {
            const { relatedActions, id } = stateAction.message;

            return {
                ...state,
                checkpointMessageId: id,
                scrolledMessageId: new Number(id),
                checkpointActionId: relatedActions[0] || null,
                scrolledActionId: relatedActions[0] != null ? new Number(relatedActions[0]) : null
            }
        }

        case StateActionTypes.SELECT_REJECTED_MESSAGE: {
            return {
                ...state,
                rejectedMessageId: stateAction.messageId,
                scrolledMessageId: new Number(stateAction.messageId)
            }
        }

        case StateActionTypes.SET_SEARCH_STRING: {
            return {
                ...state,
                searchString: stateAction.searchString,
                searchResultsCount: 0,
                searchIndex: null
            }
        }

        case StateActionTypes.SET_SEARCH_RESULTS: {
            const { searchResults } = stateAction, 
                searchResultsCount = searchResults.sum(),  
                searchIndex = searchResultsCount > 0 ? 0 : null,
                [actionId = state.scrolledActionId, msgId = state.scrolledMessageId] = getScrolledIndex(searchResults, searchIndex);

            return {
                ...state,
                searchResults,
                searchIndex,
                searchResultsCount,
                scrolledActionId: actionId,
                scrolledMessageId: msgId,
                shouldScrollToSearchItem: true
            }
        }

        case StateActionTypes.CLEAR_SEARCH: {
            return {
                ...state,
                searchResults: new SearchResult(),
                searchIndex: null,
                searchString: '',
                searchResultsCount: null
            }
        }

        case StateActionTypes.NEXT_SEARCH_RESULT: {
            if (state.searchResultsCount < 1) {
                return state;
            } 

            const targetIndex = (state.searchIndex + 1) % state.searchResultsCount,
                [actionId = state.scrolledActionId, msgId = state.scrolledMessageId] = getScrolledIndex(state.searchResults, targetIndex);

            return {
                ...state,
                searchIndex: targetIndex,
                scrolledMessageId: msgId,
                scrolledActionId: actionId,
                shouldScrollToSearchItem: true
            }
        }

        case StateActionTypes.PREV_SEARCH_RESULT: { 
            if (state.searchResultsCount < 1) {
                return state;
            }
            
            const targetIndex = (state.searchResultsCount + state.searchIndex - 1) % state.searchResultsCount,
                [actionId = state.scrolledActionId, msgId = state.scrolledMessageId] = getScrolledIndex(state.searchResults, targetIndex);

            return {
                ...state,
                searchIndex: targetIndex,
                scrolledMessageId: msgId,
                scrolledActionId: actionId,
                shouldScrollToSearchItem: true
            }
        }

        case StateActionTypes.SET_SHOULD_SCROLL_TO_SEARCH_ITEM: {
            return {
                ...state,
                shouldScrollToSearchItem: stateAction.isNeedsScroll 
            }
        }
        
        case StateActionTypes.SET_SELECTED_TESTCASE: {
            return  {
                ...state,
                selectedTestCaseId: stateAction.testCaseId
            }
        }

        case StateActionTypes.SELECT_LIVE_TESTCASE: {
            return {
                ...state,
                testCase: {
                    ...state.live.testCase,
                    actionNodeType: 'testCase',
                    actions: state.live.actions,
                    messages: state.live.messages,
                    bugs: [],
                    logs: [],
                    finishTime: null,
                    status: {
                        status: null,
                        cause: null,
                        description: null
                    }
                }
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
                scrolledActionId: getScrolledId(actionsId, +state.scrolledActionId)
            }
        }

        case StateActionTypes.UPDATE_LIVE_TEST_CASE: {
            return {
                ...state,
                live:  liveUpdateReducer(state.live, stateAction)
            }
        }

        case StateActionTypes.UPDATE_LIVE_ACTIONS: {
            const nextLiveState = liveUpdateReducer(state.live, stateAction);

            if (state.testCase != null && state.testCase.hash === nextLiveState.testCase.hash) {
                return {
                    ...state,
                    live: nextLiveState,
                    testCase: {
                        ...state.testCase,
                        actions: nextLiveState.actions
                    }
                }
            }

            return {
                ...state,
                live: nextLiveState
            }
        }

        case StateActionTypes.UPDATE_LIVE_MESSAGES: {
            const nextLiveState = liveUpdateReducer(state.live, stateAction);

            if (state.testCase != null && state.testCase.hash === nextLiveState.testCase.hash) {

                return {
                    ...state,
                    live: nextLiveState,
                    testCase: {
                        ...state.testCase,
                        messages: nextLiveState.messages
                    }
                }
            }

            return {
                ...state,
                live: nextLiveState
            }
        }

        default: {
            return state;
        }
    }
}
