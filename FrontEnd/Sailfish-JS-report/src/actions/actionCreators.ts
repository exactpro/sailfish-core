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
import {
    SetTestCaseStateAction,
    StateActionTypes,
    ActionSelectStateAction,
    MessagesSelectStateAction,
    NextTestCaseStateAction,
    PrevTestCaseStateAction,
    SetTestCasePathStateAction,
    SetReportStateAction,
    ResetTestCaseStateAction,
    SwitchSplitModeStateAction,
    SwitchActionFilterStateAction,
    SwitchFieldsFilterStateAction,
    ShowFilterStateAction,
    SetLeftPaneStateActions,
    SetRightPaneStateAction,
    CheckpointSelectStateAction,
    RejectedMessageSelectStateAction,
    ActionSelectByIdStateAction
} from "./stateActions";
import Action from '../models/Action';
import { StatusType } from "../models/Status";
import Report from "../models/Report";
import { Pane } from "../helpers/Pane";

export const setReport = (report: Report): SetReportStateAction => ({
    type: StateActionTypes.SET_REPORT,
    report: report
})

export const setTestCase = (testCase: TestCase): SetTestCaseStateAction => ({
    type: StateActionTypes.SET_TEST_CASE,
    testCase: testCase
})

export const resetTestCase = (): ResetTestCaseStateAction => ({
    type: StateActionTypes.RESET_TEST_CASE
})

export const selectAction = (action: Action): ActionSelectStateAction => ({
    type: StateActionTypes.SELECT_ACTION,
    action: action
})

export const selectActionById = (actionId: number): ActionSelectByIdStateAction => ({
    type: StateActionTypes.SELECT_ACTION_BY_ID,
    actionId: actionId
})

export const selectMessages = (messages: number[], status: StatusType = 'NA'): MessagesSelectStateAction => ({
    type: StateActionTypes.SELECT_MESSAGES,
    messagesId: messages,
    status: status
})

export const selectCheckpoint = (checkpointAction: Action): CheckpointSelectStateAction => ({
    type: StateActionTypes.SELECT_CHECKPOINT,
    checkpointAction: checkpointAction
})

export const selectRejectedMessageId = (messageId: number): RejectedMessageSelectStateAction => ({
    type: StateActionTypes.SELECT_REJECTED_MESSAGE,
    messageId: messageId
})

export const switchActionsFilter = (status: StatusType): SwitchActionFilterStateAction => ({
    type: StateActionTypes.SWITCH_ACTIONS_FILTER,
    status: status
})

export const switchFieldsFilter = (status: StatusType): SwitchFieldsFilterStateAction => ({
    type: StateActionTypes.SWITCH_FIELDS_FILTER,
    status: status
})

export const nextTestCase = (): NextTestCaseStateAction => ({
    type: StateActionTypes.NEXT_TEST_CASE
})

export const prevTestCase = (): PrevTestCaseStateAction => ({
    type: StateActionTypes.PREV_TEST_CASE
})

export const setTestCasePath = (testCasePath: string): SetTestCasePathStateAction => ({
    type: StateActionTypes.SET_TEST_CASE_PATH,
    testCasePath: testCasePath
})

export const switchSplitMode = (): SwitchSplitModeStateAction => ({
    type: StateActionTypes.SWITCH_SPLIT_MODE
})

export const showFilter = (): ShowFilterStateAction => ({
    type: StateActionTypes.SHOW_FILTER
})

export const setLeftPane = (pane: Pane): SetLeftPaneStateActions => ({
    type: StateActionTypes.SET_LEFT_PANE,
    pane: pane
})

export const setRightPane = (pane: Pane): SetRightPaneStateAction => ({
    type: StateActionTypes.SET_RIGHT_PANE,
    pane: pane
})