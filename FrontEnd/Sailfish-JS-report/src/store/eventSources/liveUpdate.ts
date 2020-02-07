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

import LiveUpdateService from '../../helpers/files/LiveUpdateService';
import { Store } from 'redux';
import AppState from '../../state/models/AppState';
import StateAction from '../../actions/stateActions';
import { ThunkDispatch } from 'redux-thunk';
import { updateLiveActions, updateLiveMessages, updateLiveTestCase, setReport, setTestCase } from '../../actions/actionCreators';
import { fetchReport, fetchTestCase } from '../../helpers/jsonp/jsonp';
import { batch } from 'react-redux';
import TestCase from '../../models/TestCase';

export function initLiveUpdateEventSource(store: Store<AppState, StateAction>, service: LiveUpdateService) {
    const dispatch: ThunkDispatch<AppState, {}, StateAction> = store.dispatch;
    
    service.setOnTestCaseUpdate = liveTestCase => {
        const { selected } = store.getState(), 
            { hash: prevLiveTestCaseHash } = selected.live.testCase,
            isLiveTestCaseSelected = selected.testCase != null && selected.testCase.hash === prevLiveTestCaseHash;
            
        if (prevLiveTestCaseHash == null || liveTestCase.hash == prevLiveTestCaseHash) {
            dispatch(updateLiveTestCase(liveTestCase));
            return;
        }
        
        // Here we handle situation when current live testcase is already done and 
        // we need to reload report and current test case.
        fetchReport()
            .then(async report => {
                let testCase: TestCase = null;

                if (isLiveTestCaseSelected) {
                    const { jsonpFileName } = report.metadata
                        .find(metadata => metadata.hash === selected.testCase.hash);
                    
                    testCase = await fetchTestCase(jsonpFileName);
                }
                
                batch(() => {
                    dispatch(setReport(report));
                    dispatch(updateLiveTestCase(liveTestCase));

                    if (testCase) {
                        dispatch(setTestCase(testCase));
                    }
                });
            })
            .catch(err => {
                console.error('Unable to fetch report after live update.')
                console.error(err);
            });
    }

    service.setOnActionUpdate = updatedActions => {
        dispatch(updateLiveActions(updatedActions));
    }

    service.setOnMessageUpdate = updatedMessages => {
        dispatch(updateLiveMessages(updatedMessages));
    }

    service.setOnFetchError = () => {
        fetchReport()
            .then(report => {
                // root.js doesn't exist and report in no more running
                if (report.finishTime !== null) {
                    service.stop();                    
                } 

                // else - root.js doesn't exist, but report still running - 
                // we dont't need to stop live update service
                if (JSON.stringify(report) !== JSON.stringify(store.getState().report)) {
                    dispatch(setReport(report));
                }
            });
    }
}
    