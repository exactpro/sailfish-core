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

import AppState from "../state/models/AppState";
import { createSelector } from "reselect";
import { isAction } from "../models/Action";
import { keyForAction } from "../helpers/keys";
import { getFilterBlocks, getFilterResults } from "./filter";
import FilterType from "../models/filter/FilterType";
import { isCheckpointAction } from "../helpers/action";
import { getCheckpointActions as filterCheckpointsActions } from "../helpers/checkpointFilter";

export const getActions = (state: AppState) => state.selected.testCase.actions;

export const getFilteredActions = createSelector(
    [getActions, getFilterBlocks, getFilterResults],
    (actions, blocks, results) => {
        if (blocks.some(({ types }) => types.includes(FilterType.ACTION))) {
            return actions
                .filter(isAction)
                .filter(action => isCheckpointAction(action) || results.includes(keyForAction(action.id)))
        }

        return actions;
    }
);

export const getCheckpointActions = createSelector(
    [getActions],
    actions => filterCheckpointsActions(actions)
);
