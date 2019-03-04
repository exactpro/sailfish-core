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

import ActionParameter from "./ActionParameter"; 
import Status from './Status';
import Verification from './Verification';
import Exception from './Exception';
import MessageAction from './MessageAction';
import Link from './Link';

export enum ActionNodeType {
    ACTION = "action",
    VERIFICATION = "verification",
    CUSTOM_MESSAGE = "customMessage",
    LINK = "link"
}

export type ActionNode = Action | MessageAction | Verification | Link;

export default interface Action {
    id?: number;
    actionNodeType: ActionNodeType;
    bugs: any[];
    name: string;
    description: string;
    parameters?: ActionParameter[];
    relatedMessages: number[];
    logs?: any;
    startTime?: string;
    finishTime?: string;
    status: Status;
    subNodes?: ActionNode[];
    checkPointId?: number;
}