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

import Status from "./Status";
import KnownBug from "./KnownBug";
import KnownBugCategory from "./KnownBugCategory"
import LiveTestCase from "./LiveTestCase";

export interface TestCaseMetadata {
    order: number;
    startTime: string;
    finishTime: string;
    name: string;
    status: Status;
    id: string;
    hash: number;
    description: string;
    jsonFileName: string;
    jsonpFileName: string;
    bugs: (KnownBug | KnownBugCategory) [];
    firstActionId: number;
    lastActionId: number;
    failedActionCount: number;
}

export function isTestCaseMetadata(testCase: TestCaseMetadata | LiveTestCase): testCase is TestCaseMetadata {
    return testCase['finishTime'] != null && testCase['status'] != null;
}
