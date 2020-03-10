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

/**
 * This function returns map, where key is the action id and value is the action, including nested actions
 * @param actions list of actions
 */
export const generateActionsMap = (actions: Action[]) : Map<number, Action> => {
    const resultMap = new Map<number, Action>();

    actions.forEach(action => appendMapByActionNode(action, resultMap));

    return resultMap;
}

const appendMapByActionNode = (actionNode: ActionNode, actionsMapRef: Map<number, Action>) => {
    if (!isAction(actionNode)) {
        return;
    }

    actionsMapRef.set(actionNode.id, actionNode);

    if (actionNode.subNodes) {
        actionNode.subNodes.forEach(subNode => appendMapByActionNode(subNode, actionsMapRef));
    }
};