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

import Report, { isReport } from '../../models/Report';
import TestCase, { TestCaseIndexFiles, TestCaseFiles } from '../../models/TestCase';
import { fetchJsonp, fetchUpdate } from '../files/fetcher';
import { ActionNode } from '../../models/Action';

const DATA_FOLDER_PATH = "reportData/jsonp/";
const REPORT_PATH = DATA_FOLDER_PATH + "report.js";

export const jsonpHandlerNames = (<const>{
    index: {
        message: 'loadMessageIndex',
        action: 'loadActionIndex',
        logentry: 'loadLogEntryIndex'
    },
    files: {
        message: 'loadMessage',
        action: 'loadAction',
        logentry: 'loadLogEntry'
    },
    testCase: 'loadTestCase',
    default: 'loadJsonp'
})

/**
 * Fetches report from jsonp file.
 */
export async function fetchReport(): Promise<Report> {
    const data = await fetchJsonp(REPORT_PATH, jsonpHandlerNames.default);
    if (isReport(data as Report)) {
        return data as Report;
    }
    throw new Error(`Invalid jsonp format at report file (${REPORT_PATH})`);
}

/**
 * Fetches test case from jsonp file.
 * @param testCasePath jsonp filename for TestCase
 */
export async function fetchTestCase(testCasePath: string): Promise<TestCase> {
    const testCase = await fetchTestCaseBody(testCasePath);
    const testCaseFiles = await fetchTestCaseIndexFiles(testCase.indexFiles);

    return {
        ...testCase,
        files: testCaseFiles
    }
}

export async function fetchTestCaseBody(testCasePath: string): Promise<Omit<TestCase, "files">>{
    const testcase = await fetchJsonp(testCasePath, jsonpHandlerNames.testCase);
    return testcase as Omit<TestCase, "files">;
}

export async function fetchTestCaseIndexFiles(indexFiles: TestCaseIndexFiles): Promise<TestCaseFiles> {
    const filesList = await Promise.all(
        Object.entries(indexFiles)
            .map(([type, link]: [keyof TestCaseIndexFiles, string]) =>
                fetchJsonp(link, jsonpHandlerNames.index[type])
                    .then(filesInfo => ({[type]: filesInfo}))
            )
    );

    return filesList.reduce((files, currFile) => ({...files, ...currFile}), {});
}

type TestCaseFileData = ActionNode[];
type TestCaseFileHandlerNames = 'loadMessage' | 'loadAction' | 'loadLogEntry';
type TestCaseFilesResponse = {
    [key in TestCaseFileHandlerNames]: TestCaseFileData
}

export async function fetchTestCaseFile(
    filePath: string,
    fileIndex: number,
    type: keyof TestCaseFiles,
    testCaseOrder: number,
): Promise<TestCaseFileData> {
    const jsonpHandlerName = jsonpHandlerNames.files[type];
    const file = await fetchUpdate<TestCaseFilesResponse>(
        filePath,
        [jsonpHandlerName],
        fileIndex,
        Number.MIN_SAFE_INTEGER,
        testCaseOrder,
    );

    return file[jsonpHandlerName];
}

export type FileWatchEvent = {
    type: 'data',
    data: unknown
} | {
    type: 'error',
    err?: Error
}

export type FileWatchCallback = (e: FileWatchEvent) => unknown;

/**
 * This async function can be used to watch to changes in report. 
 * It retuns Promise, that resolves to true when file exists, otherwise it resolves to false. 
 * @param interval interval for check updates of the test case files.
 * @param cb Watch callback - it recieves event of 2 types: 
 *  data - fires each time when file was loaded,
 *  error - fires when something goes wrong, e.g. file was not found.
 * @returns interval id.
 */
export function watchReport(interval: number, cb: FileWatchCallback) {
    async function _watchReport(){
        try {
            const report = await fetchReport();
            cb({ type: 'data', data: report });
        } catch (err) {
            cb({ type: 'error', err });
        }
    };
    _watchReport();
    return setInterval(() => {
        _watchReport();
    }, interval);
};

/**
 * This async function can be used to watch to changes in live test case. 
 * It retuns Promise, that resolves to true when file exists, otherwise it resolves to false. 
 * @param filePath path to target jsonp file.
 * @param interval interval for check updates of the test case files.
 * @param cb Watch callback - it recieves event of 2 types: 
 *  data - fires each time when file was loaded,
 *  error - fires when something goes wrong, e.g. file was not found.
 * @returns interval id.
 */
export function watchLiveTestCase(filePath: string, interval: number, cb: FileWatchCallback) {
    async function _watchLiveTestCase(){
        try {
            const testCase = await fetchTestCase(filePath);
            cb({ type: 'data', data: testCase });
        } catch (err) {
            cb({ type: 'error', err });
        }
    };
    _watchLiveTestCase();
    return setInterval(() => {
        _watchLiveTestCase();
    }, interval);
};
