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

const EMPTY_ARRAY = [];

export const getSearchTokens = (state: AppState) => state.selected.search.tokens;
const getLeftPanelEnabled = (state: AppState) => state.selected.search.leftPanelEnabled;
const getRightPanelEnabled = (state: AppState) => state.selected.search.rightPanelEnabled;

export const getSearchContent = createSelector(
    [getFilteredActions, getFilteredMessages, getLeftPanelEnabled, getRightPanelEnabled],
    (actions, messages, leftPanelEnabled, rightPanelEnabled): SearchContent => ({
        actions: leftPanelEnabled ? actions : EMPTY_ARRAY,
        messages: rightPanelEnabled ? messages : EMPTY_ARRAY
    })
);
