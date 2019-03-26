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

const CHECKPOINT_NAME = 'checkpoint';

export enum MessageType {
    MESSAGE = '',
    CHECKPOINT = 'checkpoint',
    REJECTED = 'rejected',
    ADMIN = 'admin'
}

export function isCheckpoint(message: Message) : boolean {
    const name = message.content['name'] as string;

    if (!name) {
        return false;
    }

    return name.toLowerCase() == CHECKPOINT_NAME;
}

export function isRejected(message: Message) : boolean {
    return message.content.rejectReason !== null;
}

export function isAdmin(message: Message) : boolean {
    return message.content.admin !== false;
}

export function getMessageType(message: Message) : MessageType {
    if (isCheckpoint(message)) {
        return MessageType.CHECKPOINT;
    }

    if (message.content.rejectReason !== null) {
        return MessageType.REJECTED;
    }

    return MessageType.MESSAGE;
}