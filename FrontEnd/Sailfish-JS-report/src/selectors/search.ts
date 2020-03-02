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

const EMPTY_ARRAY = [];

export const getSearchTokens = (state: AppState) => state.selected.search.tokens;
const getLeftPanelEnabled = (state: AppState) => state.selected.search.leftPanelEnabled;
const getRightPanelEnabled = (state: AppState) => state.selected.search.rightPanelEnabled;
const getLeftPanel = (state: AppState) => state.view.leftPanel;
const getRightPanel = (state: AppState) => state.view.rightPanel;

export const getSearchContent = createSelector(
    [
        getFilteredActions,
        getFilteredMessages,
        getLogs,
        getLeftPanelEnabled,
        getRightPanelEnabled,
        getLeftPanel,
        getRightPanel
    ],
    (
        actions, messages,logs , leftPanelEnabled, rightPanelEnabled, leftPanel, rightPanel
    ): SearchContent => ({
        actions: leftPanelEnabled && leftPanel == Panel.ACTIONS ? actions : EMPTY_ARRAY,
        messages: rightPanelEnabled && rightPanel == Panel.MESSAGES ? messages : EMPTY_ARRAY,
        logs: rightPanelEnabled && rightPanel == Panel.LOGS ? logs : EMPTY_ARRAY
    })
);
