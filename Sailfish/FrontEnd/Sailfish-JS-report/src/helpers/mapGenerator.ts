import Action, { ActionNode } from '../models/Action';

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
    if (actionNode.actionNodeType !== 'action') {
        return;
    }

    const action = actionNode as Action;
    actionsMapRef.set(action.id, action);

    if (action.subNodes) {
        action.subNodes.forEach(subNode => appendMapByActionNode(subNode, actionsMapRef));
    }
}