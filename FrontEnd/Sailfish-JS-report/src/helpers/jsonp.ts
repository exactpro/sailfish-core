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

import Report, { isReport } from '../models/Report';
import TestCase, { isTestCase } from '../models/TestCase';
import { fetchJsonp } from './files/fetcher';

const JSONP_HANDLER_NAME = "loadJsonp";
const DATA_FOLDER_PATH = "reportData/";
const REPORT_PATH = DATA_FOLDER_PATH + "report.js";

/**
 * Fetches report from jsonp file.
 */
export function fetchReport(): Promise<Report> {
    return fetchJsonp(REPORT_PATH, JSONP_HANDLER_NAME)
        .then((data: Report) => {
            if (isReport(data)) {
                return data;
            } else {
                throw new Error(`Invalid jsonp format at report file (${REPORT_PATH})`);
            }
        });
}

/**
 * Fetches test case from jsonp file.
 * @param testCasePath jsonp filename for TestCase
 */
export function fetchTestCase(testCasePath: string): Promise<TestCase> {
    return fetchJsonp(DATA_FOLDER_PATH + testCasePath, JSONP_HANDLER_NAME)
        .then((data: TestCase) => {
            if (isTestCase(data)) {
                return data;
            } else {
                throw new Error(`Invalid jsonp format at test case file (${testCasePath})`);
            }
        });
}
