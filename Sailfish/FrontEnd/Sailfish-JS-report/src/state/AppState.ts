import TestCase from "../models/TestCase";
import { StatusType } from "../models/Status";
import SelectedState from './SelectedState';
import Report from '../models/Report';


export default interface AppState {
    report: Report;
    currentTestCasePath: string;
    testCase: TestCase;
    actionsFilter: StatusType[];
    selected: SelectedState;
}