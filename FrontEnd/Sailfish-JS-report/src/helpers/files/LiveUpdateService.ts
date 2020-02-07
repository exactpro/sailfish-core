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

import { fetchUpdate } from "./fetcher";
import { jsonpHandlerNames, watchLiveTestCase, FileWatchCallback, watchReport } from '../jsonp/jsonp'
import { ActionNode } from "../../models/Action";
import Message from "../../models/Message";
import { isEqual } from "../object";
import { isDateEqual } from "../date";
import LiveTestCase from "../../models/LiveTestCase";
import Report from '../../models/Report';
import TestCase from '../../models/TestCase';

const STUB_CALLBACK = () => {};

const ROOT_JSONP_PATH = jsonpHandlerNames.default,
    ACTION_JSONP_PATH = jsonpHandlerNames.files.action,
    MESSAGE_JSONP_PATH = jsonpHandlerNames.files.message;

const ROOT_WATCH_INTERVAL = 1500;

type DataFiles = {
    [key: string]: number
};

type FileUpdate = {
    [ACTION_JSONP_PATH]: ActionNode[],
    [MESSAGE_JSONP_PATH]: Message[]
}

type ActionUpdate = (actions: ActionNode[], testCaseOrder: number) => unknown;
type MessageUpdate = (messages: Message[], testCaseOrder: number) => unknown;
type TestCaseUpdate = (tc: TestCase) => unknown;
type ReportUpdate = (report: Report) => unknown;
type ReportFinish = (report: Report) => unknown;

export default class LiveUpdateService {

    private report: Report = null;
    private liveTestCase: TestCase = null;
    private reportWatchIntervalId = null;
    private testCaseWatchIntervalId = null;
    private onActionUpdate: ActionUpdate = STUB_CALLBACK;
    private onMessageUpdate: MessageUpdate = STUB_CALLBACK;
    private onTestCaseUpdate: TestCaseUpdate = STUB_CALLBACK;
    private onReportUpdate: ReportUpdate = STUB_CALLBACK;
    private onReportFinish: ReportFinish = STUB_CALLBACK;
    private onError: () => void = STUB_CALLBACK;

    public set setOnActionUpdate(cb: ActionUpdate) {
        this.onActionUpdate = cb;
    }

    public set setOnReportUpdate(cb: ReportUpdate) {
        this.onReportUpdate = cb;
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

    public set setOnReportFinish(cb: ReportFinish) {
        this.onReportFinish = cb;
    };

    public startWatchingReport() {
        this.reportWatchIntervalId = watchReport(
            ROOT_WATCH_INTERVAL, 
            this.onReportDataUpdate.bind(this)
        );
    }

    public stopWatchingReport() {
        clearInterval(this.reportWatchIntervalId);
        this.report = null;
    }

    private onReportDataUpdate: FileWatchCallback = e => {
        switch(e.type) {
            case 'data': {
                this.updateReport(e.data as Report);
                break;
            }

            case 'error': {
                this.onError();
                console.error(e.err);
                break;
            }

            default: {
                console.warn('Update with unknown type has been received.');
                return;
            }
        }
    }


    public updateReport(report: Report) {
        if (!this.report) {
            this.report = report;
            return  
        } 
        const currentLiveTestCase = this.report.metadata.find(meta => meta.finishTime === null);
        const liveTestCase = report.metadata.find(meta => meta.finishTime === null);
        if (!liveTestCase) {
            this.report = null;
            this.onReportFinish(report);
            return;
        }
        if (
            this.report.metadata.length !== report.metadata.length ||
            currentLiveTestCase.hash !== liveTestCase.hash
        ) {
            this.onReportUpdate(report);
            this.report = report;
        }
        
    }

    public startWatchingTestCase(jsonpFileName: string){
        this.stopWatchingTestcase();
        this.testCaseWatchIntervalId = watchLiveTestCase(
            jsonpFileName, 
            ROOT_WATCH_INTERVAL, 
            this.onTestCaseDataUpdate
        );
    }

    public stopWatchingTestcase() {
        clearInterval(this.testCaseWatchIntervalId);
        this.liveTestCase = null;
    }


    private onTestCaseDataUpdate: FileWatchCallback = e => {
        switch(e.type) {
            case 'data': {
                this.updateLiveTestCase(e.data as TestCase);
                break;
            }

            case 'error': {
                this.onError();
                console.error(e.err);
                break;
            }

            default: {
                console.warn('Update with unknown type has been received.');
                return;
            }
        }
    }

    private updateLiveTestCase(testcase: TestCase) {
        const { files, lastUpdate, hash } = testcase;
       
        if (this.liveTestCase && isDateEqual(lastUpdate, this.liveTestCase.lastUpdate)) {
            // no update found
            return;
        }

        if (lastUpdate !== this.liveTestCase?.lastUpdate) {
            this.onTestCaseUpdate(testcase);
            if (hash !== this.liveTestCase?.hash) {
                this.fetchFullUpdateData(files, testcase.order);
            } else if (!this.liveTestCase || !isEqual(files, this.liveTestCase.files)) {
                this.fetchDiffUpdateData(files, this.liveTestCase?.files, testcase.order);
            }
        }

        this.liveTestCase = testcase;
    }

    private async fetchDiffUpdateData(nextFiles: LiveTestCase['files'], prevFiles: LiveTestCase['files'], testCaseOrder: number) {
        const nextFilesData: DataFiles = nextFiles ? Object.keys(nextFiles)
            .reduce((files, currFile) => ({ ...files, ...nextFiles[currFile].dataFiles }) , {}) : {};
        const prevFilesData: DataFiles = prevFiles ? Object.keys(prevFiles)
            .reduce((files, currFile) => ({ ...files, ...prevFiles[currFile].dataFiles }) , {}) : {};

        for (let [filePath, index] of Object.entries(nextFilesData)) {
            const prevIndex = typeof prevFilesData[filePath] === 'number' ?
                prevFilesData[filePath] + 1 :
                    Number.MIN_SAFE_INTEGER;

            const fileUpdate = await fetchUpdate<FileUpdate>(
                filePath, 
                [ACTION_JSONP_PATH, MESSAGE_JSONP_PATH],
                index,
                prevIndex,
                testCaseOrder 
            );

            this.handleFileUpdate(fileUpdate, testCaseOrder);
        }
    }

    private async fetchFullUpdateData(dataFiles: LiveTestCase['files'], testCaseOrder: number) {
        if (!dataFiles) return;
        const files: DataFiles = Object.keys(dataFiles)
            .reduce((files, currFile) => ({ ...files, ...dataFiles[currFile].dataFiles }) , {});
        for (let [filePath, currentIndex] of Object.entries(files)) {
            const fileUpdate = await fetchUpdate<FileUpdate>(
                filePath,
                [ACTION_JSONP_PATH, MESSAGE_JSONP_PATH],
                currentIndex,
                Number.MIN_SAFE_INTEGER,
                testCaseOrder
            );
            this.handleFileUpdate(fileUpdate, testCaseOrder);
        }
    }

    private handleFileUpdate(update: FileUpdate, testCaseOrder: number) {
        Object.entries(update).forEach(([path, updatedValues]) => {
            switch (path) {
                case ACTION_JSONP_PATH: {
                    if (updatedValues.length != 0) {
                        this.onActionUpdate(updatedValues as ActionNode[], testCaseOrder);
                    }

                    break;
                }

                case MESSAGE_JSONP_PATH: {
                    if (updatedValues.length != 0) {
                        this.onMessageUpdate(updatedValues as Message[], testCaseOrder);
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
