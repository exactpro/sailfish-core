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

import AppState from "./AppState";
import { statusValues } from "../models/Status";
import SelectedState from './SelectedState';
import { Pane } from "../helpers/Pane";

export const initialSelectedState: SelectedState = {
    actionId: null,
    messagesId: [],
    status: 'NA',
    checkpointMessageId: null,
    rejectedMessageId: null
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