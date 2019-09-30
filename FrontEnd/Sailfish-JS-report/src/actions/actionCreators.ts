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
import { StateActionTypes } from "./stateActions";
import Action, { ActionNode } from '../models/Action';
import { StatusType } from "../models/Status";
import Report from "../models/Report";
import { Panel } from "../util/Panel";
import Message from '../models/Message';
import {PredictionData, SubmittedData} from "../models/MlServiceResponse";
import SearchResult from '../helpers/search/SearchResult';
import LiveTestCase from '../models/LiveTestCase';

export const setReport = (report: Report) => (<const>{
    type: StateActionTypes.SET_REPORT,
    report
})

export const setTestCase = (testCase: TestCase) => (<const>{
    type: StateActionTypes.SET_TEST_CASE,
    testCase
})

export const resetTestCase = () => (<const>{
    type: StateActionTypes.RESET_TEST_CASE
})

export const selectAction = (action: Action) => (<const>{
    type: StateActionTypes.SELECT_ACTION,
    action
})

export const selectActionById = (actionId: number) => (<const>{
    type: StateActionTypes.SELECT_ACTION_BY_ID,
    actionId
})

export const selectMessage = (message: Message, status: StatusType = null) => (<const>{
    type: StateActionTypes.SELECT_MESSAGE,
    message,
    status
})

export const selectVerification = (messageId: number, rootActionId: number = null, status: StatusType = StatusType.NA) => (<const>{
    type: StateActionTypes.SELECT_VERIFICATION,
    messageId,
    status,
    rootActionId
})

export const selectCheckpointAction = (action: Action) => (<const>{
    type: StateActionTypes.SELECT_CHECKPOINT_ACTION,
    action
})

export const selectCheckpointMessage = (message: Message) => (<const>{
    type: StateActionTypes.SELECT_CHECKPOINT_MESSAGE,
    message
})

export const selectRejectedMessageId = (messageId: number) => (<const>{
    type: StateActionTypes.SELECT_REJECTED_MESSAGE,
    messageId
})

export const selectLiveTestCase = () => (<const>{
    type: StateActionTypes.SELECT_LIVE_TESTCASE
})

export const updateLiveTestCase = (testCase: LiveTestCase) => (<const>{
    type: StateActionTypes.UPDATE_LIVE_TEST_CASE,
    testCase
})

export const updateLiveActions = (actions: ActionNode[]) => (<const>{
    type: StateActionTypes.UPDATE_LIVE_ACTIONS,
    actions
})

export const updateLiveMessages = (messages: Message[]) => (<const>{
    type: StateActionTypes.UPDATE_LIVE_MESSAGES,
    messages
})

export const switchActionsFilter = (status: StatusType) => (<const>{
    type: StateActionTypes.SWITCH_ACTIONS_FILTER,
    status
})

export const switchFieldsFilter = (status: StatusType) => (<const>{
    type: StateActionTypes.SWITCH_FIELDS_FILTER,
    status
})

export const switchActionsTransparencyFilter = (status: StatusType) => (<const>{
    type: StateActionTypes.SWITCH_ACTIONS_TRANSPARENCY_FILTER,
    status
})

export const switchFieldsTransparencyFilter = (status: StatusType) => (<const>{
    type: StateActionTypes.SWITCH_FIELDS_TRANSPARENCY_FILTER,
    status
})

export const setAdminMsgEnabled = (adminEnabled: boolean) => (<const>{
    type: StateActionTypes.SET_ADMIN_MSG_ENABLED,
    adminEnabled
})

export const setLeftPane = (pane: Panel) => (<const>{
    type: StateActionTypes.SET_LEFT_PANE,
    pane
})

export const setRightPane = (pane: Panel) => (<const>{
    type: StateActionTypes.SET_RIGHT_PANE,
    pane
})

export const setMlToken = (token: string) => (<const>{
    type: StateActionTypes.SET_ML_TOKEN,
    token: token
})

export const setSubmittedMlData = (data: SubmittedData[]) => (<const>{
    type: StateActionTypes.SET_SUBMITTED_ML_DATA,
    data: data
})

export const addSubmittedMlData = (data: SubmittedData) => (<const>{
    type: StateActionTypes.ADD_SUBMITTED_ML_DATA,
    data: data
})

export const removeSubmittedMlData = (data: SubmittedData) => (<const>{
    type: StateActionTypes.REMOVE_SUBMITTED_ML_DATA,
    data: data
})

export const setSearchString = (searchString: string) => (<const>{
    type: StateActionTypes.SET_SEARCH_STRING,
    searchString
})

export const setSearchResults = (searchResults: SearchResult) => (<const>{
    type: StateActionTypes.SET_SEARCH_RESULTS,
    searchResults
})

export const nextSearchResult = () => (<const>{
    type: StateActionTypes.NEXT_SEARCH_RESULT
})

export const prevSearchResult = () => (<const>{
    type: StateActionTypes.PREV_SEARCH_RESULT
})

export const clearSearch = () => (<const>{
    type: StateActionTypes.CLEAR_SEARCH
})

export const setShouldScrollToSearchItem = (isNeedsScroll: boolean) => (<const>{
    type: StateActionTypes.SET_SHOULD_SCROLL_TO_SEARCH_ITEM,
    isNeedsScroll
}) 

export const setIsLoading = (isLoading: boolean) => (<const>{
    type: StateActionTypes.SET_IS_LOADING,
    isLoading
})

export const saveMlData = (data: PredictionData[]) => (<const>{
    type: StateActionTypes.SAVE_ML_DATA,
    data
})

export const togglePredictions = () => (<const>{
    type: StateActionTypes.TOGGLE_PREDICTIONS
})

export const setSelectedTestCase = (testCaseId: string) => (<const>{
    type: StateActionTypes.SET_SELECTED_TESTCASE,
    testCaseId
})

export const toggleMessageBeautifier = (messageId: number) => (<const>{
    type: StateActionTypes.TOGGLE_MESSAGE_BEAUTIFIER,
    messageId
})

export const uglifyAllMessages = () => (<const>{
    type: StateActionTypes.UGLIFY_ALL_MESSAGES
})
