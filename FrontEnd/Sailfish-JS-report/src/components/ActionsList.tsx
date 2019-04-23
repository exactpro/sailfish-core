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

import { h } from 'preact';
import { connect } from 'preact-redux';
import '../styles/action.scss';
import Action, { ActionNode, ActionNodeType } from '../models/Action';
import { ActionTree } from './ActionTree';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { VirtualizedList } from './VirtualizedList';
import PureComponent from '../util/PureComponent';
import AppState from '../state/models/AppState';

interface ListProps {
    actions: Array<ActionNode>;
}

export class ActionsListBase extends PureComponent<ListProps> {

    //private elements: ActionTree[] = [];
    private scrollbar: HeatmapScrollbar;

    scrollToTop() {
        this.scrollbar && this.scrollbar.scrollToTop();
    }

    private elements: any[] = [];

    scrollToAction(actionId: number) {
        // if (this.elements[actionId]) {
        //     // smooth behavior is disabled here
        //     // base - get HTMLElement by ref
        //     this.elements[actionId].base.scrollIntoView({block: 'center'});
        // }    
    }

    render({ actions }: ListProps) {

        return (
            <div class="actions">
                <div class="actions__list">
                    <VirtualizedList
                        rowCount={actions.length}
                        itemSpacing={6}
                        elementRenderer={idx => {
                            const action = actions[idx];

                            return (
                                <ActionTree 
                                    action={action}
                                    ref={ref => {
                                        if (action.actionNodeType === ActionNodeType.ACTION) {
                                            this.elements[(action as Action).id] = ref;
                                        }
                                    }}/>
                            )
                        }}/>
                </div>
            </div> 
        )
    }
}   

export const ActionsList = connect(
    (state: AppState): ListProps => ({
        actions: state.selected.testCase.actions
    }),
    dispatch => ({ }),
    null,
    {
        withRef: true
    }
)(ActionsListBase);
