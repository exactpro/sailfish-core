/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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