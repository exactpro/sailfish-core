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
import KnownBugCategory from '../../models/KnownBugCategory';
import KnownBug, { isKnownBug } from '../../models/KnownBug';
import { KnownBugCard } from './KnownBugCard';
import Action from '../../models/Action';
import { selectKnownBug } from '../../actions/actionCreators';
import { StatusType } from '../../models/Status';
import { intersection } from '../../helpers/array';

const NO_CATEGORY_TITLE = 'No Categories';
const RIGHT_ARROW = '\u25B6';

interface CategorizedBugEntry {
    bug: KnownBug;
    parents: KnownBugCategory[];
    categories: string[];
}

interface StateProps {
    bugs: (KnownBugCategory | KnownBug)[];
    selectedActionIds: number[];
    selectedStatus: StatusType;
    actionsMap: Map<number, Action>;
}

interface DispatchProps {
    onSelect: (knownBug: KnownBug, status: StatusType) => void;
}

interface Props extends DispatchProps, StateProps {
}

function KnownBugPanelBase({ bugs, actionsMap, selectedActionIds, onSelect, selectedStatus }: Props) {
    const bugEntries: CategorizedBugEntry[] = bugs.flatMap(item =>
        isKnownBug(item) ?
            { bug: item, parents: [], categories: [] } :
            flattenKnownBugCategoriesRecursive(item)
    );

    const categoryGroups: string[][] = bugEntries
        .map(({ categories }) => categories)
        .filter((cat, i, self) => self.map(c => c.toString()).indexOf(cat.toString()) === i)
        .sort((catA, catB) => catA.toString().localeCompare(catB.toString()));

    const arrow = <span className="known-bug-list__arrow">{RIGHT_ARROW}</span>;
    return (
        <div className="known-bug-list">
            {
               categoryGroups.map(categories => {
                    const categoriesCombined = categories.join();
                    return (
                        <div key={categoriesCombined}>
                            <h5 className="known-bug-list__category-title">
                            {
                                categories.length === 0 ? 
                                    NO_CATEGORY_TITLE :
                                    categories.map((name, i) => (
                                        <React.Fragment key={i}>
                                            {name} {i + 1 !== categories.length && arrow}
                                        </React.Fragment>
                                    ))
                            }
                            </h5>
                            {
                                bugEntries
                                    .filter(({categories: bugCategories}) => bugCategories.join() === categoriesCombined)
                                    .map(({bug}, i) => (
                                        <KnownBugCard
                                            key={i}
                                            bug={bug}
                                            actionsMap={actionsMap}
                                            selectedStatus={selectedStatus}
                                            isSelected={intersection(selectedActionIds, bug.relatedActionIds).length > 0}
                                            onSelect={(status = null) => onSelect(bug, status)} />
                                    ))
                            }
                        </div>
                    )
                })
            }

        </div>
    );
};

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

function flattenKnownBugCategoriesRecursive(element: KnownBugCategory, pathToElement: KnownBugCategory[] = []): CategorizedBugEntry[] {
    const currentPath = [...pathToElement, element];

    return element.subNodes.flatMap(item => 
        isKnownBug(item) ?
            {
                bug: item, 
                parents: currentPath, 
                categories: currentPath.map(c => c.name)
            } : flattenKnownBugCategoriesRecursive(item, currentPath)
    );
}
