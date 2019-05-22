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

import * as React from 'react';
import { connect } from 'react-redux';
import Action, { ActionNode, ActionNodeType, isAction } from '../../models/Action';
import { ActionCard } from './ActionCard';
import UserMessage from '../../models/UserMessage';
import Verification from '../../models/Verification'
import Link from '../../models/Link';
import { StatusType } from '../../models/Status';
import UserTable from '../../models/UserTable';
import { CustomMessage } from './CustomMessage';
import Tree, { createNode, mapTree } from '../../models/util/Tree';
import AppState from '../../state/models/AppState';
import { CheckpointAction } from '../Checkpoint';
import { isCheckpoint } from '../../helpers/actionType';
import { selectAction } from '../../actions/actionCreators';
import { selectVerification } from '../../actions/actionCreators';
import VerificationCard from './VerificationCard';
import UserTableCard from './UserTableCard';
import '../../styles/action.scss';
import { raf } from '../../helpers/raf';

interface ActionExpandStatus {
    id: number;
    isExpanded: boolean;
}

interface ActionTreeOwnProps {
    action: ActionNode;
    onExpand: () => void;
}

interface ActionTreeStateProps {
    selectedVerififcationId: number;
    selectedActionsId: number[];
    scrolledActionId: Number;
    actionsFilter: StatusType[];
    expandedTreePath: Tree<number>;
}

interface ActionTreeDispatchProps {
    actionSelectHandler: (action: Action) => any;
    messageSelectHandler: (id: number, status: StatusType) => any;
}

interface ActionTreeProps extends ActionTreeOwnProps, ActionTreeStateProps, ActionTreeDispatchProps {}

interface ActionTreeState {
    expandTree: Tree<ActionExpandStatus>;
}

class ActionTreeBase extends React.Component<ActionTreeProps, ActionTreeState> {

    private treeElements: React.ReactNode[] = [];

    constructor(props: ActionTreeProps) {
        super(props);

        const { action }  = props;

        this.state = {
            expandTree: isAction(action) ? this.getExpandTree(action, props.expandedTreePath) : createNode({ id: null, isExpanded: false })
        }
    }

    getExpandTree(action: Action, treePath: Tree<number>): Tree<ActionExpandStatus> {
        const expandState: ActionExpandStatus = {
            id: action.id,
            isExpanded: treePath && treePath.value === action.id
        }

        if (action.subNodes.length === 0) {
            return createNode(expandState);
        }

        return createNode(
            expandState,
            action.subNodes.map(
                actionNode => isAction(actionNode) ? 
                    this.getExpandTree(actionNode, treePath && treePath.nodes.find(node => node.value === actionNode.id)) : 
                    null
            ).filter(node => node)
        );
    }

    // scrolling to action, selected by url sharing
    componentDidMount() {
        if (!this.treeElements[+this.props.scrolledActionId]) {
            return;
        }

        // https://stackoverflow.com/questions/26556436/react-after-render-code/28748160#comment64053397_34999925
        // At his point (componentDidMount) DOM havn't fully rendered, so, we calling RAF twice:
        // At this point React passed components tree to DOM, however it still could be not redered.
        // First callback will be called before actual render
        // Second callback will be called when DOM is fully rendered.
       raf(() => {
            this.scrollToAction(+this.props.scrolledActionId);
        }, 2);
    }

    componentDidUpdate(prevProps: ActionTreeProps) {
        // reference comparison works here because scrolledActionId is a Number object 
        if (prevProps.scrolledActionId != this.props.scrolledActionId) {
            this.scrollToAction(+this.props.scrolledActionId);
        }
    }



    // shouldComponentUpdate(nextProps: ActionTreeProps) {
    //     if (nextProps.action !== this.props.action) return true;

    //     if (isAction(nextProps.action)) {
    //         const nextAction = nextProps.action;

    //         if (this.props.actionsFilter !== nextProps.actionsFilter) {
    //             return true;
    //         }

    //         // compare current action id and selected action id
    //         return this.shouldActionUpdate(nextAction, nextProps, this.props);
    //     } else {
    //         return true;
    //     }
    // }

    shouldActionUpdate(action: Action, nextProps: ActionTreeProps, prevProps: ActionTreeProps): boolean {

        // hadnling scrolled action change
        if (nextProps.scrolledActionId != prevProps.scrolledActionId && action.id === +nextProps.scrolledActionId) {
            return true;
        }

        // the first condition - current action is selected and we should update to show it
        // the second condition - current action was selected and we should disable selection
        if (nextProps.selectedActionsId != prevProps.selectedActionsId && (
                nextProps.selectedActionsId.includes(action.id) || prevProps.selectedActionsId.includes(action.id))) {
            return true;
        }

        // here we check verification selection
        if (nextProps.selectedVerififcationId !== prevProps.selectedVerififcationId &&
            action.relatedMessages &&
            (action.relatedMessages.some(msgId => msgId == nextProps.selectedVerififcationId || msgId == prevProps.selectedVerififcationId))) {
            return true;
        }

        if (action.subNodes) {
            // if at least one of the subactions needs an update, we update the whole action
            return action.subNodes.some(action => {
                if (isAction(action)) {
                    return this.shouldActionUpdate(action, nextProps, prevProps)
                } else {
                    return false;
                }
            });
        }

        return false;
    }

