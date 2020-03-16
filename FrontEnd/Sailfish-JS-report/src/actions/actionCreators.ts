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

import TestCase from "../models/TestCase";
import { StateActionTypes } from "./stateActions";
import Action from '../models/Action';
import { StatusType } from "../models/Status";
import Report from "../models/Report";
import Panel from "../util/Panel";
import Message from '../models/Message';
import { PredictionData, SubmittedData } from "../models/MlServiceResponse";
import SearchResult from '../helpers/search/SearchResult';
import KnownBug from '../models/KnownBug';
import { FilterBlock } from "../models/filter/FilterBlock";
import Log from '../models/Log';
import SearchToken from "../models/search/SearchToken";
import PanelArea from "../util/PanelArea";

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

export const selectAction = (action: Action, shouldScrollIntoView: boolean = false) => (<const>{
    type: StateActionTypes.SELECT_ACTION,
    action,
    shouldScrollIntoView
})

export const selectActionById = (actionId: number) => (<const>{
    type: StateActionTypes.SELECT_ACTION_BY_ID,
    actionId
})

export const selectMessage = (message: Message, status: StatusType = null, shouldScrollIntoView: boolean = false) => (<const>{
    type: StateActionTypes.SELECT_MESSAGE,
    message,
    status,
    shouldScrollIntoView
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

export const selectKnownBug = (knownBug: KnownBug, status: StatusType = null) => (<const>{
    type: StateActionTypes.SELECT_KNOWN_BUG,
    knownBug,
    status
})

export const setFilterResult = (results: string[]) => (<const>{
    type: StateActionTypes.SET_FILTER_RESULTS,
    results
})

export const setFilterBlocks = (blocks: FilterBlock[]) => (<const>{
    type: StateActionTypes.SET_FILTER_CONFIG,
    blocks
})

export const setFilterIsTransparent = (isTransparent: boolean) => (<const>{
    type: StateActionTypes.SET_FILTER_IS_TRANSPARENT,
    isTransparent
})

export const setFilterIsHighlighted = (isHighlighted: boolean) => (<const>{
    type: StateActionTypes.SET_FILTER_IS_HIGHLIGHTED,
    isHighlighted
})

export const setAdminMsgEnabled = (adminEnabled: boolean) => (<const>{
    type: StateActionTypes.SET_ADMIN_MSG_ENABLED,
    adminEnabled
})

export const resetFilter = () => (<const>{
    type: StateActionTypes.RESET_FILTER
})

export const setLeftPane = (panel: Panel.ACTIONS | Panel.STATUS) => (<const>{
    type: StateActionTypes.SET_LEFT_PANE,
    panel
})

export const setRightPane = (panel: Panel.MESSAGES | Panel.KNOWN_BUGS | Panel.LOGS) => (<const>{
    type: StateActionTypes.SET_RIGHT_PANE,
    panel
})

export const setPanelArea = (panelArea: PanelArea) => (<const>{
    type: StateActionTypes.SET_PANEL_AREA,
    panelArea
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

export const setSearchTokens = (searchTokens: SearchToken[]) => (<const>{
    type: StateActionTypes.SET_SEARCH_TOKENS,
    searchTokens
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

export const setSearchLeftPanelEnabled = (isEnabled: boolean) => (<const>{
    type: StateActionTypes.SET_SEARCH_LEFT_PANEL_ENABLED,
    isEnabled
})

export const setSearchRightPanelEnabled = (isEnabled: boolean) => (<const>{
    type: StateActionTypes.SET_SEARCH_RIGHT_PANEL_ENABLED,
    isEnabled
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

export const addTestCaseMessages = (messages: Message[], testCaseOrder: number) => (<const>{
    type: StateActionTypes.ADD_TEST_CASE_MESSAGES,
    messages,
    testCaseOrder
})

export const addTestCaseActions = (actions: Action[], testCaseOrder: number) => (<const>{
    type: StateActionTypes.ADD_TEST_CASE_ACTIONS,
    actions,
    testCaseOrder
})

export const addTestCaseLogs = (logs: Log[], testCaseOrder: number) => (<const>{
    type: StateActionTypes.ADD_TEST_CASE_LOGS,
    logs,
    testCaseOrder
})

export const updateTestCase = (testCase: TestCase) => (<const>{
    type: StateActionTypes.UPDATE_TEST_CASE,
    testCase
})


export const setIsConnectionError = (isConnectionError: boolean) => (<const>{
    type: StateActionTypes.SET_IS_CONNECTION_ERROR,
    isConnectionError
})

export const selectActionsScrollHintIds = (actionId: Number) => (<const>{
    type: StateActionTypes.SELECT_ACTIONS_SCROLL_HINT_IDS,
    actionId
})

export const selectMessagesScrollHintIds = (messageId: Number) => (<const>{
    type: StateActionTypes.SELECT_MESSAGES_SCROLL_HINT_IDS,
    messageId
})
