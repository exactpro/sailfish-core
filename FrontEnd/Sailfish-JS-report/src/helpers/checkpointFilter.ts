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

import Action from '../models/Action';

const ACTION_CHECKPOINT_NAME = "GetCheckPoint";

export function isCheckpoint(action: Action): boolean {
    return action.name.includes(ACTION_CHECKPOINT_NAME);
}

export function getCheckpointActions(actions: Action[]) {
    return actions.reduce((checkpoints, action) => [...checkpoints, ...getActionCheckpoints(action)], []);
}

function getActionCheckpoints(action: Action, checkpoints: Action[] = []): Action[]  {
    if (isCheckpoint(action)) {
        return [...checkpoints, action];
    }

    return action.subNodes.reduce((checkpoints, subNode) => {
        if (subNode.actionNodeType == 'action') {
            return [...checkpoints, ...getActionCheckpoints(subNode as Action, checkpoints)];
        } else {
            return checkpoints;
        }
    }, [])
}
