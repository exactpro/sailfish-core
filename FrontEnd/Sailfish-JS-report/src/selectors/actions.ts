/*******************************************************************************
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
 *  limitations under the License.
 ******************************************************************************/

import { createSelector } from "reselect";
import AppState from "../state/models/AppState";
import { ActionNode, isAction } from "../models/Action";
import { getFilterBlocks, getFilterResults, getIsFilterApplied, getIsFilterTransparent } from "./filter";
import FilterType from "../models/filter/FilterType";
import { getCheckpointActions as filterCheckpointsActions } from "../helpers/checkpointFilter";
import { filterActionNode, removeNonexistingRelatedMessages } from "../helpers/action";
import { getMessagesIds } from './messages';
import { ScrollHint } from '../models/util/ScrollHint';

const EMPTY_ACTIONS_ARRAY = new Array<ActionNode>();

export const getActions = (state: AppState) => state.selected.testCase?.actions ?? EMPTY_ACTIONS_ARRAY;
const getSelectedCheckpointId = (state: AppState) => state.selected.checkpointActionId;

export const getActionsWithoutNonexistingRelatedMessages = createSelector(
    [getActions, getMessagesIds],
    (actions, messagesIds) => actions.map(action => removeNonexistingRelatedMessages(action, messagesIds))
);

export const getFilteredActions = createSelector(
    [getActionsWithoutNonexistingRelatedMessages, getFilterBlocks, getFilterResults],
    (actions, blocks, results) => {
        if (blocks.some(({ types }) => types.includes(FilterType.ACTION))) {
            return actions
                .map(action => filterActionNode(action, results))
                .filter(Boolean);
        }

        return actions;
    }
);

export const getCheckpointActions = createSelector(
    [getActions],
    actions => filterCheckpointsActions(actions)
);

export const getSelectedCheckpoint = createSelector(
    [getCheckpointActions, getSelectedCheckpointId],
    (checkpoints, selectedId) => checkpoints.find(cp => cp.id == selectedId)
);

export const getActionsFilesCount = (state: AppState) => state.selected.testCase.files?.action.count || 0;

export const getCurrentActions = createSelector(
    [getIsFilterApplied, getIsFilterTransparent, getFilteredActions, getActionsWithoutNonexistingRelatedMessages],
    (filterIsApllied, isTransparent, filteredActions, actions) =>
        filterIsApllied && !isTransparent ? filteredActions : actions
);

export const getActionsCount = createSelector(
    [getIsFilterApplied, getIsFilterTransparent, getFilteredActions, getActionsFilesCount],
    (isFilterApplied, isTransparent, filteredActions, actionsFilesCount) => 
        isFilterApplied  && !isTransparent ? filteredActions.length : actionsFilesCount
);

export const getActionScrollHintsIds = (state: AppState) => state.selected.actionsScrollHintsIds;

export const getActionsScrollHints = createSelector(
    [getCurrentActions, getActionScrollHintsIds],
    (actions, scrollHintsIds) => {
        const scrollHints: ScrollHint[] = [];
        actions.filter(isAction)
            .forEach(({ id }, index) => {
                if (scrollHintsIds.includes(id)) {
                    scrollHints.push({
                        index,
                        id,
                        type: 'Action',
                    })
                }
            })
        return scrollHints;
    }
);

export const getIsActionsEmpty = (state: AppState) => state.selected.testCase?.files.action.count == 0;
