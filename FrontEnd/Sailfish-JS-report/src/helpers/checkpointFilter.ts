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

import Action, { ActionNode, isAction } from '../models/Action';
import { isCheckpointAction } from './action';

export function getCheckpointActions(actions: ActionNode[]) {
    return actions.reduce(
        (checkpoints, action) => [...checkpoints, ...getActionCheckpoints(action)],
        new Array<Action>()
    );
}

function getActionCheckpoints(actionNode: ActionNode, checkpoints: Action[] = []): Action[]  {
    if (!isAction(actionNode)) {
        return checkpoints;
    }

    if (isCheckpointAction(actionNode)) {
        return [...checkpoints, actionNode];
    }

    return actionNode.subNodes.reduce((checkpoints, subNode) => {
        if (isAction(subNode)) {
            return [...checkpoints, ...getActionCheckpoints(subNode, checkpoints)];
        } else {
            return checkpoints;
        }
    }, [])
}
