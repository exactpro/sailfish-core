import TestCase from "../models/TestCase";
import { StatusType } from "../models/Status";
import SelectedState from './SelectedState';
import Report from '../models/Report';
import { Pane } from "../helpers/Pane";


export default interface AppState {
    report: Report;
    currentTestCasePath: string;
    testCase: TestCase;
    actionsFilter: StatusType[];
    fieldsFilter: StatusType[];
    selected: SelectedState;
    splitMode: boolean;
    showFilter: boolean;
    leftPane: Pane;
    rightPane: Pane;
}