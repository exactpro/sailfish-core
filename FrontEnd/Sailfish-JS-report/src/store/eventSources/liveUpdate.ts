/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * u may not use this file except in compliance with the License.
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

import { Store } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import LiveUpdateService from '../../helpers/files/LiveUpdateService';
import AppState from '../../state/models/AppState';
import StateAction from '../../actions/stateActions';
import { setReport, updateTestCase, addTestCaseActions, addTestCaseMessages, setIsConnectionError, addTestCaseLogs } from '../../actions/actionCreators';
import { isAction } from '../../models/Action';
import { isMessage } from '../../models/Message';
import Report from '../../models/Report';
import { isDateEqual } from '../../helpers/date';
import { isEqual } from '../../helpers/object';
import { isLog } from '../../models/Log';

export function initLiveUpdateEventSource(store: Store<AppState, StateAction>, service: LiveUpdateService) {
    const dispatch: ThunkDispatch<AppState, {}, StateAction> = store.dispatch;

    service.setOnReportUpdate = report => {
        dispatch(setReport(report));
    }

    service.setOnTestCaseUpdate = liveTestCase => {
        const testCase = store.getState().selected.testCase;

        if (
            !isDateEqual(testCase.lastUpdate, liveTestCase.lastUpdate) ||
            !isEqual(testCase.files, liveTestCase.files)
            ) {
            dispatch(updateTestCase(liveTestCase));
        }

        const filesAreLoaded = (testCase.messages.length + testCase.actions.length) === 
            (liveTestCase.files.action.count + liveTestCase.files.message.count);

        if (liveTestCase.finishTime !== null && filesAreLoaded) {
            service.stopWatchingTestcase();
        }
    }

    service.setOnActionUpdate = (updatedActions, testcaseOrder) => {
        dispatch(addTestCaseActions(updatedActions.filter(isAction), testcaseOrder));
    }

    service.setOnMessageUpdate = (updatedMessages, testcaseOrder) => {
        dispatch(addTestCaseMessages(updatedMessages.filter(isMessage), testcaseOrder));
    }

    service.setOnLogsUpdate = (updatedLogs, testCaseOrder) => {
        dispatch(addTestCaseLogs(updatedLogs.filter(isLog), testCaseOrder));
    }

    service.setOnFetchError = () => {
        dispatch(setIsConnectionError(true));
        service.stopWatchingReport();
        service.stopWatchingTestcase();
    }

    service.setOnReportFinish = (report: Report) => {
        // There are no running test cases and report is no more running
        dispatch(setReport(report));
        if (report.finishTime !== null) {
            service.stopWatchingReport();
        }

        // There are no running test cases, but report still running - 
        // we dont't need to stop live update service
        if (JSON.stringify(report) !== JSON.stringify(store.getState().report)) {
            dispatch(setReport(report));
        }
    }
}
    