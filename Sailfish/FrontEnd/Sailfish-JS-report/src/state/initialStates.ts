import AppState from "./AppState";
import { statusValues } from "../models/Status";
import SelectedState from './SelectedState';

export const initialSelectedState: SelectedState = {
    actionId: null,
    messagesId: null,
    status: 'NA'
}

export const initialAppState: AppState = {
    report: null,
    currentTestCasePath: "",
    actionsFilter: statusValues,
    testCase: null,
    selected: initialSelectedState
}