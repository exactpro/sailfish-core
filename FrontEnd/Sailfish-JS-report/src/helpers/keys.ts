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
import Action from '../models/Action';
import UserMessage, { isUserMessage } from '../models/UserMessage';
import UserTable, { isUserTable } from '../models/UserTable';

export function keyForAction(id: number, fieldName: keyof Action = null): string {
    return `action-${id}` + (fieldName ? `-${fieldName}` : '');
}

export function keyForMessage(id: number, fieldName: keyof Message = null): string {
    return `msg-${id}` + (fieldName ? `-${fieldName}` : '');
}

export function keyForVerification(parentActionId: number, msgId: number): string {
    return `action-${parentActionId}-verification-${msgId}`;
}

export function keyForUserMessage(userMessage: UserMessage, parent: Action): string {
    const index = parent.subNodes ? (
        parent.subNodes
            .filter(isUserMessage)
            .indexOf(userMessage)
    ) : '';

    return `${parent.id}-user_message-${index}`;
}

export function keyForUserTable(table: UserTable, parent: Action): string {
    const index = parent.subNodes ? (
        parent.subNodes
            .filter(isUserTable)
            .indexOf(table)
    ) : '';

    return `${parent.id}-user_table-${index}`;
}

export function keyForActionParamter(actionId: number, index: number): string {
    return `${keyForAction(actionId, 'parameters')}-${index}`;
}
