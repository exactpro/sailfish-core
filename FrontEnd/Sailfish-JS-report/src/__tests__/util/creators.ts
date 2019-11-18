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

import Action, { ActionNode, ActionNodeType } from "../../models/Action";
import TestCase from "../../models/TestCase";
import Message from "../../models/Message";
import { StatusType } from "../../models/Status";
import ActionParameter from "../../models/ActionParameter";

export function createAction(id: number = 0, subNodes: ActionNode[] = [], name = '', parameters: ActionParameter[] = []): Action {
    return {
        actionNodeType: ActionNodeType.ACTION,
        name,
        id,
        subNodes,
        parameters
    } as Action;
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
        actionNodeType: 'message',
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
