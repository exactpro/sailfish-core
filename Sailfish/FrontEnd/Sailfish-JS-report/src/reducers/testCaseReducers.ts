import { combineReducers } from 'redux';
import SelectedState from '../state/SelectedState';
import { initialSelectedState, initialTestCaseState } from '../state/initialStates';
import { StateActionType, StateActionTypes } from '../actions/stateActions';
import { StatusType } from '../models/Status';
import TestCaseState from '../state/TestCaseState';

function select(state: SelectedState = initialSelectedState, stateAction: StateActionType): SelectedState {
    switch (stateAction.type) {

        case StateActionTypes.SELECT_ACTION: {
            return {
                ...state,
                actionId: stateAction.action.id,
                status: stateAction.action.status.status,
                messagesId: stateAction.action.relatedMessages
            } 
        }

        case StateActionTypes.SELECT_MESSAGES: {
            return {
                ...state,
                messagesId: stateAction.messagesId,
                status: stateAction.status
            }
        }

        default: {
            return state;
        }
    }
}

function actionsFilter(state: StatusType[] = [], stateAction: StateActionType): StatusType[] {
    switch (stateAction.type) {

        case StateActionTypes.ADD_ACTION_FILTER: {
            return [
                ...state,
                stateAction.status
            ]
        }

        case StateActionTypes.REMOVE_ACTION_FILTER: {
            return state.filter(status => status != status);
        }

        default: {
            return state;
        }
    }
}

function testCase(state: TestCaseState = initialTestCaseState, stateAction: StateActionType): TestCaseState {
    switch (stateAction.type) {

        case StateActionTypes.UPDATE_TEST_CASE: {
            return {
                ...state,
                testCase: stateAction.testCase,
                selected: initialSelectedState
            }
        }

        default: {
            return state;
        }
    }
}

export const testCaseReducer = combineReducers({
    select,
    actionsFilter,
    testCase
});