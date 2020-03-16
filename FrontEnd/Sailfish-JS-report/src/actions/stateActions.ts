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

import * as actions from './actionCreators';

export enum StateActionTypes {
    // set test case and report
    SET_REPORT = 'SET_REPORT', 
    SET_TEST_CASE = 'SET_TEST_CASE',
    RESET_TEST_CASE = 'RESET_TEST_CASE',
    UPDATE_TEST_CASE = 'UPDATE_TEST_CASE',

    // add entities
    ADD_TEST_CASE_ACTIONS = 'ADD_TEST_CASE_ACTIONS',
    ADD_TEST_CASE_MESSAGES = 'ADD_TEST_CASE_MESSAGES',
    ADD_TEST_CASE_LOGS = 'ADD_TEST_CASE_LOGS',

    // select entities
    SELECT_ACTION = 'SELECT_ACTION',
    SELECT_ACTION_BY_ID = 'SELECT_ACTION_BY_ID',
    SELECT_MESSAGE = 'SELECT_MESSAGE',
    SELECT_VERIFICATION = 'SELECT_VERIFICATION',
    SELECT_CHECKPOINT_ACTION = 'SELECT_CHECKPOINT_ACTION',
    SELECT_CHECKPOINT_MESSAGE = 'SELECT_CHECKPOINT_MESSAGE',
    SELECT_REJECTED_MESSAGE = 'SELECT_REJECTED_MESSAGE',
    SET_SELECTED_TESTCASE = 'SET_SELECTED_TESTCASE',
    SELECT_KNOWN_BUG = 'SELECT_KNOWN_BUG',
    SELECT_ACTIONS_SCROLL_HINT_IDS = 'SELECT_ACTIONS_SCROLL_HINT_IDS',
    SELECT_MESSAGES_SCROLL_HINT_IDS = 'SELECT_MESSAGES_SCROLL_HINT_IDS',
    
    // search
    SET_SEARCH_TOKENS = 'SET_SEARCH_TOKENS',
    SET_SEARCH_RESULTS = 'SET_SEARCH_RESULTS',
    NEXT_SEARCH_RESULT = 'NEXT_SEARCH_RESULT',
    PREV_SEARCH_RESULT = 'PREV_SEARCH_RESULT',
    SET_SHOULD_SCROLL_TO_SEARCH_ITEM = 'SET_SHOULD_SCROLL_TO_SEARCH_ITEM',
    SET_SEARCH_LEFT_PANEL_ENABLED = 'SET_SEARCH_LEFT_PANEL_ENABLED',
    SET_SEARCH_RIGHT_PANEL_ENABLED = 'SET_SEARCH_RIGHT_PANEL_ENABLED',
    CLEAR_SEARCH = 'CLEAR_SEARCH',

    //filter
    SET_FILTER_RESULTS = 'SET_FILTER_RESULTS',
    SET_FILTER_CONFIG = 'SET_FILTER_CONFIG',
    SET_FILTER_IS_TRANSPARENT = 'SET_FILTER_IS_TRANSPARENT',
    SET_FILTER_IS_HIGHLIGHTED = 'SET_FILTER_IS_HIGHLIGHTED',
    RESET_FILTER = 'RESET_FILTER',
    
    // view
    SET_ADMIN_MSG_ENABLED = 'SET_ADMIN_MSG_ENABLED',
    SET_LEFT_PANE = 'SET_LEFT_PANE',
    SET_RIGHT_PANE = 'SET_RIGHT_PANE',
    SET_PANEL_AREA = 'SET_PANEL_AREA',
    UGLIFY_ALL_MESSAGES = 'UGLIFY_ALL_MESSAGES',
    SET_IS_LOADING = 'SET_IS_LOADING',
    TOGGLE_MESSAGE_BEAUTIFIER = 'TOGGLE_MESSAGE_BEAUTIFIER',
    SET_IS_CONNECTION_ERROR = 'SET_IS_CONNECTION_ERROR',

    // machinelearning
    SET_ML_TOKEN = 'SET_ML_TOKEN',
    SET_SUBMITTED_ML_DATA = 'SET_SUBMITTED_ML_DATA',
    ADD_SUBMITTED_ML_DATA = 'ADD_SUBMITTED_ML_DATA',
    REMOVE_SUBMITTED_ML_DATA = 'REMOVE_SUBMITTED_ML_DATA',
    SAVE_ML_DATA = 'SAVE_ML_DATA',
    TOGGLE_PREDICTIONS = 'TOGGLE_PREDICTIONS'
}

// How it works:
// https://habr.com/ru/company/alfa/blog/452620/

// This type helper returns union of all types in generic type
type InferValueTypes<T> = T extends { [key: string]: infer U }
    ? U
    : never;

// ReturnType is used here to extract all function's return types from every action creator
type StateAction = ReturnType<InferValueTypes<typeof actions>>;

export function isStateAction(action: any): action is StateAction {
    return action && typeof action.type === 'string' && action.type in StateActionTypes;
}

export default StateAction;
