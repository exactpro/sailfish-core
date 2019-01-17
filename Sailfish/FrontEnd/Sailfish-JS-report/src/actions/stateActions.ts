import TestCase from "../models/TestCase";
import Action from '../models/Action';
import { StatusType } from "../models/Status";
import Report from '../models/Report';

export enum StateActionTypes {
    SET_REPORT = 'SET_REPORT', 
    NEXT_TEST_CASE = 'NEXT_TEST_CASE',
    PREV_TEST_CASE = 'PREV_TEST_CASE',
    SET_TEST_CASE_PATH = 'SET_TEST_CASE_PATH',
    SET_TEST_CASE = 'SET_TEST_CASE',
    RESET_TEST_CASE= 'RESET_TEST_CASE',
    SELECT_ACTION = 'SELECT_ACTION',
    SELECT_MESSAGES = 'SELECT_MESSAGES',
    ADD_ACTION_FILTER = 'ADD_ACTION_FILTER',
    REMOVE_ACTION_FILTER = 'REMOVE_ACTION_FILTER'
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

export interface MessagesSelectStateAction {
    type: StateActionTypes.SELECT_MESSAGES;
    messagesId: number[];
    status: StatusType;
}

export interface AddActionFilterStateAction {
    type: StateActionTypes.ADD_ACTION_FILTER;
    status: StatusType;
}

export interface RemoveActionFilterStateAction {
    type: StateActionTypes.REMOVE_ACTION_FILTER;
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

export type StateActionType = SetReportStateAction |
    SetTestCaseStateAction | 
    ResetTestCaseStateAction |
    ActionSelectStateAction |
    MessagesSelectStateAction | 
    AddActionFilterStateAction | 
    RemoveActionFilterStateAction | 
    NextTestCaseStateAction |
    PrevTestCaseStateAction | 
    SetTestCaseStateAction | 
    SetTestCasePathStateAction;
