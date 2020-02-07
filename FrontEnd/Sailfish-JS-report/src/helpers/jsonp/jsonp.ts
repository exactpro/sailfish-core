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
import TestCase from '../../models/TestCase';
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

export async function fetchTestCaseIndexFiles(indexFiles: TestCase['indexFiles']): Promise<TestCase['files']> {
    const filesList = await Promise.all(
        Object.keys(indexFiles)
            .map((key: keyof TestCase['indexFiles']) =>
                fetchJsonp(indexFiles[key], jsonpHandlerNames.index[key])
                    .then(filesInfo => ({[key]: filesInfo}))
            )
    );

    return filesList.reduce((files, currFile) => ({...files, ...currFile}), {});
}

type TestCaseFileData = ActionNode[];
type TestCaseFilesResponse = {
    [jsonpHandlerNames.default]: TestCaseFileData
}

export async function fetchTestCaseFile(filePath: string, fileIndex: number, type: keyof TestCase['files']): Promise<TestCaseFileData> {
    const file = await fetchUpdate<TestCaseFilesResponse>(filePath, [jsonpHandlerNames.files[type]], fileIndex);
    return Object.keys(file).reduce((fileNodes, currFile) => [...fileNodes, ...file[currFile]], []);
}
