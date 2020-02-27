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
import { isEqual, getObjectKeys } from "../object";
import Report from '../../models/Report';
import TestCase, { TestCaseFiles } from '../../models/TestCase';
import Log from '../../models/Log';

const STUB_CALLBACK = () => {};

const ACTION_JSONP_PATH = jsonpHandlerNames.files.action,
    MESSAGE_JSONP_PATH = jsonpHandlerNames.files.message,
    LOG_JSON_PATH = jsonpHandlerNames.files.logentry;

const ROOT_WATCH_INTERVAL = 1500;
const TESTCASE_WATCH_INTERVAL = 1500;

const EMPTY_LOG_FILES: TestCaseFiles['logentry'] = {
    count: 0,
    dataFiles: {},
    lastUpdate: ''
};

type FileUpdate = {
    [ACTION_JSONP_PATH]: ActionNode[],
    [MESSAGE_JSONP_PATH]: Message[]
}

type ActionUpdate = (actions: ActionNode[], testCaseOrder: number) => unknown;
type MessageUpdate = (messages: Message[], testCaseOrder: number) => unknown;
type LogsUpdate = (logs: Log[], testCaseOrder: number) => unknown;
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
    private onLogsUpdate: LogsUpdate = STUB_CALLBACK;
    private onTestCaseUpdate: TestCaseUpdate = STUB_CALLBACK;
    private onReportUpdate: ReportUpdate = STUB_CALLBACK;
    private onReportFinish: ReportFinish = STUB_CALLBACK;
    private onError: () => void = STUB_CALLBACK;
    private isLoading = false;
    private watchLogs = false;
    private loadedFiles: TestCaseFiles | null = null;

    public set setOnActionUpdate(cb: ActionUpdate) {
        this.onActionUpdate = cb;
    }

    public set setOnMessageUpdate(cb: MessageUpdate) {
        this.onMessageUpdate = cb;
    }

    public set setOnLogsUpdate(cb: LogsUpdate) {
        this.onLogsUpdate = cb;
    }

    public set setOnReportUpdate(cb: ReportUpdate) {
        this.onReportUpdate = cb;
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
            this.onReportDataUpdate,
        );
    }

    public stopWatchingReport() {
        clearInterval(this.reportWatchIntervalId);
        this.report = null;
    }

    public startWatchingLogs(){
        this.watchLogs = true;
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
            TESTCASE_WATCH_INTERVAL, 
            this.onTestCaseDataUpdate
        );
    }

    public stopWatchingTestcase() {
        clearInterval(this.testCaseWatchIntervalId);
        this.liveTestCase = null;
        this.loadedFiles = null;
        this.isLoading = false;
        this.watchLogs = false;
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
        const { files, order } = testcase;

        if (!this.liveTestCase) {
            this.fetchFullUpdateData(files, order);
        } else if (!isEqual(files, this.liveTestCase.files)) {
            this.fetchDiffUpdateData(files, this.loadedFiles, order);
        }

        this.onTestCaseUpdate(testcase);
        this.liveTestCase = testcase;
    }

    private async fetchFullUpdateData(files: TestCaseFiles | null, testCaseOrder: number) {
        if (!files) return;
        this.isLoading = true;
        const filesToLoad: TestCaseFiles = {
            ...files,
            logentry: this.watchLogs ? files.logentry : EMPTY_LOG_FILES
        };
        await Promise.all(
            getObjectKeys(filesToLoad)
                .filter(fileType => filesToLoad[fileType].count > 0)
                .map(async fileType => {
                    for (const [filePath, currentIndex] of Object.entries(filesToLoad[fileType].dataFiles)) {
                        try {
                            await this.fetchFileUpdate(
                                filePath,
                                jsonpHandlerNames.files[fileType],
                                currentIndex,
                                Number.MIN_SAFE_INTEGER,
                                testCaseOrder
                            );
                        } catch (error) {
                            if (error.isCancelled) {
                                break;
                            } else {
                                this.onError();
                            }
                        }
                    }
                })
        );
        this.loadedFiles = filesToLoad;
        this.isLoading = false;
        this.checkFilesForUpdates(this.liveTestCase.files, this.loadedFiles, testCaseOrder);
    }

    private async fetchDiffUpdateData(nextFiles: TestCaseFiles, prevFiles: TestCaseFiles, testCaseOrder: number) {
        if (this.isLoading) return;
        this.isLoading = true;
        const filesToLoad: TestCaseFiles = {
            ...nextFiles,
            logentry: this.watchLogs ? nextFiles.logentry : EMPTY_LOG_FILES
        };
        await Promise.all(
            getObjectKeys(filesToLoad)
                .filter(fileType => filesToLoad[fileType].count > 0)
                .map(async fileType => {
                    for (const [filePath, index] of Object.entries(filesToLoad[fileType].dataFiles)) {
                        if (filesToLoad[fileType].dataFiles[filePath] == prevFiles[fileType].dataFiles[filePath]) continue;
                        try {
                            const prevIndex = typeof prevFiles[fileType].dataFiles[filePath] === 'number'
                                ? prevFiles[fileType].dataFiles[filePath] + 1
                                : Number.MIN_SAFE_INTEGER;
                            await this.fetchFileUpdate(
                                filePath,
                                jsonpHandlerNames.files[fileType],
                                index,
                                prevIndex,
                                testCaseOrder
                            );
                        } catch (error) {
                            if (error.isCancelled) {
                                break;
                            } else {
                                this.onError();
                            }
                        }
                    }
                })
        );
        this.loadedFiles = filesToLoad;
        this.isLoading = false;
        this.checkFilesForUpdates(this.liveTestCase.files, this.loadedFiles, testCaseOrder);
    }

    private async fetchFileUpdate(filePath: string, jsonpHandlerName: string, index: number, prevIndex: number, testCaseOrder: number) {
        const fileUpdate = await fetchUpdate<FileUpdate>(
            filePath, 
            [jsonpHandlerName],
            index,
            prevIndex,
            testCaseOrder 
        );
        this.handleFileUpdate(fileUpdate, testCaseOrder);
    }

    private async checkFilesForUpdates(nextFiles: TestCaseFiles, prevFiles: TestCaseFiles, testCaseOrder: number){
        if (!this.liveTestCase || this.liveTestCase.order !== testCaseOrder) return;
        const currentFiles: TestCaseFiles = {
            ...nextFiles,
            logentry: this.watchLogs ? nextFiles.logentry : EMPTY_LOG_FILES
        };
        if (!prevFiles || !isEqual(currentFiles, prevFiles)) {
            this.fetchDiffUpdateData(currentFiles, prevFiles, this.liveTestCase.order);
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

                case LOG_JSON_PATH: {
                    if (updatedValues.length != 0) {
                        this.onLogsUpdate(updatedValues as Log[], testCaseOrder);
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
