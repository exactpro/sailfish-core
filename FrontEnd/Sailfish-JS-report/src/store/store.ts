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

import { createStore, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import { composeWithDevTools } from 'redux-devtools-extension';
import { initialAppState } from '../state/initial/initialAppState';
import { urlHandler } from '../middleware/urlHandler';
import { combineReducers } from 'redux';
import { reportReducer } from '../reducers/reportReducer';
import { selectedReducer } from '../reducers/selectedReducer';
import { viewReducer } from '../reducers/viewReducer';
import { filterReducer } from '../reducers/filterReducer';
import { machineLearningReducer } from '../reducers/machineLearningReducer';
import AppState from '../state/models/AppState';
import StateActionType from '../actions/stateActions';
import { initLiveUpdateEventSource } from './eventSources/liveUpdate';
import LiveUpdateService from '../helpers/files/LiveUpdateService';
import ThunkExtraArgument from '../models/ThunkExtraArgument';
import initBrowserHistoryEventSource from './eventSources/browserHistory';
import searchAutorun from "../middleware/autorun/searchAutorun";

export function createAppStore() {
    const liveUpdateService = new LiveUpdateService(), 
        thunkExtra: ThunkExtraArgument = { liveUpdateService },
        middleware = [
            thunk.withExtraArgument(thunkExtra),
            urlHandler,
            searchAutorun
        ];

    const store = createStore<AppState, StateActionType, {}, {}>(
        combineReducers({
            report: reportReducer,
            selected: selectedReducer,
            view: viewReducer,
            filter: filterReducer,
            machineLearning: machineLearningReducer
        }),
        initialAppState,
        composeWithDevTools(applyMiddleware(...middleware))
    );

    initLiveUpdateEventSource(store, liveUpdateService);
    initBrowserHistoryEventSource(store);

    return store;
}
