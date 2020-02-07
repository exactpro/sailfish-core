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
import { fetchTestCase, fetchTestCaseFile } from "../helpers/jsonp/jsonp";
import { setTestCase, setIsLoading, selectLiveTestCase, setIsConnectionError, addTestCaseLogs } from "../actions/actionCreators";
import AppState from '../state/models/AppState';
import { findNextCyclicItem, findPrevCyclicItem } from "../helpers/array";
import { isTestCaseMetadata } from "../models/TestcaseMetadata";
import { addTestCaseActions, addTestCaseMessages } from '../actions/actionCreators'
import TestCase from '../models/TestCase';
import { isAction } from '../models/Action';
import { isMessage } from '../models/Message';
import { getObjectKeys } from '../helpers/object';
import { makeCancelableJsonpPromise } from '../helpers/jsonp/jsonpPromise'
import ThunkExtraArgument from '../models/ThunkExtraArgument';
import { isLog } from '../models/Log';
import StateAction from '../actions/stateActions';

export function loadTestCase(testCasePath: string): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { jsonpTaskController }) => {
        dispatch(setIsLoading(true));
        jsonpTaskController.cancelRunningTasks();
        fetchTestCase(testCasePath)
            .then((testCase: TestCase) => {
                dispatch(setTestCase(testCase));
                dispatch(loadMessagesAndActions());
            })
            .catch(err => {
                console.error('Error catched while trying to fetch test case');
                console.error(err);
            })
            .finally(() => dispatch(setIsLoading(false)));
    }
}

export function loadNextTestCase(): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { jsonpTaskController }) => {
        jsonpTaskController.cancelRunningTasks();
        const { report, selected } = getState(),
            nextMetadata = findNextCyclicItem(report.metadata, metadata => metadata.id === selected.testCase.id);

        if (isTestCaseMetadata(nextMetadata)) {
            dispatch(setIsLoading(true));

            fetchTestCase(nextMetadata.jsonpFileName)
                .then(testCase =>  {
                    dispatch(setTestCase(testCase));
                    dispatch(loadMessagesAndActions());
                })
                .catch(err => {
                    console.error('Error catched while trying to fetch test case');
                    console.error(err);
                })
                .finally(() => dispatch(setIsLoading(false)));
        } else {
            dispatch(selectLiveTestCase());
        }
    }
}

export function loadPrevTestCase(): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { jsonpTaskController }) => {
        jsonpTaskController.cancelRunningTasks();
        const { report, selected } = getState(),
            nextMetadata = findPrevCyclicItem(report.metadata, metadata => metadata.id === selected.testCase.id);

        if (isTestCaseMetadata(nextMetadata)) {
            dispatch(setIsLoading(true));

            fetchTestCase(nextMetadata.jsonpFileName)
                .then(testCase =>  {
                    dispatch(setTestCase(testCase));
                    dispatch(loadMessagesAndActions());
                })
                .catch(err => {
                    console.error('Error catched while trying to fetch test case');
                    console.error(err);
                })
                .finally(() => dispatch(setIsLoading(false)));
        } else {
            dispatch(selectLiveTestCase());
        }
    }
}

export function loadTestCaseFiles(files: TestCase['files']): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { jsonpTaskController }) => {  
        getObjectKeys(files)
            .filter(fileType => files[fileType].count > 0)
            .forEach(async fileType => {
                for (const [filePath, index] of Object.entries(files[fileType].dataFiles)) {
                    try {
                        const { promise, cancel } = makeCancelableJsonpPromise(
                            () => fetchTestCaseFile(filePath, index, fileType), filePath, 3
                        );
                        jsonpTaskController.addTask({ cancel, filePath });
                        const fileData = await promise;
                        switch(fileType){
                            case "action":
                                dispatch(addTestCaseActions(fileData.filter(isAction)))
                                break;
                            case "message":
                                dispatch(addTestCaseMessages(fileData.filter(isMessage)))
                                break;
                            case "logentry":
                                dispatch(addTestCaseLogs(fileData.filter(isLog)))
                                break;
                            default:
                                console.warn(`Unknown file type has been received: ${fileType}`);
                                return;
                        }
                        jsonpTaskController.removeTask(filePath);
                    } catch (err) {
                        if (!err.isCanceled) {
                            dispatch(setIsConnectionError(true));
                            console.error('Error occured while fetching test case file');
                            console.error(err);
                        }
                    }
                }
            })
    }
}

export function loadMessagesAndActions(): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { jsonpTaskController }) => {
        const { selected: { testCase }} = getState();
        const filesToLoad: TestCase['files'] = {
            action: testCase.files.action,
            message: testCase.files.message
        }
        dispatch(loadTestCaseFiles(filesToLoad));
    }
}

export function loadLogs(): ThunkAction<void, AppState, ThunkExtraArgument, StateAction> {
    return (dispatch, getState, { jsonpTaskController }) => {
        const { selected: { testCase }} = getState();
        const filesToLoad: TestCase['files'] = {
            logentry: testCase.files.logentry
        }
        dispatch(loadTestCaseFiles(filesToLoad));
    }
}
