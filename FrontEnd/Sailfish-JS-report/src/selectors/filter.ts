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
import { isKeyForAction, isKeyForMessage } from "../helpers/keys";
import FilterType from '../models/filter/FilterType';
import FilterPath from "../models/filter/FilterPath";
import { PanelSearchToken } from "../models/search/SearchToken";
import Panel from "../util/Panel";

const FILTER_HIGHLIGHT_COLOR = '#00BBCC';

export const getFilterBlocks = (state: AppState) => state.filter.blocks;
export const getFilterResults = (state: AppState) => state.filter.results;
export const getIsFilterHighlighted = (state: AppState) => state.filter.isHighlighted;
export const getIsFilterTransparent = (state: AppState) => state.filter.isTransparent;

const mapFilterTypeToPanel = (type: FilterType): Panel => {
    switch (type) {
        case FilterType.ACTION:
            return Panel.ACTIONS;
        case FilterType.MESSAGE:
            return Panel.MESSAGES;
    }
};

export const getFilterTokens = createSelector(
    [getFilterResults, getFilterBlocks, getIsFilterHighlighted],
    (results, blocks, isHighlighted): PanelSearchToken[] => {
        if (!isHighlighted) {
            return [];
        }

        return blocks
            .filter(({ path }) => path == FilterPath.ALL)
            .flatMap(({values, types}) =>
                values.map(val => ({
                    pattern: val,
                    color: FILTER_HIGHLIGHT_COLOR,
                    panels: types.map(mapFilterTypeToPanel),
                    isActive: false,
                    isScrollable: false
                }))
            )
    }
);

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
