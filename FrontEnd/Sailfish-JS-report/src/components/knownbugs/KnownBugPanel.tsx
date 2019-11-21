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
import { KnownBugCard } from "./KnownBugCard";
import Action from "../../models/Action";
import { selectKnownBug } from "../../actions/actionCreators";
import { StatusType } from '../../models/Status';
import { intersection } from '../../helpers/array';

interface CategorizedBugEntry {
    bug: KnownBug;
    parents: KnownBugCategory[];
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
            { bug: item, parents: [] } :
            flattenKnownBugCategoriesRecursive(item)
    );

    return (
        <div className="known-bug-list">
            {
                bugEntries.map(({ bug, parents }, index) => (
                    <KnownBugCard
                        key={index}
                        bug={bug}
                        actionsMap={actionsMap}
                        selectedStatus={selectedStatus}
                        parentCategories={parents}
                        isSelected={intersection(selectedActionIds, bug.relatedActionIds).length > 0}
                        onSelect={(status = null) => onSelect(bug, status)} />
                ))
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
            { bug: item, parents: currentPath } :
            flattenKnownBugCategoriesRecursive(item, currentPath)
    );
}
