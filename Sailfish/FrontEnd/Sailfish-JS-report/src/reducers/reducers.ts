import { initialSelectedState, initialAppState } from '../state/initialStates';
import { StateActionType, StateActionTypes } from '../actions/stateActions';
import AppState from '../state/AppState';

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

        case StateActionTypes.ADD_ACTION_FILTER: {
            return {
                ...state,
                actionsFilter: [ ...state.actionsFilter, stateAction.status]
            }
        }

        case StateActionTypes.REMOVE_ACTION_FILTER: {
            return {
                ...state,
                actionsFilter: state.actionsFilter.filter(status => status != status)
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
                currentTestCasePath: ""
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

        default: {
            return state
        }
    }
}