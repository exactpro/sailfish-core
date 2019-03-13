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

import { ActionNode, ActionNodeType } from "../models/Action";
import Action from '../models/Action';
import { StatusType } from "../models/Status";

const ACTION_CHECKPOINT_NAME = "GetCheckPoint";

export function getActions(actionNodes: ActionNode[]) : Action[] {
    return actionNodes.reduce((actions, node) => 
        isAction(node) ? [...actions, node as Action] : actions,
        []);
}

export function isAction(actionNode: ActionNode) : boolean {
    return actionNode.actionNodeType == ActionNodeType.ACTION;
}

export function isCheckpoint(action: Action): boolean {
    return action.name.includes(ACTION_CHECKPOINT_NAME);
}

export function getStatusChipDescription(status: StatusType): string {
    const statusFormatted = status.toLowerCase().replace('_', ' '),
        statusCapitalized = statusFormatted.charAt(0).toUpperCase() + statusFormatted.slice(1);

    return `${statusCapitalized} actions count.`;
}