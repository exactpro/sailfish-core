import TestCase from "../models/TestCase";
import Action from '../models/Action';
import Message from '../models/Message';
import { StatusType } from "../models/Status";

export enum StateActionTypes { 
    UPDATE_TEST_CASE = 'UPDATE_TEST_CASE',
    SELECT_ACTION = 'SELECT_ACTION',
    SELECT_MESSAGES = 'SELECT_MESSAGES',
    ADD_ACTION_FILTER = 'ADD_ACTION_FILTER',
    REMOVE_ACTION_FILTER = 'REMOVE_ACTION_FILTER'
}

export interface UpdateTestCaseStateAction { 
    type: StateActionTypes.UPDATE_TEST_CASE;
    testCase: TestCase;
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

export type StateActionType = UpdateTestCaseStateAction | 
    ActionSelectStateAction |
    MessagesSelectStateAction | 
    AddActionFilterStateAction | 
    RemoveActionFilterStateAction;
