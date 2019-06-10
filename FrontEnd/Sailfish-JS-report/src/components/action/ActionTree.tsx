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
import { connect, Omit } from 'react-redux';
import '../../styles/action.scss';
import Action, { ActionNode, ActionNodeType, isAction } from '../../models/Action';
import { StatusType } from '../../models/Status';
import Tree, { createNode, mapTree } from '../../models/util/Tree';
import AppState from '../../state/models/AppState';
import ActionExpandStatus from '../../models/util/ActionExpandStatus';
import { ActionCard } from './ActionCard';
import { CustomMessage } from './CustomMessage';
import { CheckpointAction } from '../Checkpoint';
import { selectAction } from '../../actions/actionCreators';
import { selectVerification } from '../../actions/actionCreators';
import VerificationCard from './VerificationCard';
import UserTableCard from './UserTableCard';
import StateSaver from '../util/StateSaver';
import memoize from '../../helpers/memoize';
import { raf } from '../../helpers/raf';
import { isCheckpoint } from '../../helpers/actionType';
import { createExpandTreePath, createExpandTree, getSubTree } from '../../helpers/tree';

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

interface ActionTreeContainerProps extends ActionTreeOwnProps, ActionTreeStateProps, ActionTreeDispatchProps {}

// Omit is just omitting property from interface
type ActionTreeProps = Omit<ActionTreeContainerProps, 'expandedTreePath'> & { 
    expandState: Tree<ActionExpandStatus>,
    saveState: (state: Tree<ActionExpandStatus>) => any; 
}

/**
 * This component use context to save action's expand status.
 * We can't use state for this, because the state is destroyed after each unmount due virtualization.
 */
class ActionTreeBase extends React.PureComponent<ActionTreeProps> {

    private treeElements: React.ReactNode[] = [];

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

        if (prevProps.expandState !== this.props.expandState) {
           this.props.onExpand();
        }
    }

    scrollToAction(actionId: number) {
        if (this.treeElements[actionId]) {
            //this.treeElements[actionId].base.scrollIntoView({ block: 'center' });
        }
    }

    render() {
        return this.renderNode(this.props, true, this.props.expandState);
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
                                    getSubTree(childAction, expandTreePath),
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
        this.props.saveState(mapTree(
            (expandStatus: ActionExpandStatus) => expandStatus.id === actionId ? ({ ...expandStatus, isExpanded }) : expandStatus, 
            this.props.expandState
        ));
    }
}

type ActionTreeRecoveredState = [Tree<ActionExpandStatus>, Tree<number>];

/**
 * State saver for ActionTree component. Responsible for creating default state value and updating state in context.
 * @param props 
 */
const RecoverableActionTree = (props: ActionTreeContainerProps) => {
    const stateKey = 'action-' + (isAction(props.action) ? props.action.id : props.action.actionNodeType),
        defaultState: ActionTreeRecoveredState = [
            isAction(props.action) ? 
                createExpandTree(props.action, props.expandedTreePath) : 
                createNode({ id: null, isExpanded: false }),
            null
        ];

    return (
        <StateSaver 
            stateKey={stateKey}
            defaultState={defaultState}>
            {([expandState, lastTreePath]: ActionTreeRecoveredState, stateSaver: (state: ActionTreeRecoveredState) => any) => (
                <ActionTreeBase
                    {...props}
                    expandState={expandState}
                    saveState={nextExpandState => stateSaver([nextExpandState, lastTreePath])}/>
            )}
        </StateSaver>
    )
}

// we need memoization here not to recalculate expand tree for each virtualized render call
const getExpandedTreePath = memoize(
    createExpandTreePath, 
    (actionNode: ActionNode) => isAction(actionNode) ? actionNode.id.toString() : 'not-action'
);

export const ActionTree = connect(
    (state: AppState, ownProps: ActionTreeOwnProps): ActionTreeStateProps => ({
        selectedVerififcationId: state.selected.verificationId,
        selectedActionsId: state.selected.actionsId,
        scrolledActionId: state.selected.scrolledActionId,
        actionsFilter: state.filter.actionsFilter,
        expandedTreePath: getExpandedTreePath(ownProps.action, [...state.selected.actionsId, +state.selected.scrolledActionId])
    }),
    (dispatch, ownProps: ActionTreeOwnProps): ActionTreeDispatchProps => ({
        actionSelectHandler: action => dispatch(selectAction(action)),
        messageSelectHandler: (id, status) => dispatch(selectVerification(id, status))
    })
)(RecoverableActionTree);
