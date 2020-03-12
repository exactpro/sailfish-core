
/******************************************************************************
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

import FilterState from '../state/models/FiltersState';
import initialFilterState from '../state/initial/initialFilterState';
import StateActionType, { StateActionTypes } from '../actions/stateActions';

export function filterReducer(state: FilterState = initialFilterState, stateAction: StateActionType): FilterState {
    switch(stateAction.type) {

        case StateActionTypes.SET_FILTER_RESULTS: {
            return {
                ...state,
                results: stateAction.results
            }
        }

        case StateActionTypes.SET_FILTER_CONFIG: {
            return {
                ...state,
                blocks: stateAction.blocks
            }
        }

        case StateActionTypes.SET_FILTER_IS_TRANSPARENT: {
            return {
                ...state,
                isTransparent: stateAction.isTransparent
            }
        }

        case StateActionTypes.SET_FILTER_IS_HIGHLIGHTED: {
            return {
                ...state,
                isHighlighted: stateAction.isHighlighted
            }
        }

        case StateActionTypes.SET_TEST_CASE:
        case StateActionTypes.RESET_FILTER: {
            return {
                ...initialFilterState,
                isTransparent: state.isTransparent
            }
        }

        default: {
            return state;
        }
    }
}
