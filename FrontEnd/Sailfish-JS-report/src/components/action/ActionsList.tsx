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
import { ActionNode, isAction } from '../../models/Action';
import { ActionTree } from './ActionTree';
import { VirtualizedList } from '../VirtualizedList';
import AppState from '../../state/models/AppState';
import StateSaverProvider from '../util/StateSaverProvider';
import { actionsHeatmap } from '../../helpers/heatmapCreator';
import { getActions } from '../../helpers/action';
import { memoizeLast } from '../../helpers/memoize';
import { StatusType } from '../../models/Status';

interface Props {
    actions: Array<ActionNode>;
    selectedActions: number[];
    scrolledActionId: Number;
}

interface State {
    // Number objects is used here because in some cases (eg one message / action was selected several times by diferent entities)
    // We can't understand that we need to scroll to the selected entity again when we are comparing primitive numbers.
    // Objects and reference comparison is the only way to handle numbers changing in this case.
    scrolledIndex: Number;
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
        const { actions, selectedActions } = this.props,
            { scrolledIndex } = this.state;

        return (
            <div className="actions">
                <div className="actions__list">
                    <StateSaverProvider>
                        <VirtualizedList
                            rowCount={actions.length}
                            itemSpacing={6}
                            ref={this.list}
                            elementRenderer={this.renderAction}
                            scrolledIndex={scrolledIndex}
                            selectedElements={actionsHeatmap(getActions(actions), selectedActions)}
                        />
                    </StateSaverProvider>
                </div>
            </div> 
        )
    }

    private renderAction = (idx: number, onExpand: () => void): React.ReactElement => (
        <ActionTree 
            action={this.props.actions[idx]}
            onExpand={onExpand}/>
    )
}   

const applyActionsFilter = memoizeLast((actions: ActionNode[], filter: Set<StatusType>) => {
    return actions.filter(action => isAction(action) ? filter.has(action.status.status) : true)
});

export const ActionsList = connect(
    (state: AppState): Props => ({
        actions: applyActionsFilter(state.selected.testCase.actions, state.filter.actionsFilter),
        selectedActions: state.selected.actionsId,
        scrolledActionId: state.selected.scrolledActionId
    }),
    dispatch => ({ }),
    (stateProps, dispatchProps, ownProps) => ({ ...stateProps, ...dispatchProps, ...ownProps}),
    {
        forwardRef: true
    } 
)(ActionsListBase);
