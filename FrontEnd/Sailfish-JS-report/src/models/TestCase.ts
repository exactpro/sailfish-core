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

import Action, { ActionNode } from "./Action";
import Message from "./Message";
import Log from "./Log";
import Status from './Status';

export default interface TestCase {
    actionNodeType: string;
    name?: string;
    actions: ActionNode[];
    logs: Log[];
    messages: Message[];
    bugs: any[];
    type: string;
    reference?: any;
    order: number;
    outcomes?: any[];
    matrixOrder: number;
    id: string;
    hash: number;
    description: string;
    status: Status;
    startTime: string;
    finishTime: string;
    verifications?: any[];
}