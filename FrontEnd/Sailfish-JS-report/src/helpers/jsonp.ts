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

const JSONP_HANDLER_NAME = "loadJsonp";
const DATA_FOLDER_PATH = "reportData/";
const REPORT_PATH = DATA_FOLDER_PATH + "report.js";

/**
 * Creates script tag that triggers jsonp load for report
 */
export function fetchReport(): Promise<Report> {
    return new Promise<Report>((resolve, reject) => {
        window[JSONP_HANDLER_NAME] = (jsonp: Report) => {
            // reset handler
            window[JSONP_HANDLER_NAME] = undefined;

            if (isReport(jsonp)) {
                resolve(jsonp);
            } else {
                reject(new Error("Wrong jsonp format."))
            }
        }

        loadJsonp(REPORT_PATH);
    })
}

/**
 * Creates script tag that triggers jsonp load for report
 * @param testCasePath jsonp filename for TestCase
 */
export function fetchTestCase(testCasePath: string): Promise<TestCase> {
    return new Promise<TestCase>((resolve, reject) => {
        window[JSONP_HANDLER_NAME] = (jsonp: TestCase) => {
            window[JSONP_HANDLER_NAME] = undefined;

            if (isTestCase(jsonp)) {
                resolve(jsonp);
            } else {
                reject(new Error("Wrong jsonp format."));
            }
        }

        loadJsonp(DATA_FOLDER_PATH + testCasePath);
    })
}

function loadJsonp(path: string) {
    const jsonpLoader = <HTMLScriptElement>document.createElement('script');
    
    jsonpLoader.src = path;
    jsonpLoader.async = true;

    jsonpLoader.onload = () => {
        document.body.removeChild(jsonpLoader);
    }

    document.body.appendChild(jsonpLoader);
}
