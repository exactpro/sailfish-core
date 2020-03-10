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

import Action, { ActionNode, ActionNodeType } from "../../models/Action";
import TestCase from "../../models/TestCase";
import Message from "../../models/Message";
import { StatusType } from "../../models/Status";
import ActionParameter from "../../models/ActionParameter";
import Verification from "../../models/Verification";
import VerificationEntry from "../../models/VerificationEntry";
import SearchToken, { PanelSearchToken } from "../../models/search/SearchToken";
import KnownBug, { KnownBugNode } from "../../models/KnownBug";
import { KnownBugStatus } from "../../models/KnownBugStatus";
import KnownBugCategory from "../../models/KnownBugCategory";
import Report from '../../models/Report';
import { TestCaseMetadata } from '../../models/TestcaseMetadata';
import SearchSplitResult from "../../models/search/SearchSplitResult";
import Panel from "../../util/Panel";

export function createAction(
    id: number = 0, 
    subNodes: ActionNode[] = [], 
    name = '', 
    parameters: ActionParameter[] = [], 
    status: StatusType = StatusType.PASSED
): Action {

    return {
        actionNodeType: ActionNodeType.ACTION,
        name,
        id,
        subNodes,
        parameters,
        bugs: [],
        description: '',
        messageType: '',
        relatedMessages: [],
        isTruncated: false,
        verificationCount: 0,
        status: { status }
    };
}

export function createVerification(messageId: number = 0, name: string = '', status: StatusType = StatusType.PASSED): Verification {
    return {
        actionNodeType: ActionNodeType.VERIFICATION,
        messageId,
        description: '',
        entries: [],
        name,
        status: { status }
    }
}

export function createVerificationEntry(name = '', actual = '', expected = '', status = StatusType.PASSED): VerificationEntry {
    return {
        name,
        actual, 
        expected,
        status,
        actualType: '',
        expectedType: '',
        hint: null
    }
}

export function createTestCase(id: string = '0', actions: ActionNode[] = [], messages: Message[] = [], status: StatusType = StatusType.PASSED): TestCase {
    return {
        actionNodeType: 'testCase',
        id, 
        actions,
        messages,
        logs: [],
        bugs: [],
        type: '',
        order: 0,
        matrixOrder: 0,
        description: '',
        hash: 0,
        status: { status },
        startTime: new Date().toString(),
        finishTime: new Date().toString(),
        hasErrorLogs: false,
        hasWarnLogs: false
    }
}

export function createMessage(id: number = 0, msgName: string = 'test'): Message {
    return {
        actionNodeType: ActionNodeType.MESSAGE,
        id, 
        msgName,
        raw: '',
        relatedActions: [],
        from: '',
        to: '',
        content: {},
        contentHumanReadable: '',
        timestamp: new Date().toString()
    }
}

export function createSearchToken(
    pattern = 'test',
    color = 'default',
    isActive = false,
    isScrollable = true,
    panels: Panel[] = [Panel.ACTIONS, Panel.MESSAGES, Panel.KNOWN_BUGS, Panel.LOGS]
): PanelSearchToken {
    return {
        pattern,
        color,
        isActive,
        isScrollable,
        panels
    }
}

export function createSearchSplitResult(content: string = '', token: SearchToken | null = null): SearchSplitResult {
    return {
        content,
        token
    }
}

export function createKnownBug(id: number = 0, subject: string = '', relatedActionIds: number[] = []): KnownBug {
    return {
        actionNodeType: ActionNodeType.KNOWN_BUG,
        id,
        subject,
        relatedActionIds,
        status: KnownBugStatus.REPRODUCED
    }
}

export function createKnownBugCategory(name: string = '', subNodes: KnownBugNode[] = []): KnownBugCategory {
    return {
        actionNodeType: ActionNodeType.KNOWN_BUG_CATEGORY,
        name,
        subNodes
    }
}

export function createReport(
    startTime: string, 
    finishTime: string | null, 
    metadata: TestCaseMetadata[] = [],
    ): Report {
    return {
        branchName: '',
        bugs: [],
        description: '',
        startTime,
        finishTime,
        hostName: '',
        metadata,
        name: '',
        plugins: [],
        precision: '',
        scriptRunId: 1,
        userName: '',
        version: '',
        alerts: [],
        exception: null,
        outcomes: null,
        reportProperties: null,
        tags: [],
        testCases: [],
    }
}

export function createTestCaseMetadata(
    order: number = 1,
    finishTime: string | null,
    hash: number
): TestCaseMetadata {
    return {
        order,
        startTime: new Date().toString(),
        finishTime,
        name: '',
        status: null,
        id: order.toString(),
        hash,
        description: '',
        jsonFileName: '',
        jsonpFileName: '',
        bugs: [],
        failedActionCount: 0,
        firstActionId: 0,
        lastActionId: 0
    }
}
