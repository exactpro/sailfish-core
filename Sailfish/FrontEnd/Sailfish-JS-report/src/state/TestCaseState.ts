import TestCase from "../models/TestCase";
import { StatusType } from "../models/Status";
import SelectedState from './SelectedState';


export default interface TestCaseState {
    testCase: TestCase;
    actionsFilter: StatusType[];
    selected: SelectedState;
}