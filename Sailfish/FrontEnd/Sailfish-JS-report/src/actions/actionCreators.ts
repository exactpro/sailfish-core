import TestCase from "../models/TestCase";
import {
    UpdateTestCaseStateAction,
    StateActionTypes,
    ActionSelectStateAction,
    MessagesSelectStateAction,
    AddActionFilterStateAction,
    RemoveActionFilterStateAction
} from "./stateActions";
import Action from '../models/Action';
import { StatusType } from "../models/Status";

export const updateTestCase = (testCase: TestCase): UpdateTestCaseStateAction => ({
    type: StateActionTypes.UPDATE_TEST_CASE,
    testCase: testCase
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

