import TestCaseState from "./TestCaseState";
import { statusValues } from "../models/Status";
import SelectedState from './SelectedState';

export const initialSelectedState: SelectedState = {
    actionId: null,
    messagesId: null,
    status: 'NA'
}

export const initialTestCaseState: TestCaseState = {
    actionsFilter: statusValues,
    testCase: null,
    selected: initialSelectedState
}