    componentWillReceiveProps(nextProps: ActionTreeProps, nextState: ActionTreeState) {
        if (nextProps.expandedTreePath !== this.props.expandedTreePath) {
            // this.setState({
            //     expandTree: nextProps.expandedTreePath
            // });
        }
    }

    getSubTree(action: ActionNode, expandTree: Tree<ActionExpandStatus>): Tree<ActionExpandStatus> {
        if (isAction(action)) {
            return expandTree && expandTree.nodes.find(subNode => subNode.value.id == action.id);
        } else {
            return null;
        }
    }

    scrollToAction(actionId: number) {
        if (this.treeElements[actionId]) {
            //this.treeElements[actionId].base.scrollIntoView({ block: 'center' });
        }
    }

    render() {
        return this.renderNode(this.props, true, this.state.expandTree);
    }    

    renderNode(props: ActionTreeProps, isRoot = false, expandTreePath: Tree<ActionExpandStatus> = null, parentAction: Action = null): JSX.Element {
        const { actionSelectHandler, messageSelectHandler, selectedActionsId, selectedVerififcationId: selectedMessageId, actionsFilter, onExpand } = props;

        // https://www.typescriptlang.org/docs/handbook/advanced-types.html#discriminated-unions
        switch (props.action.actionNodeType) {
            case ActionNodeType.ACTION: {
                const { action } = props;

                if (isCheckpoint(action)) {
                    return (
                        <CheckpointAction
                            action={action}/>
                    );
                }

                return (
                    <ActionCard action={action}
                        isSelected={selectedActionsId.includes(action.id)}
                        isTransaparent={!actionsFilter.includes(action.status.status)}
                        onSelect={actionSelectHandler}
                        isRoot={isRoot}
                        isExpanded={expandTreePath.value.isExpanded}
                        onExpand={onExpand}
                        onRootExpand={this.onExpandFor(action.id)}
                        ref={ref => this.treeElements[action.id] = ref}>
                        {
                            action.subNodes ? action.subNodes.map(
                                childAction => this.renderNode(
                                    { ...props, action: childAction },
                                    false, 
                                    this.getSubTree(childAction, expandTreePath),
                                    action
                                )) : null
                        }
                    </ActionCard>
                );
            }

            case ActionNodeType.CUSTOM_MESSAGE: {
                const messageAction = props.action;

                if (!messageAction.message && !messageAction.exception) {
                    return <div/>;
                }

                return (
                    <CustomMessage
                        userMessage={messageAction}
                        parent={parentAction}
                        onExpand={onExpand}/>
                );
            }

            case ActionNodeType.VERIFICATION: {
                const verification = props.action;
                const isSelected = verification.messageId === selectedMessageId;
                const isTransparent = !actionsFilter.includes(verification.status.status);

                return (
                    <VerificationCard
                        key={verification.messageId}
                        verification={verification}
                        isSelected={isSelected}
                        isTransparent={isTransparent}
                        onSelect={messageSelectHandler}
                        parentActionId={parentAction && parentAction.id}
                        onExpand={onExpand}/>
                )
            }

            case ActionNodeType.LINK: {
                const { link } = props.action;

                return (
                    <div className="action-card">
                        <h3>{"Link : " + link}</h3>
                    </div>
                );
            }

            case ActionNodeType.TABLE: {
                const table = props.action;

                return (
                    <UserTableCard
                        table={table}
                        parent={parentAction}
                        onExpand={onExpand}/>
                );
            }

            default: {
                console.warn("WARNING: unknown action node type");
                return <div></div>;
            }
        }
    }

    private onExpandFor = (actionId: number) => (isExpanded: boolean) => {
        this.setState({
            expandTree: mapTree(
                (expandStatus: ActionExpandStatus) => expandStatus.id === actionId ? ({ ...expandStatus, isExpanded }) : expandStatus, 
                this.state.expandTree
            )
        });

        this.props.onExpand && this.props.onExpand();
    }
}

function getExpandedTreePath(actionNode: ActionNode, targetActionsId: number[]): Tree<number> {

    if (!isAction(actionNode)) {
        return null;
    }

    const treeNode = createNode(actionNode.id);

    if (actionNode.subNodes) {
        actionNode.subNodes.forEach(actionSubNode => {
            if (isAction(actionSubNode)) {
                const subNodePath = getExpandedTreePath(actionSubNode, targetActionsId);

                subNodePath && treeNode.nodes.push(subNodePath);
            }
        })
    }

    // checking wheather the current action is the one of target acitons OR some of action's sub nodes is the target aciton
    return targetActionsId.includes(actionNode.id) || treeNode.nodes.length != 0 ? 
            treeNode : 
            null;
}

export const ActionTree = connect(
    (state: AppState, ownProps: ActionTreeOwnProps): ActionTreeStateProps => ({
        selectedVerififcationId: state.selected.verificationId,
        selectedActionsId: state.selected.actionsId,
        scrolledActionId: state.selected.scrolledActionId,
        actionsFilter: state.filter.actionsFilter,
        expandedTreePath: getExpandedTreePath(ownProps.action, state.selected.actionsId)
    }),
    (dispatch, ownProps: ActionTreeOwnProps): ActionTreeDispatchProps => ({
        actionSelectHandler: action => dispatch(selectAction(action)),
        messageSelectHandler: (id, status) => dispatch(selectVerification(id, status))
    })
)(ActionTreeBase);
