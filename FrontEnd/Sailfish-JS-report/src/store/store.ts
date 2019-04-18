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
import { initialAppState } from '../state/initial/initialAppState';
import Report from '../models/Report';
import { urlHandler } from '../middleware/urlHandler';
import initialReportState from '../state/initial/initialReportState';
import { combineReducers } from 'redux';
import { reportReducer } from '../reducers/reportReducer';
import { selectedReducer } from '../reducers/selectedReducer';
import { viewReducer } from '../reducers/viewReducer';
import { filterReducer } from '../reducers/filterReducer';

export const createAppStore = (report: Report) => createStore(
    combineReducers({
        report: reportReducer,
        selected: selectedReducer,
        view: viewReducer,
        filter: filterReducer
    }),
    {
        ...initialAppState,
        report: {
            ...initialReportState,
            report: report
        }
    },
    applyMiddleware(urlHandler)
)
