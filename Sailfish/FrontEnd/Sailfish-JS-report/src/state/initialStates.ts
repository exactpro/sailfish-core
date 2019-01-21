import AppState from "./AppState";
import { statusValues } from "../models/Status";
import SelectedState from './SelectedState';
import { Pane } from "../helpers/Pane";

export const initialSelectedState: SelectedState = {
    actionId: null,
    messagesId: [],
    status: 'NA'
}

export const initialAppState: AppState = {
    report: null,
    currentTestCasePath: "",
    actionsFilter: statusValues,
    fieldsFilter: statusValues,
    testCase: null,
    selected: initialSelectedState,
    splitMode: true,
    showFilter: false,
    leftPane: Pane.Actions,
    rightPane: Pane.Messages
}