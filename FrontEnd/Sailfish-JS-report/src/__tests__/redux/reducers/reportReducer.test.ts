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

import thunk from "redux-thunk";
import configureStore from "redux-mock-store";
import { initialAppState } from "../../../state/initial/initialAppState";
import { setReport } from "../../../actions/actionCreators";
import Report from "../../../models/Report";
import { reportReducer } from "../../../reducers/reportReducer";

const middlewares = [thunk],
    mockStore = configureStore(middlewares);

const mockReport: Report = {
    metadata: [],
    startTime: '0',
    finishTime: '1',
    plugins: [],
    bugs: [],
    userName: '',
    hostName: '',
    name: 'report',
    scriptRunId: 0,
    version: '',
    branchName: 'master',
    description: '',
    precision: '0.1'
};

describe('[Redux] Report reducer', () => {
    test('SET_REPORT action', () => {
        const store = mockStore(initialAppState);        

        store.dispatch(setReport(mockReport));
        
        const actions = store.getActions(),
            expectedPlayload = setReport(mockReport);

        expect(actions).toEqual([expectedPlayload]);
    });

    test('SET_REPORT reducer handle', () => {
        const action = setReport(mockReport);

        const state = reportReducer(null, action);

        expect(state).toEqual(mockReport);
    });
});

