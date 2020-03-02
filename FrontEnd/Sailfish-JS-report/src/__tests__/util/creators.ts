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
import SearchToken from "../../models/search/SearchToken";
import KnownBug, { KnownBugNode } from "../../models/KnownBug";
import { KnownBugStatus } from "../../models/KnownBugStatus";
import KnownBugCategory from "../../models/KnownBugCategory";

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
        finishTime: new Date().toString()
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

export function createSearchToken(pattern = 'test', color = 'default', isActive = false): SearchToken {
    return {
        pattern,
        color,
        isActive
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
