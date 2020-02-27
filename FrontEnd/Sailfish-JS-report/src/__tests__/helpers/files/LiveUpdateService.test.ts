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

import waitForExpect from 'wait-for-expect';
import * as JsonpModule from '../../../helpers/jsonp/jsonp';
import * as FetcherModule from '../../../helpers/files/fetcher';
import LiveUpdateService from '../../../helpers/files/LiveUpdateService';
import { createAction, createMessage, createTestCase, createReport, createTestCaseMetadata } from '../../util/creators';
import LiveTestCase from '../../../models/LiveTestCase';

type Writable<T> = {
    -readonly [K in keyof T]: T[K]
}

const jsonpModule = JsonpModule as Writable<typeof JsonpModule>,
    successInitWatchReportMock = jest.fn<ReturnType<typeof JsonpModule.watchReport>, Parameters<typeof JsonpModule.watchReport>>(() => null),
    successInitWatchTestCaseMock = jest.fn<ReturnType<typeof JsonpModule.watchLiveTestCase>, Parameters<typeof JsonpModule.watchLiveTestCase>>(() => null),
    failedInitWatchTestCaseMock = jest.fn<ReturnType<typeof JsonpModule.watchLiveTestCase>, Parameters<typeof JsonpModule.watchLiveTestCase>>(() => null),
    fetcherModule = FetcherModule as Writable<typeof FetcherModule>,
    mocks = [
        successInitWatchReportMock, 
        successInitWatchTestCaseMock, 
        failedInitWatchTestCaseMock
    ];

const ROOT_JSONP_PATH = 'loadJsonp',
    ACTION_JSONP_PATH = 'loadAction',
    MESSAGE_JSONP_PATH = 'loadMessage',
    JSONP_PATHS = [ROOT_JSONP_PATH, ACTION_JSONP_PATH, MESSAGE_JSONP_PATH];

describe('LiveUpdateService tests', () => {

    describe('Report updates', () => {

        beforeEach(() => {
            JSONP_PATHS.forEach(path => delete window[path]);
            mocks.forEach(mock => mock.mockClear());
        })

        test('receives Report updates', () => {
            jsonpModule.watchReport = successInitWatchReportMock;
            const service = new LiveUpdateService(),
                updateReportHandler = jest.fn(),
                report = createReport(
                    new Date().toString(),
                    null,
                    [
                        createTestCaseMetadata(1, null, Math.random())
                    ]
                ),
                updatedReport = {
                    ...report,
                    metadata: [
                        createTestCaseMetadata(1, new Date().toString(), Math.random()),
                        createTestCaseMetadata(2, null, Math.random()),
                    ]
                };

            service.startWatchingReport();
            expect(successInitWatchReportMock).toHaveBeenCalledTimes(1);

            const reportUpdateCb = successInitWatchReportMock.mock.calls[0][1];
            service.setOnReportUpdate = updateReportHandler;

            reportUpdateCb({ type: 'data', data: report });
            expect(updateReportHandler).not.toHaveBeenCalled();

            reportUpdateCb({ type: 'data', data: updatedReport });
            expect(updateReportHandler).toHaveBeenCalledWith(updatedReport);
        })
    });
    
    describe('TestCase updates', () => {
        const { actions, messages, ...liveTestCase } = createTestCase();
        const testCase: LiveTestCase = {
            ...liveTestCase,
            name: 'test',
            id: 'test',
            hash: 0,
            description: 'test',
            finishTime: null,
            lastUpdate: new Date().toString(),
            order: 1
        };
    
        const action = createAction();
        const message = createMessage();
    
        beforeEach(() => {
            JSONP_PATHS.forEach(path => delete window[path]);
            mocks.forEach(mock => mock.mockClear());
        })

        test('initiates live testCase', async () => {
            jsonpModule.watchLiveTestCase = successInitWatchTestCaseMock;
            const testcaseFilePath = 'reportData/jsonp/testcase-1/testcase.js';
            const service = new LiveUpdateService(),
                updateTestCaseHandler = jest.fn(),
                updatedLiveTestCase = {
                    ...testCase,
                    finishTime: null,
                    files: null,
                    lastUpdate: new Date().toString(),
                };
    
            service.startWatchingTestCase(testcaseFilePath);
    
            expect(successInitWatchTestCaseMock).toHaveBeenCalledTimes(1);
    
            const testCaseCB = successInitWatchTestCaseMock.mock.calls[0][2];
            service.setOnTestCaseUpdate = updateTestCaseHandler;
    
            testCaseCB({ type: 'data', data: updatedLiveTestCase });
    
            expect(updateTestCaseHandler).toHaveBeenCalledTimes(1);
            expect(updateTestCaseHandler).toHaveBeenCalledWith(updatedLiveTestCase);
        })
    
        test('fetches testcase files on update', async () => {
            jsonpModule.watchLiveTestCase = successInitWatchTestCaseMock;
            const testcaseFilePath = 'reportData/jsonp/testcase-1/testcase.js';
            const fetchUpdateMock = jest.fn(async () => ({
                [ACTION_JSONP_PATH]: [action],
                [MESSAGE_JSONP_PATH]: [message]
            }));
    
            fetcherModule.fetchUpdate = fetchUpdateMock as any;
    
            const service = new LiveUpdateService(),
                onActionMock = jest.fn(),
                onMessageMock = jest.fn(),
                updatedLiveTestCase = {
                    ...testCase,
                    files: { 
                        action: {
                            count: 1,
                            dataFiles: {
                                'reportData/jsonp/testcase-1/actions/data1.js': 1
                            },
                            lastUpdated: new Date().toString(),
                        },
                    },
                    lastUpdate: new Date().toString(),
                };
    
    
            service.setOnActionUpdate = onActionMock;
            service.setOnMessageUpdate = onMessageMock;
    
            service.startWatchingTestCase(testcaseFilePath);
            const rootStateInit = successInitWatchTestCaseMock.mock.calls[0][2];
            rootStateInit({ type: 'data', data: updatedLiveTestCase });
            expect(fetchUpdateMock.mock.calls.length).toBe(1);
            expect(fetchUpdateMock.mock.calls[0]).toEqual([
                'reportData/jsonp/testcase-1/actions/data1.js', 
                [ACTION_JSONP_PATH], 
                1,
                Number.MIN_SAFE_INTEGER,
                testCase.order
            ]);

            // wait for internal async calls
            await waitForExpect(() => {
                expect(onActionMock.mock.calls.length).toBe(1);
                expect(onActionMock.mock.calls[0][0]).toEqual([action]);
                expect(onMessageMock.mock.calls.length).toBe(1);
                expect(onMessageMock.mock.calls[0][0]).toEqual([message]);
            });
        })
    });

})
