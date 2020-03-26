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
import { ACTION_FIELDS, MESSAGE_FIELDS } from "./search/searchEngine";
import Log from "../models/Log";
import KnownBug from "../models/KnownBug";

const ACTION_KEY_PREFIX = 'action',
    MESSAGE_KEY_PREFIX = 'msg',
    VERIFICATION_KEY_PREFIX = 'verification';

export function keyForAction(id: number, fieldName: keyof Action = null): string {
    return `${ACTION_KEY_PREFIX}-${id}` + (fieldName ? `-${fieldName}` : '');
}

export function isKeyForAction(key: string): boolean {
    const [prefix, id, fieldName] = key.split('-');

    return prefix == ACTION_KEY_PREFIX &&
        !isNaN(+id) &&
        (fieldName === undefined || ACTION_FIELDS.includes(fieldName as keyof Action));
}

export function keyForMessage(id: number, fieldName: keyof Message = null): string {
    return `${MESSAGE_KEY_PREFIX}-${id}` + (fieldName ? `-${fieldName}` : '');
}

export function isKeyForMessage(key: string): boolean {
    const [prefix, id, fieldName] = key.split('-');

    return prefix == MESSAGE_KEY_PREFIX &&
        !isNaN(+id) &&
        (fieldName === undefined || MESSAGE_FIELDS.includes(fieldName as keyof Message));
}

export function keyForVerification(parentActionId: number, msgId: number): string {
    return `${ACTION_KEY_PREFIX}-${parentActionId}-${VERIFICATION_KEY_PREFIX}-${msgId}`;
}

export function isKeyForVerification(key: string): boolean {
    const [actionPrefix, actionId, verificationPrefix, msgId] = key.split('-');

    return actionPrefix == ACTION_KEY_PREFIX &&
        !isNaN(+actionId) &&
        verificationPrefix == VERIFICATION_KEY_PREFIX &&
        !isNaN(+msgId);
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

export function keyForActionParameter(actionId: number, index: number): string {
    return `${keyForAction(actionId, 'parameters')}-${index}`;
}

export function keyForLog(index: number, fieldName?: keyof Log): string {
    return `log-${index}` + (fieldName ? `-${fieldName}` : '');
}

export function keyForKnownBug(knownBug: KnownBug, fieldName?: keyof KnownBug): string {
    return `known-bug-${knownBug.id}` + (fieldName ? `-${fieldName}` : '');
}

export function getKeyField(key: string): string | null {
    const [type, id, field] = key.split('-');

    return field ?? null;
}
