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

import SearchState from "../state/models/SearchState";
import initialSearchState from "../state/initial/initialSearchState";
import StateAction, { StateActionTypes } from "../actions/stateActions";

export default function searchReducer(state: SearchState = initialSearchState, stateAction: StateAction): SearchState {
    switch (stateAction.type) {

        case StateActionTypes.SET_SEARCH_TOKENS: {
            return {
                ...state,
                tokens: stateAction.searchTokens,
                resultsCount: 0,
                index: null
            }
        }

        case StateActionTypes.SET_SEARCH_RESULTS: {
            const { searchResults: results } = stateAction,
                resultsCount = results.sum();

            return {
                ...state,
                results,
                resultsCount,
                index: initialSearchState.index
            }
        }

        case StateActionTypes.CLEAR_SEARCH: {
            return {
                ...initialSearchState
            };
        }

        case StateActionTypes.NEXT_SEARCH_RESULT: {
            const targetIndex = state.index != null ?
                (state.index + 1) % state.resultsCount :
                0;

            return {
                ...state,
                index: targetIndex,
                shouldScrollToItem: true
            }
        }

        case StateActionTypes.PREV_SEARCH_RESULT: {
            const targetIndex = state.index != null ?
                (state.resultsCount + state.index - 1) % state.resultsCount :
                state.resultsCount - 1;

            return {
                ...state,
                index: targetIndex,
                shouldScrollToItem: true
            }
        }

        case StateActionTypes.SET_SHOULD_SCROLL_TO_SEARCH_ITEM: {
            return {
                ...state,
                shouldScrollToItem: stateAction.isNeedsScroll
            }
        }

        case StateActionTypes.SET_SEARCH_LEFT_PANEL_ENABLED: {
            return {
                ...state,
                leftPanelEnabled: stateAction.isEnabled
            }
        }

        case StateActionTypes.SET_SEARCH_RIGHT_PANEL_ENABLED: {
            return {
                ...state,
                rightPanelEnabled: stateAction.isEnabled
            }
        }

        default: {
            return state;
        }
    }
}