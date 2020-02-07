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

import TestCase from "./TestCase";
import { TestCaseMetadata } from "./TestcaseMetadata";
import { ReportProperties } from "./ReportProperties";
import Exception from "./Exception";
import Alert from "./Alert";

export default interface Report {
    alerts?: Alert[];
    startTime: string;
    finishTime: string;
    plugins: any;
    testCases?: TestCase[];
    bugs: any[];
    hostName: string;
    userName: string;
    name: string;
    scriptRunId: number;
    version: string;
    branchName: string;
    description: string;
    exception?: Exception;
    outcomes?: any;
    reportProperties?: ReportProperties;
    metadata: TestCaseMetadata[];
    precision: string;
    tags?: string[];
}

export function isReport(report: TestCase | Report): report is Report {
    return (report as Report).metadata !== undefined;
}
