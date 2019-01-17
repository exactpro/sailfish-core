import TestCase from "../models/TestCase";
import {
    SetTestCaseStateAction,
    StateActionTypes,
    ActionSelectStateAction,
    MessagesSelectStateAction,
    AddActionFilterStateAction,
    RemoveActionFilterStateAction,
    NextTestCaseStateAction,
    PrevTestCaseStateAction,
    SetTestCasePathStateAction,
    SetReportStateAction,
    ResetTestCaseStateAction
} from "./stateActions";
import Action from '../models/Action';
import { StatusType } from "../models/Status";
import Report from "../models/Report";

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

export const selectMessages = (messages: number[], status: StatusType = 'NA'): MessagesSelectStateAction => ({
    type: StateActionTypes.SELECT_MESSAGES,
    messagesId: messages,
    status: status
})

export const addActionFilter = (status: StatusType): AddActionFilterStateAction => ({
    type: StateActionTypes.ADD_ACTION_FILTER,
    status: status
})

export const removeActionFilter = (status: StatusType): RemoveActionFilterStateAction => ({
    type: StateActionTypes.REMOVE_ACTION_FILTER,
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