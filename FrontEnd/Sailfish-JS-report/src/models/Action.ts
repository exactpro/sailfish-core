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

import ActionParameter from "./ActionParameter"; 
import Status from './Status';
import Verification from './Verification';
import UserMessage from './UserMessage';
import Link from './Link';
import UserTable from "./UserTable";
import Message from './Message';
import Log from './Log';
import KnownBug from "./KnownBug";

export enum ActionNodeType {
    ACTION = "action",
    VERIFICATION = "verification",
    CUSTOM_MESSAGE = "customMessage",
    LINK = "link",
    TABLE = "table",
    KNOWN_BUG = "bug",
    KNOWN_BUG_CATEGORY = "category",
    LOG = "logEntry",
    MESSAGE = "message"
}

export type ActionNode = Action | UserMessage | Verification | Link | UserTable | Message | Log | KnownBug;

export default interface Action {
    id?: number;
    matrixId?: string;
    serviceName?: string;
    actionNodeType: ActionNodeType.ACTION;
    bugs: unknown[];
    name: string;
    messageType: string;
    description: string;
    parameters?: ActionParameter[];
    relatedMessages: number[];
    logs?: unknown;
    startTime?: string;
    finishTime?: string;
    status: Status;
    subNodes?: ActionNode[];
    checkPointId?: number;
    outcome?: string;
    verificationCount: number;
    isTruncated: boolean;
}

export function isAction(action: ActionNode): action is Action {
    return action.actionNodeType === ActionNodeType.ACTION;
}
