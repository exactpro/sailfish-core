/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import { ThunkAction } from "redux-thunk";
import AppState from '../state/models/AppState';
import { setTestCase, setIsLoading, setIsConnectionError, addTestCaseLogs, resetTestCase } from "../actions/actionCreators";
import { addTestCaseActions, addTestCaseMessages } from '../actions/actionCreators'
import StateAction from '../actions/stateActions';
import TestCase, { TestCaseFiles } from '../models/TestCase';
import { isAction } from '../models/Action';
import { isMessage } from '../models/Message';
import { isLog } from '../models/Log';
import ThunkExtraArgument from '../models/ThunkExtraArgument';
import { findNextCyclicItem, findPrevCyclicItem } from "../helpers/array";
import { fetchTestCase, fetchTestCaseFile } from "../helpers/jsonp/jsonp";
import { getObjectKeys } from '../helpers/object';
import { retryRequest } from '../helpers/jsonp/retryRequest';

export const CURRENT_TESTCASE_ORDER = 'CURRENT_TESTCASE_ORDER';

export function loadTestCase(testCasePath: string): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { liveUpdateService }) => {
        dispatch(setIsLoading(true));
        const { report: { metadata }, selected: { testCase } } = getState();
        if (testCase) {
            dispatch(resetTestCase());
        };
        const { order, finishTime } = metadata.find(meta => meta.jsonpFileName === testCasePath);
        const isLiveTestCase = finishTime === null;
        window[CURRENT_TESTCASE_ORDER] = order;

        fetchTestCase(testCasePath)
            .then((testCase: TestCase) => {
                dispatch(setTestCase(testCase));
                if (isLiveTestCase) {
                    liveUpdateService.startWatchingTestCase(testCasePath);
                } else { 
                    dispatch(loadMessagesAndActions());
                }
            })
            .catch(err => {
                console.error('Error catched while trying to fetch test case');
                console.error(err);
            })
            .finally(() => dispatch(setIsLoading(false)));
    }
}

export function loadNextTestCase(): ThunkAction<void, AppState, {}, StateAction> {
    return (dispatch, getState) => {
        const { report, selected } = getState(),
            nextMetadata = findNextCyclicItem(report.metadata, metadata => metadata.order === selected.testCase.order);
            dispatch(loadTestCase(nextMetadata.jsonpFileName));
    }
}

export function loadPrevTestCase(): ThunkAction<void, AppState, {}, StateAction> {
    return (dispatch, getState) => {
        const { report, selected } = getState(),
            nextMetadata = findPrevCyclicItem(report.metadata, metadata => metadata.order === selected.testCase.order);
            dispatch(loadTestCase(nextMetadata.jsonpFileName))
    }
}

export function loadTestCaseFiles(files: TestCaseFiles, testCaseOrder: number): ThunkAction<void, AppState, {}, StateAction> {
    return (dispatch) => {  
        getObjectKeys(files)
            .filter(fileType => files[fileType].count > 0)
            .forEach(async fileType => {
                for (const [filePath, index] of Object.entries(files[fileType].dataFiles)) {
                    try {
                        const fileData = await retryRequest(
                            () => fetchTestCaseFile(filePath, index, fileType, testCaseOrder), 3
                        );
                        switch(fileType){
                            case "action":
                                dispatch(addTestCaseActions(fileData.filter(isAction), testCaseOrder))
                                break;
                            case "message":
                                dispatch(addTestCaseMessages(fileData.filter(isMessage), testCaseOrder))
                                break;
                            case "logentry":
                                dispatch(addTestCaseLogs(fileData.filter(isLog), testCaseOrder))
                                break;
                            default:
                                console.warn(`Unknown file type has been received: ${fileType}`);
                                return;
                        }

                    } catch (err) {
                        if (err.isCancelled) {
                            break;
                        } else {
                            dispatch(setIsConnectionError(true));
                            console.error('Error occured while fetching test case file');
                            console.error(err);
                        }
               
                    }
                }
            })
    }
}

export function loadMessagesAndActions(): ThunkAction<void, AppState, {}, StateAction> {
    return (dispatch, getState) => {
        const { selected: { testCase }} = getState();
        const filesToLoad: TestCaseFiles = {
            action: testCase.files.action,
            message: testCase.files.message
        }
        dispatch(loadTestCaseFiles(filesToLoad, testCase.order));
    }
}

export function loadLogs(): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { liveUpdateService }) => {
        const { selected: { testCase }} = getState();
        if (testCase.finishTime !== null) {
            const filesToLoad: TestCaseFiles = {
                logentry: testCase.files.logentry
            }
            dispatch(loadTestCaseFiles(filesToLoad, testCase.order));
        } else {
            liveUpdateService.startWatchingLogs();
        }

    }
}

export function stopWatchingTestCase(): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { liveUpdateService }) => {
        liveUpdateService.stopWatchingTestcase();
    }
}
