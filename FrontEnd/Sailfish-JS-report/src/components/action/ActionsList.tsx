/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
import { ActionNode, isAction } from '../../models/Action';
import { VirtualizedList } from '../VirtualizedList';
import AppState from '../../state/models/AppState';
import StateSaverProvider from '../util/StateSaverProvider';
import { actionsHeatmap } from '../../helpers/heatmapCreator';
import { getActionsScrollHints, getCurrentActions } from "../../selectors/actions";
import { getActionsFilterResultsCount, getIsFilterApplied } from "../../selectors/filter";
import { createBemElement } from "../../helpers/styleCreators";
import { getActions } from '../../helpers/action';
import SkeletonedActionTree from './SkeletonedActionTree';
import { getActionsCount } from '../../selectors/actions';
import { ScrollHint } from '../../models/util/ScrollHint';

interface Props {
    actions: Array<ActionNode>;
    selectedActions: number[];
    scrolledActionId: Number;
    isFilterApplied: boolean;
    filteredActionsCount: number;
    actionsCount: number;
    actionsScrollHints: ScrollHint[];
}

interface State {
    // Number objects is used here because in some cases (eg one message / action was selected several times by diferent entities)
    // We can't understand that we need to scroll to the selected entity again when we are comparing primitive numbers.
    // Objects and reference comparison is the only way to handle numbers changing in this case.
    scrolledIndex: Number;
}

interface OwnProps {
    ref?: React.MutableRefObject<ActionsListBase>
}

export class ActionsListBase extends React.PureComponent<Props, State> {

    private list = React.createRef<VirtualizedList>();

    constructor(props: Props) {
        super(props);

        this.state = {
            scrolledIndex: this.getScrolledIndex(props.scrolledActionId, props.actions)
        }
    }

    scrollToTop() {
        this.setState({
            scrolledIndex: new Number(0)
        });
    }

    private getScrolledIndex(scrolledActionId: Number, actions: ActionNode[]): Number {
        const scrolledIndex = actions.findIndex(
            action => isAction(action) && action.id === +scrolledActionId
        );

        return scrolledIndex !== -1 ? new Number(scrolledIndex) : null;
    }

    componentDidUpdate(prevProps: Props) {
        if (this.props.scrolledActionId !== prevProps.scrolledActionId && this.props.scrolledActionId != null) {
            this.setState({
                scrolledIndex: this.getScrolledIndex(this.props.scrolledActionId, this.props.actions)
            });
        }
    }

    render() {
        const { actions, selectedActions, isFilterApplied, filteredActionsCount, actionsCount, actionsScrollHints } = this.props,
            { scrolledIndex } = this.state;
        const listRootClass = createBemElement(
            "actions",
            "list",
            isFilterApplied ? "filter-applied" : null
        );

        return (
            <div className="actions">
                {
                    isFilterApplied ? (
                        <div className="actions__filter-info">
                            {filteredActionsCount} Actions Filtered
                        </div>
                    ) : null
                }
                <div className={listRootClass}>
                    <StateSaverProvider>
                        <VirtualizedList
                            rowCount={actionsCount}
                            ref={this.list}
                            renderElement={this.renderAction}
                            computeItemKey={this.computeKey}
                            scrolledIndex={scrolledIndex}
                            selectedElements={actionsHeatmap(getActions(actions), selectedActions)}
                            scrollHints={actionsScrollHints}
                        />
                    </StateSaverProvider>
                </div>
            </div>
        )
    }

    private renderAction = (index: number): React.ReactElement => {
        return <SkeletonedActionTree index={index} />
    };

    private computeKey = (index: number): number => {
        const action = this.props.actions[index];

        return action && isAction(action) ? action.id : index;
    };
}

export const ActionsList = connect<Props, {}, OwnProps, Props & OwnProps>(
    (state: AppState): Props => ({
        actions: getCurrentActions(state),
        selectedActions: state.selected.actionsId,
        scrolledActionId: state.selected.scrolledActionId,
        filteredActionsCount: getActionsFilterResultsCount(state),
        isFilterApplied: getIsFilterApplied(state),
        actionsCount: getActionsCount(state),
        actionsScrollHints: getActionsScrollHints(state)
    }),
    dispatch => ({}),
    (stateProps, dispatchProps, ownProps) => ({...stateProps, ...dispatchProps,...ownProps}),
    {
        forwardRef: true
    }
)(ActionsListBase);
