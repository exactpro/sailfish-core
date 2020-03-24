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
import '../../styles/action.scss';
import Action, { ActionNode, isAction } from '../../models/Action';
import { StatusType } from '../../models/Status';
import Tree, { createNode, mapTree } from '../../models/util/Tree';
import AppState from '../../state/models/AppState';
import ActionExpandStatus from '../../models/util/ActionExpandStatus';
import { selectAction, selectVerification } from '../../actions/actionCreators';
import StateSaver from '../util/StateSaver';
import memoize from '../../helpers/memoize';
import { createExpandTree, createExpandTreePath, updateExpandTree } from '../../helpers/tree';
import { keyForAction } from '../../helpers/keys';
import { fetchPredictions } from "../../thunks/machineLearning";
import { ThunkDispatch } from 'redux-thunk';
import StateAction from '../../actions/stateActions';
import ActionTreeNode from './ActionTreeNode';
import { getIsFilterApplied } from "../../selectors/filter";
import FilterType from "../../models/filter/FilterType";
import PanelArea from "../../util/PanelArea";

interface OwnProps {
    action: ActionNode;
}

interface StateProps {
    selectedVerificationId: number;
    selectedActionsId: number[];
    panelArea: PanelArea;
    filter: {
        results: string[];
        isActive: boolean;
        isTransparent: boolean;
    };
    expandedTreePath: Tree<number> | null;
    mlDataActionIds: Set<number>;
}

interface DispatchProps {
    fetchPredictions: (actionId: number) => void;
    actionSelectHandler: (action: Action) => void;
    verificationSelectHandler: (messageId: number, rootActionId: number, status: StatusType) => void;
}

interface ContainerProps extends OwnProps, StateProps, DispatchProps {}

type Props = ContainerProps & { 
    expandState: Tree<ActionExpandStatus>,
    saveState: (state: Tree<ActionExpandStatus>) => void; 
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

    componentWillUnmount() {
        this.props.saveState(this.state.expandTree);
    }

    render() {
        const props = this.props;

        return (
            <ActionTreeNode
                action={props.action}
                panelArea={props.panelArea}
                isRoot
                selectedActionsId={props.selectedActionsId}
                selectedVerificationId={props.selectedVerificationId}
                filter={props.filter}
                expandPath={this.state.expandTree}
                onRootExpand={this.onRootExpand}
                onActionSelect={this.onActionSelect}
                onVerificationSelect={this.onVerificationSelect}/>
        )
    }   
    
    private onActionSelect = (selectedAction: Action) => {
        if (selectedAction.status.status == StatusType.FAILED && !this.props.mlDataActionIds.has(selectedAction.id)) {
            this.props.fetchPredictions(selectedAction.id);
        }

        this.props.actionSelectHandler(selectedAction);
    };

    private onVerificationSelect = (messageId: number, actionId: number, status: StatusType) => {
        if (status == StatusType.FAILED && !this.props.mlDataActionIds.has(actionId)) {
            this.props.fetchPredictions(actionId);
        }
        
        this.props.verificationSelectHandler(messageId, actionId, status);
    };

    private onRootExpand = (actionId: number, isExpanded: boolean) => {
        this.setState({
            expandTree: mapTree(
                (expandStatus: ActionExpandStatus) => 
                    expandStatus.id === actionId ? ({ ...expandStatus, isExpanded: !expandStatus.isExpanded }) : expandStatus,
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
            {(expandState: Tree<ActionExpandStatus>, stateSaver: (state: Tree<ActionExpandStatus>) => void) => (
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
        selectedVerificationId: state.selected.verificationId,
        selectedActionsId: state.selected.actionsId,
        panelArea: state.view.panelArea,
        filter: {
            results: state.filter.results,
            isActive: getIsFilterApplied(state) && state.filter.blocks.some(({ types }) => types.includes(FilterType.ACTION)),
            isTransparent: state.filter.isTransparent
        },
        mlDataActionIds: new Set<number>(state.machineLearning.predictionData.map(item => item.actionId)),
        expandedTreePath: 
            // scrolledActionId can be null in 2 cases:
            // 1. there is no selected actions at all
            // 2. user clicked on some action card
            // For both cases we don't need to expand any nodes manually, so we pass null as expand path.
            state.selected.scrolledActionId != null ? 
                getExpandedTreePath(ownProps.action, [...state.selected.actionsId, state.selected.scrolledActionId.valueOf()]) : 
                null
    }),
    (dispatch: ThunkDispatch<AppState, never, StateAction>, ownProps: OwnProps): DispatchProps => ({
        fetchPredictions: actionId => dispatch(fetchPredictions(actionId)),
        actionSelectHandler: action => dispatch(selectAction(action)),
        verificationSelectHandler: (messageId, rootActionId, status) => dispatch(selectVerification(messageId, rootActionId, status))
    })
)(RecoverableActionTree);
