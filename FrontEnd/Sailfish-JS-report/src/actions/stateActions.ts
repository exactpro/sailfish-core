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

import * as actions from './actionCreators';

export enum StateActionTypes {
    SET_REPORT = 'SET_REPORT', 
    SET_TEST_CASE = 'SET_TEST_CASE',
    RESET_TEST_CASE = 'RESET_TEST_CASE',
    SELECT_ACTION = 'SELECT_ACTION',
    SELECT_ACTION_BY_ID = 'SELECT_ACTION_BY_ID',
    SELECT_MESSAGE = 'SELECT_MESSAGE',
    SELECT_VERIFICATION = 'SELECT_VERIFICATION',
    SELECT_CHECKPOINT = 'SELECT_CHECKPOINT',
    SELECT_REJECTED_MESSAGE = 'SELECT_REJECTED_MESSAGE',
    SET_ADMIN_MSG_ENABLED = 'SET_ADMIN_MSG_ENABLED',
    SWITCH_ACTIONS_FILTER = 'SWITCH_ACTIONS_FILTER',
    SWITCH_FIELDS_FILTER = 'SWITCH_FIELDS_FILTER',
    SET_SEARCH_STRING = 'SET_SEARCH_STRING',
    NEXT_SEARCH_RESULT = 'NEXT_SEARCH_RESULT',
    PREV_SEARCH_RESULT = 'PREV_SEARCH_RESULT',
    SET_LEFT_PANE = 'SET_LEFT_PANE',
    SET_RIGHT_PANE = 'SET_RIGHT_PANE',
    SET_ML_TOKEN = 'SET_ML_TOKEN',
    SET_SUBMITTED_ML_DATA = "SET_SUBMITTED_ML_DATA",
    ADD_SUBMITTED_ML_DATA = "ADD_SUBMITTED_ML_DATA",
    REMOVE_SUBMITTED_ML_DATA = "REMOVE_SUBMITTED_ML_DATA",
    SET_IS_LOADING = 'SET_IS_LOADING'
}

// How it works:
// https://habr.com/ru/company/alfa/blog/452620/

// This type helper returns union of all types in generic type
type InferValueTypes<T> = T extends { [key: string]: infer U }
    ? U
    : never;

// ReturnType is used here to extract all function's return types from every action creator
type StateAction = ReturnType<InferValueTypes<typeof actions>>;

export default StateAction;
