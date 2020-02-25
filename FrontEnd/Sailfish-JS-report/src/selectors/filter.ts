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
import { isKeyForAction, isKeyForMessage, isKeyForVerification } from "../helpers/keys";
import FilterType from '../models/filter/FilterType';

export const getFilterBlocks = (state: AppState) => state.filter.blocks;
export const getFilterResults = (state: AppState) => state.filter.results;

export const getIsFilterApplied = createSelector(
    [getFilterBlocks],
    (blocks) =>
        blocks.length > 0 && blocks.some(block => block.values.length > 0)
);

export const getActionsFilterResultsCount = createSelector(
    [getFilterResults],
    (results) => results.filter(isKeyForAction).length
);

export const getMessagesFilterResultsCount = createSelector(
    [getFilterResults],
    (results) => results.filter(isKeyForMessage).length
);

export const getIsMessageFilterApplied = createSelector(
    [getIsFilterApplied, getFilterBlocks],
    (isFilterApplied, filterBlocks) =>
        isFilterApplied && filterBlocks.some(({ types }) => types.includes(FilterType.MESSAGE))
);
export const getIsFilterTransparent = (state: AppState) => state.filter.isTransparent;