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

import * as Fetcher from '../../../helpers/files/fetcher';
import LiveUpdateService from '../../../helpers/files/LiveUpdateService';
import { createAction, createMessage } from '../../util/creators';
import waitForExpect from 'wait-for-expect';

type Writable<T> = {
    -readonly [K in keyof T]: T[K]
}

const fetcherModule = Fetcher as Writable<typeof Fetcher>,
    successInitWatchFileMock = jest.fn<ReturnType<typeof Fetcher.watchFile>, Parameters<typeof Fetcher.watchFile>>(() => null),
    failedInitWatchFileMock = jest.fn<ReturnType<typeof Fetcher.watchFile>, Parameters<typeof Fetcher.watchFile>>(() => null),

    mocks = [successInitWatchFileMock, failedInitWatchFileMock];

const ROOT_JSONP_PATH = 'loadLiveReport',
    ACTION_JSONP_PATH = 'loadAction',
    MESSAGE_JSONP_PATH = 'loadMessage',
    JSONP_PATHS = [ROOT_JSONP_PATH, ACTION_JSONP_PATH, MESSAGE_JSONP_PATH];

describe('LiveUpdateService tests', () => {

    const testCase = {
        startTime: new Date().toString(),
        name: 'test',
        id: 'test',
        hash: 0,
        description: 'test'
    }

    const action = createAction();
    const message = createMessage();

    beforeEach(() => {
        JSONP_PATHS.forEach(path => delete window[path]);
        mocks.forEach(mock => mock.mockClear());
    })

    test('recieve TestCase update', async () => {
        fetcherModule.watchFile = successInitWatchFileMock;

        const service = new LiveUpdateService(),
            updateTestCaseHandler = jest.fn(),
            fileUpdate = {
                ...testCase,
                lastUpdate: new Date().toString(),
                dataFiles: {}
            };

        service.start();

        expect(successInitWatchFileMock.mock.calls.length).toBe(1);

        const testCaseCB = successInitWatchFileMock.mock.calls[0][3];
        service.setOnTestCaseUpdate = updateTestCaseHandler;

        testCaseCB({ type: 'data', data: fileUpdate });

        expect(updateTestCaseHandler.mock.calls.length).toBe(1);
        expect(updateTestCaseHandler.mock.calls[0][0]).toEqual(testCase);
    })

    test('update fetching', async () => {
        fetcherModule.watchFile = successInitWatchFileMock;

        const fetchUpdateMock = jest.fn(async () => ({
            [ACTION_JSONP_PATH]: [action],
            [MESSAGE_JSONP_PATH]: [message]
        }));

        fetcherModule.fetchUpdate = fetchUpdateMock as any;

        const service = new LiveUpdateService(),
            onActionMock = jest.fn(),
            onMessageMock = jest.fn(),
            fileUpdate = {
                ...testCase,
                lastUpdate: new Date().toString(),
                dataFiles: { 'test.js': 1 }
            };

        service.setOnActionUpdate = onActionMock;
        service.setOnMessageUpdate = onMessageMock;

        service.start();

        const rootStateInit = successInitWatchFileMock.mock.calls[0][3];
        rootStateInit({ type: 'data', data: fileUpdate });

        expect(fetchUpdateMock.mock.calls.length).toBe(1);
        expect(fetchUpdateMock.mock.calls[0]).toEqual([
            'reportData/live/test.js', 
            [ACTION_JSONP_PATH, MESSAGE_JSONP_PATH], 
            1
        ]);

        // wait for internal async calls
        await waitForExpect(() => {
            expect(onActionMock.mock.calls.length).toBe(1);
            expect(onActionMock.mock.calls[0][0]).toEqual([action]);
            expect(onMessageMock.mock.calls.length).toBe(1);
            expect(onMessageMock.mock.calls[0][0]).toEqual([message]);
        });
    })


})

