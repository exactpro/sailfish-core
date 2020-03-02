/*******************************************************************************
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
import '../../styles/statusPanel.scss';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import KnownBugCategory, { isKnownBugCategory } from '../../models/KnownBugCategory';
import KnownBug, { isKnownBug, KnownBugNode } from '../../models/KnownBug';
import { KnownBugCard } from './KnownBugCard';
import Action from '../../models/Action';
import { selectKnownBug } from '../../actions/actionCreators';
import { StatusType } from '../../models/Status';
import { intersection } from '../../helpers/array';
import { getCategoryBugChains } from "../../helpers/knownbug";

const NO_CATEGORY_TITLE = 'No Categories';
const RIGHT_ARROW = '\u25B6';

interface StateProps {
    bugs: KnownBugNode[];
    selectedActionIds: number[];
    selectedStatus: StatusType;
    actionsMap: Map<number, Action>;
}

interface DispatchProps {
    onSelect: (knownBug: KnownBug, status: StatusType) => void;
}

interface Props extends DispatchProps, StateProps {
}

function KnownBugPanelBase({ bugs, actionsMap, selectedActionIds, onSelect, selectedStatus }: Props): React.ReactElement {
    const categoryChains = getCategoryBugChains(bugs);

    const arrow = <span className="known-bug-list__arrow">{RIGHT_ARROW}</span>;

    return (
        <div className="known-bug-list">
            {
                categoryChains.map(({categoriesChain, categoryBugs}, index) => (
                    <React.Fragment key={index}>
                        <div className="known-bug-list__category-title">
                            {
                                categoriesChain.length === 0 ?
                                    NO_CATEGORY_TITLE :
                                    categoriesChain.map((categoryName, index) => (
                                        <React.Fragment key={index}>
                                            {categoryName} {index + 1 !== categoriesChain.length ? arrow : null}
                                        </React.Fragment>
                                    ))
                            }
                        </div>
                        {
                            categoryBugs.map(bug => (
                                <KnownBugCard
                                    key={bug.id}
                                    bug={bug}
                                    actionsMap={actionsMap}
                                    selectedStatus={selectedStatus}
                                    isSelected={intersection(selectedActionIds, bug.relatedActionIds).length > 0}
                                    onSelect={(status = null) => onSelect(bug, status)} />
                            ))
                        }
                    </React.Fragment>
                ))
            }
        </div>
    );
}

export const KnownBugPanel = connect(
    (state: AppState): StateProps => ({
        bugs: state.selected.testCase.bugs,
        actionsMap: state.selected.actionsMap,
        selectedStatus: state.selected.selectedActionStatus,
        selectedActionIds: state.selected.actionsId
    }),
    (dispatch): DispatchProps => ({
        onSelect: (knownBug, status) => dispatch(selectKnownBug(knownBug, status))
    })
)(KnownBugPanelBase);
