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
import { getFilteredActions } from "./actions";
import { getFilteredMessages } from "./messages";
import SearchContent from "../models/search/SearchContent";
import { getLogs } from "./logs";
import Panel from "../util/Panel";
import { getKnownBugs } from "./knownBugs";
import { getCategoryBugChains } from "../helpers/knownbug";
import PanelSide from "../util/PanelSide";
import { getFilterTokens } from "./filter";
import { PanelSearchToken } from "../models/search/SearchToken";
import { getClosedPanel, getLeftPanel, getLeftPanelEnabled, getRightPanel, getRightPanelEnabled } from "./view";

const getSearchTokens = (state: AppState) => state.selected.search.tokens;

const getActivePanels = createSelector(
    [
        getLeftPanelEnabled,
        getRightPanelEnabled,
        getLeftPanel,
        getRightPanel,
        getClosedPanel
    ],
    (
        leftPanelEnabled,
        rightPanelEnabled,
        leftPanel,
        rightPanel,
        closedPanel
    ): Panel[] => [
        leftPanelEnabled && closedPanel != PanelSide.LEFT ? leftPanel : null,
        rightPanelEnabled && closedPanel != PanelSide.RIGHT ? rightPanel : null
    ].filter(Boolean)
);

const getSearchPanelTokens = createSelector(
    [getSearchTokens, getActivePanels],
    (searchTokens, activePanels): PanelSearchToken[] =>
        searchTokens.map(token => ({
            ...token,
            panels: activePanels
        }))
);

export const getTokens = createSelector(
    [getSearchPanelTokens, getFilterTokens],
    (searchTokens, filterTokens) => [...filterTokens, ...searchTokens]
);

export const getSearchContent = createSelector(
    [getFilteredActions, getFilteredMessages, getLogs, getKnownBugs],
    (actions, messages, logs, knownBugs): SearchContent => ({
        actions,
        messages,
        logs,
        // we need to place it in chains first to receive list of bugs in correct order
        bugs: getCategoryBugChains(knownBugs)
            .reduce((acc, { categoryBugs }) => [...acc, ...categoryBugs], [])
    })
);
