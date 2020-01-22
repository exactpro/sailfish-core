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
import Message from '../models/Message';
import { StatusType } from '../models/Status';
import Action from '../models/Action';
import { TestCaseMetadata, isTestCaseMetadata } from '../models/TestcaseMetadata';
import LiveTestCase from '../models/LiveTestCase';

export function messagesHeatmap(messages: Message[], selectedMessages: number[], selectedStatus: StatusType): Map<number, StatusType> {
    const heatmap = new Map<number, StatusType>();

    messages.forEach((message, idx) => {
        if (selectedMessages.includes(message.id)) {
            heatmap.set(idx, selectedStatus ?? StatusType.NA);
        }
    });

    return heatmap;
}

export function actionsHeatmap(actions: Action[], selectedActionsId: number[]): Map<number, StatusType> {
    const heatmap = new Map<number, StatusType>();

    actions.forEach((action, idx) => {
        if (selectedActionsId.includes(action.id)) {
            heatmap.set(idx, action.status.status);
        }
    });

    return heatmap;
}

export function testCasesHeatmap(testCases: (TestCaseMetadata | LiveTestCase)[]): Map<number, StatusType> {
    const heatmap = new Map<number, StatusType>();

    testCases.forEach((metadata, idx) => {
        // skip only passed testcases on heatmap
        if (isTestCaseMetadata(metadata) && metadata.status.status != StatusType.PASSED) {
            heatmap.set(idx, metadata.status.status);
        }
    });

    return heatmap;
}
