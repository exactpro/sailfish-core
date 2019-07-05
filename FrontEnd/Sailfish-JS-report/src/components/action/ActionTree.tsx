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
import { isCheckpoint } from '../../helpers/actionType';
import { createExpandTreePath, createExpandTree, getSubTree, updateExpandTree } from '../../helpers/tree';
import { keyForAction } from '../../helpers/keys';

interface OwnProps {
    action: ActionNode;
    onExpand: () => void;
}

interface StateProps {
    selectedVerififcationId: number;
    selectedActionsId: number[];
    scrolledActionId: Number;
    actionsFilter: StatusType[];
    expandedTreePath: Tree<number>;
}

interface DispatchProps {
    actionSelectHandler: (action: Action) => any;
    messageSelectHandler: (id: number, status: StatusType) => any;
}

interface ContainerProps extends OwnProps, StateProps, DispatchProps {}

type Props = ContainerProps & { 
    expandState: Tree<ActionExpandStatus>,
    saveState: (state: Tree<ActionExpandStatus>) => any; 
}

interface State {
    expandTree: Tree<ActionExpandStatus>;
    lastTreePath: Tree<number>;
}

/**
 * This component use context to save action's expand status.
 * We can't use state for this, because the state is destroyed after each unmount due virtualization.
 */
class ActionTreeBase extends React.PureComponent<Props, State> {

    constructor(props: Props) {
        super(props);

        this.state = {
            expandTree: props.expandState,
            lastTreePath: null
        };
    }

    static getDerivedStateFromProps(props: Props, state: State): State {
        if (props.expandedTreePath !== state.lastTreePath) {
            return {
                expandTree: updateExpandTree(state.expandTree, props.expandedTreePath),
                lastTreePath: props.expandedTreePath
            }
        }

        return null;
    }

    componentDidUpdate(prevProps: Props, prevState: State) {
        if (prevState.expandTree !== this.state.expandTree) {
           this.props.onExpand();
        }
    }

    componentWillUnmount() {
        this.props.saveState(this.state.expandTree);
    }

    render() {
        return this.renderNode(this.props, true, this.state.expandTree);
    }    

    renderNode(props: Props, isRoot = false, expandTreePath: Tree<ActionExpandStatus> = null, parentAction: Action = null): JSX.Element {
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
                        onRootExpand={this.onExpandFor(action.id)}>
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
        this.setState({
            expandTree: mapTree(
                (expandStatus: ActionExpandStatus) => expandStatus.id === actionId ? ({ ...expandStatus, isExpanded }) : expandStatus, 
                this.state.expandTree
            )
        });
    }
}

/**
 * State saver for ActionTree component. Responsible for creating default state value and updating state in context.
 * @param props 
 */
const RecoverableActionTree = (props: ContainerProps) => {
    const stateKey = isAction(props.action) ? keyForAction(props.action.id) : props.action.actionNodeType,
        getDefaultState: () => Tree<ActionExpandStatus> = () => 
            isAction(props.action) ? 
                createExpandTree(props.action, props.expandedTreePath) : 
                createNode({ id: null, isExpanded: false });

    return (
        <StateSaver 
            stateKey={stateKey}
            getDefaultState={getDefaultState}>
            {(expandState: Tree<ActionExpandStatus>, stateSaver: (state: Tree<ActionExpandStatus>) => any) => (
                <ActionTreeBase
                    {...props}
                    expandState={expandState}
                    saveState={stateSaver}/>
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
    (state: AppState, ownProps: OwnProps): StateProps => ({
        selectedVerififcationId: state.selected.verificationId,
        selectedActionsId: state.selected.actionsId,
        scrolledActionId: state.selected.scrolledActionId,
        actionsFilter: state.filter.actionsFilter,
        expandedTreePath: getExpandedTreePath(ownProps.action, [...state.selected.actionsId, +state.selected.scrolledActionId])
    }),
    (dispatch, ownProps: OwnProps): DispatchProps => ({
        actionSelectHandler: action => dispatch(selectAction(action)),
        messageSelectHandler: (id, status) => dispatch(selectVerification(id, status))
    })
)(RecoverableActionTree);
