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

import { initialSelectedState, initialAppState } from '../state/initialStates';
import { StateActionType, StateActionTypes } from '../actions/stateActions';
import AppState from '../state/AppState';
import { Pane } from '../helpers/Pane';

export function appReducer(state: AppState = initialAppState, stateAction: StateActionType): AppState {
    switch (stateAction.type) {

        case StateActionTypes.SET_REPORT: {
            return {
                ...state,
                report: stateAction.report,
                testCase: null,
                currentTestCasePath: ""
            }
        }

        case StateActionTypes.SELECT_ACTION: {
            return {
                ...state,
                selected: {
                    ...state.selected,
                    actionId: stateAction.action.id,
                    status: stateAction.action.status.status,
                    messagesId: stateAction.action.relatedMessages
                }
            } 
        }

        case StateActionTypes.SELECT_MESSAGES: {
            return {
                ...state,
                selected: {
                    ...state.selected,
                    messagesId: stateAction.messagesId,
                    status: stateAction.status,
                    actionId: null
                }
            }
        }

        case StateActionTypes.SELECT_CHECKPOINT: {
            return {
                ...state,
                selected: {
                    ...state.selected,
                    checkpointMessageId: stateAction.checkpointId
                }
            }
        }

        case StateActionTypes.SELECT_REJECTED_MESSAGE: {
            return {
                ...state,
                selected: {
                    ...state.selected,
                    rejectedMessageId: stateAction.messageId
                }
            }
        }

        case StateActionTypes.SWITCH_ACTIONS_FILTER: {
            if (state.actionsFilter.includes(stateAction.status)) {
                return {
                    ...state,
                    actionsFilter: state.actionsFilter.filter(status => status != stateAction.status)
                }
            }

            return {
                ...state,
                actionsFilter: [ ...state.actionsFilter, stateAction.status]
            }
        }

        case StateActionTypes.SWITCH_FIELDS_FILTER: {
            if (state.fieldsFilter.includes(stateAction.status)) {
                return {
                    ...state,
                    fieldsFilter: state.fieldsFilter.filter(status => status != stateAction.status)
                }
            }

            return {
                ...state,
                fieldsFilter: [ ...state.fieldsFilter, stateAction.status ]
            }
        }

        case StateActionTypes.SET_TEST_CASE: {
            return {
                ...state,
                testCase: stateAction.testCase,
                selected: initialSelectedState
            }
        }

        case StateActionTypes.RESET_TEST_CASE: {
            return {
                ...state,
                testCase: null,
                currentTestCasePath: "",
                selected: initialSelectedState
            }
        }

        case StateActionTypes.NEXT_TEST_CASE: {
            const nextTestCaseIndex = state.report.testCaseLinks.findIndex(link => link === state.currentTestCasePath) + 1;

            return {
                ...state,
                testCase: null,
                currentTestCasePath: state.report.testCaseLinks[nextTestCaseIndex] || state.report.testCaseLinks[0]
            }
        }

        case StateActionTypes.PREV_TEST_CASE: {
            const prevTestCaseIndex = state.report.testCaseLinks.findIndex(link => link === state.currentTestCasePath) - 1;

            return {
                ...state,
                testCase: null,
                currentTestCasePath: state.report.testCaseLinks[prevTestCaseIndex] || 
                    state.report.testCaseLinks[state.report.testCaseLinks.length - 1]
            }
        }

        case StateActionTypes.SET_TEST_CASE_PATH: {
            return {
                ...state,
                testCase: null,
                currentTestCasePath: stateAction.testCasePath
            }
        }

        case StateActionTypes.SWITCH_SPLIT_MODE: {
            if (state.splitMode) {
                return {
                    ...state,
                    splitMode: false
                }
            }

            return {
                ...state,
                splitMode: true,
                leftPane: Pane.Actions
            }
        }

        case StateActionTypes.SHOW_FILTER: {
            return {
                ...state,
                showFilter: !state.showFilter
            }
        }

        case StateActionTypes.SET_LEFT_PANE: {
            return {
                ...state,
                leftPane: stateAction.pane
            }
        }

        case StateActionTypes.SET_RIGHT_PANE: {
            return {
                ...state,
                rightPane: stateAction.pane
            }
        }

        default: {
            return state
        }
    }
}