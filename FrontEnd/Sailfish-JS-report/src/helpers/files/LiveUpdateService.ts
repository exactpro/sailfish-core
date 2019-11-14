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

import { watchFile, FileWatchCallback, fetchUpdate } from "./fetcher";
import { ActionNode } from "../../models/Action";
import Message from "../../models/Message";
import { isEqual } from "../object";
import { isDateEqual } from "../date";
import LiveTestCase from "../../models/LiveTestCase";

const STUB_CALLBACK = () => {};

const LIVE_UPDATE_FOLDER_PATH = 'reportData/live/',
    LIVE_UPDATE_ROOT_PATH = LIVE_UPDATE_FOLDER_PATH + 'root.js';

const ROOT_JSONP_PATH = 'loadLiveReport',
    ACTION_JSONP_PATH = 'loadAction',
    MESSAGE_JSONP_PATH = 'loadMessage';

const ROOT_WATCH_INTERVAL = 1500;

type DataFiles = {
    [key: string]: number
};

interface RootState extends LiveTestCase {
    dataFiles: DataFiles;
    lastUpdate: string;
}

type FileUpdate = {
    [ACTION_JSONP_PATH]: ActionNode[],
    [MESSAGE_JSONP_PATH]: Message[]
}

type ActionUpdate = (actions: ActionNode[]) => unknown;
type MessageUpdate = (messages: Message[]) => unknown;
type TestCaseUpdate = (tc: LiveTestCase) => unknown;

export default class LiveUpdateService {

    private rootFileWatchIntervalId = null
    private onActionUpdate: ActionUpdate = STUB_CALLBACK;
    private onMessageUpdate: MessageUpdate = STUB_CALLBACK;
    private onTestCaseUpdate: TestCaseUpdate = STUB_CALLBACK;
    private onError: (err: Error) => void = STUB_CALLBACK;
    private rootState: RootState = {
        startTime: '',
        name: '',
        id: '',
        hash: null,
        description: '',
        dataFiles: {},
        lastUpdate: ''
    };

    constructor(private rootFilePath: string = LIVE_UPDATE_ROOT_PATH) { }

    public start() {
        this.rootFileWatchIntervalId = watchFile(
            this.rootFilePath, 
            ROOT_JSONP_PATH, 
            ROOT_WATCH_INTERVAL, 
            this.onRootUpdate
        );
    }
        
    public stop() {
        clearInterval(this.rootFileWatchIntervalId);
    }

    public set setOnActionUpdate(cb: ActionUpdate) {
        this.onActionUpdate = cb;
    }

    public set setOnMessageUpdate(cb: MessageUpdate) {
        this.onMessageUpdate = cb;
    }

    public set setOnTestCaseUpdate(cb: TestCaseUpdate) {
        this.onTestCaseUpdate = cb;
    }

    public set setOnFetchError(cb: () => void) {
        this.onError = cb;
    }

    private onRootUpdate: FileWatchCallback = e => {
        switch(e.type) {
            case 'data': {
                this.updateRootState(e.data as RootState);
                break;
            }

            case 'error': {
                this.onError(e.err);
                console.error(e.err);
                break;
            }

            default: {
                console.warn('Update with unknown type has been received.');
                return;
            }
        }
    }

    private updateRootState(state: RootState) {
        const { dataFiles, lastUpdate, ...tcInfo } = state;

        if (isDateEqual(state.lastUpdate, this.rootState.lastUpdate)) {
            // no update found
            return;
        }

        if (lastUpdate !== this.rootState.lastUpdate) {
            if (tcInfo.hash !== this.rootState.hash) {
                this.onTestCaseUpdate(tcInfo);
                this.fetchFullUpdateData(state.dataFiles)
            } else if (!isEqual(dataFiles, this.rootState.dataFiles)) {
                this.fetchDiffUpdateData(state.dataFiles, this.rootState.dataFiles);
            }
        }

        this.rootState = state;
    }

    private async fetchDiffUpdateData(nextFiles: DataFiles, prevFiles: DataFiles) {        
        for (let [filePath, index] of Object.entries(nextFiles)) {
            const prevIndex = typeof prevFiles[filePath] === 'number' ?
                prevFiles[filePath] :
                Number.MIN_SAFE_INTEGER;

            const fileUpdate = await fetchUpdate<FileUpdate>(
                LIVE_UPDATE_FOLDER_PATH + filePath, 
                [ACTION_JSONP_PATH, MESSAGE_JSONP_PATH],
                prevIndex,
                index
            );

            this.handleFileUpdate(fileUpdate);
        }
    }

    private async fetchFullUpdateData(dataFiles: DataFiles) {
        for (let [filePath, currentIndex] of Object.entries(dataFiles)) {
            const fileUpdate = await fetchUpdate<FileUpdate>(
                LIVE_UPDATE_FOLDER_PATH + filePath,
                [ACTION_JSONP_PATH, MESSAGE_JSONP_PATH],
                currentIndex
            );

            this.handleFileUpdate(fileUpdate);
        }
    }

    private handleFileUpdate(update: FileUpdate) {
        Object.entries(update).forEach(([path, updatedValues]) => {
            switch (path) {
                case ACTION_JSONP_PATH: {
                    if (updatedValues.length != 0) {
                        this.onActionUpdate(updatedValues as ActionNode[]);
                    }

                    break;
                }

                case MESSAGE_JSONP_PATH: {
                    if (updatedValues.length != 0) {
                        this.onMessageUpdate(updatedValues as Message[]);
                    }

                    break;
                }

                default: {
                    console.warn(`Update with unknown jsonp path has been received (${path}).`);
                    break;
                }
            }
        })
    }


}
