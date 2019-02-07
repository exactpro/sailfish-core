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

import TestCase from "../models/TestCase";
import Action from '../models/Action';
import { StatusType } from "../models/Status";
import Report from '../models/Report';
import { Pane } from "../helpers/Pane";

export enum StateActionTypes {
    SET_REPORT = 'SET_REPORT', 
    NEXT_TEST_CASE = 'NEXT_TEST_CASE',
    PREV_TEST_CASE = 'PREV_TEST_CASE',
    SET_TEST_CASE_PATH = 'SET_TEST_CASE_PATH',
    SET_TEST_CASE = 'SET_TEST_CASE',
    RESET_TEST_CASE= 'RESET_TEST_CASE',
    SELECT_ACTION = 'SELECT_ACTION',
    SELECT_ACTION_BY_ID = 'SELECT_ACTION_BY_ID',
    SELECT_MESSAGES = 'SELECT_MESSAGES',
    SELECT_CHECKPOINT = 'SELECT_CHECKPOINT',
    SELECT_REJECTED_MESSAGE = 'SELECT_REJECTED_MESSAGE',
    SWITCH_ACTIONS_FILTER = 'SWITCH_ACTIONS_FILTER',
    SWITCH_FIELDS_FILTER = 'SWITCH_FIELDS_FILTER',
    SWITCH_SPLIT_MODE = 'SWITCH_SPLIT_MODE',
    SHOW_FILTER = 'SHOW_FILTER',
    SET_LEFT_PANE = 'SET_LEFT_PANE',
    SET_RIGHT_PANE = 'SET_RIGHT_PANE'
}

export interface SetReportStateAction {
    type: StateActionTypes.SET_REPORT;
    report: Report;
}

export interface SetTestCaseStateAction { 
    type: StateActionTypes.SET_TEST_CASE;
    testCase: TestCase;
}

export interface ResetTestCaseStateAction {
    type: StateActionTypes.RESET_TEST_CASE;
}

export interface ActionSelectStateAction {
    type: StateActionTypes.SELECT_ACTION;
    action: Action;
}

export interface ActionSelectByIdStateAction {
    type: StateActionTypes.SELECT_ACTION_BY_ID;
    actionId: number;
}

export interface MessagesSelectStateAction {
    type: StateActionTypes.SELECT_MESSAGES;
    messagesId: number[];
    status: StatusType;
}

export interface CheckpointSelectStateAction {
    type: StateActionTypes.SELECT_CHECKPOINT;
    checkpointId: number;
}

export interface RejectedMessageSelectStateAction {
    type: StateActionTypes.SELECT_REJECTED_MESSAGE;
    messageId: number;
}

export interface SwitchActionFilterStateAction {
    type: StateActionTypes.SWITCH_ACTIONS_FILTER;
    status: StatusType;
}

export interface SwitchFieldsFilterStateAction {
    type: StateActionTypes.SWITCH_FIELDS_FILTER;
    status: StatusType;
}

export interface NextTestCaseStateAction {
    type: StateActionTypes.NEXT_TEST_CASE;
}

export interface PrevTestCaseStateAction {
    type: StateActionTypes.PREV_TEST_CASE;
}

export interface SetTestCasePathStateAction {
    type: StateActionTypes.SET_TEST_CASE_PATH;
    testCasePath: string;
}

export interface SwitchSplitModeStateAction {
    type: StateActionTypes.SWITCH_SPLIT_MODE;
}

export interface ShowFilterStateAction {
    type: StateActionTypes.SHOW_FILTER;
}

export interface SetLeftPaneStateActions {
    type: StateActionTypes.SET_LEFT_PANE;
    pane: Pane;
}

export interface SetRightPaneStateAction {
    type: StateActionTypes.SET_RIGHT_PANE;
    pane: Pane;
}

export type StateActionType = SetReportStateAction |
    SetTestCaseStateAction | 
    ResetTestCaseStateAction |
    ActionSelectStateAction |
    ActionSelectByIdStateAction |
    MessagesSelectStateAction | 
    CheckpointSelectStateAction |
    RejectedMessageSelectStateAction |
    NextTestCaseStateAction |
    PrevTestCaseStateAction | 
    SetTestCaseStateAction | 
    SetTestCasePathStateAction | 
    SwitchSplitModeStateAction | 
    SwitchActionFilterStateAction | 
    SwitchFieldsFilterStateAction | 
    ShowFilterStateAction | 
    SetRightPaneStateAction | 
    SetLeftPaneStateActions;
