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

import { ThunkDispatch, ThunkAction } from "redux-thunk";
import StateAction from "../actions/stateActions";
import { setSearchString, setSearchResults } from "../actions/actionCreators";
import { findAll } from "../helpers/search/searchEngine";
import AppState from "../state/models/AppState";


export function performSearch(searchString: string): ThunkAction<void, {}, {}, StateAction> {
    return async (dispatch: ThunkDispatch<{}, {}, StateAction>, getState: () => AppState) => {
        const { testCase } = getState().selected;
        
        dispatch(setSearchString(searchString));
        const results = await findAll(searchString, testCase);

        if (searchString === getState().selected.searchString) {
            dispatch(setSearchResults(results));
        }

    }
}
