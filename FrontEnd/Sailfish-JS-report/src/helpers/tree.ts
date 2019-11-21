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

import Action, { isAction, ActionNode } from '../models/Action';
import Tree, { createNode } from '../models/util/Tree';
import ActionExpandStatus from '../models/util/ActionExpandStatus';

export function createExpandTree(action: Action, treePath: Tree<number>): Tree<ActionExpandStatus> {
    const expandState: ActionExpandStatus = {
        id: action.id,
        isExpanded: treePath ? treePath.value === action.id : false
    }

    if (action.subNodes.length === 0) {
        return createNode(expandState);
    }

    return createNode(
        expandState,
        action.subNodes.map(
            actionNode => isAction(actionNode) ? 
                createExpandTree(actionNode, treePath && treePath.nodes.find(node => node.value === actionNode.id)) : 
                null
        ).filter(node => node)
    );
}

export function updateExpandTree(tree: Tree<ActionExpandStatus>, nextPath: Tree<number> | null) {
    if (!nextPath) {
        return tree;
    }

    const nextState: ActionExpandStatus = {
        id: tree.value.id,
        isExpanded: nextPath.value === tree.value.id ? true : tree.value.isExpanded
    }

    if (tree.nodes.length === 0 || nextPath.nodes.length === 0) {
        return createNode(nextState, tree.nodes);
    }

    return createNode(
        nextState,
        tree.nodes.map(node => updateExpandTree(node, nextPath.nodes.find(({ value }) => value === node.value.id)))
    );
}

export function getSubTree(action: ActionNode, expandTree: Tree<ActionExpandStatus>): Tree<ActionExpandStatus> {
    return isAction(action) ?
        expandTree && expandTree.nodes.find(({ value }) => value.id == action.id) : 
        null;
}

export function createExpandTreePath(actionNode: ActionNode, targetActionsId: number[]): Tree<number> {

    if (!isAction(actionNode)) {
        return null;
    }

    const treeNode = createNode(actionNode.id);

    if (actionNode.subNodes) {
        actionNode.subNodes.forEach(actionSubNode => {
            if (isAction(actionSubNode)) {
                const subNodePath = createExpandTreePath(actionSubNode, targetActionsId);

                subNodePath && treeNode.nodes.push(subNodePath);
            }
        })
    }

    // checking wheather the current action is the one of target acitons OR some of action's sub nodes is the target aciton
    return targetActionsId.includes(actionNode.id) || treeNode.nodes.length != 0 ? 
            treeNode : 
            null;
}
