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

import ReportState from '../state/models/ReportState';
import initialReportState from '../state/initial/initialReportState';
import { StateActionType, StateActionTypes } from '../actions/stateActions';
import { findNextCyclicItem, findPrevCyclicItem } from '../helpers/array';

export function reportReducer(state : ReportState = initialReportState, stateAction : StateActionType): ReportState {
    switch(stateAction.type) {

        case StateActionTypes.SET_REPORT: {
            return {
                ...state,
                report: stateAction.report,
                currentTestCasePath: initialReportState.currentTestCasePath
            }
        }

        case StateActionTypes.SET_TEST_CASE_PATH: {
            return {
                ...state, 
                currentTestCasePath: stateAction.testCasePath
            }
        }

        case StateActionTypes.RESET_TEST_CASE: {
            return {
                ...state, 
                currentTestCasePath: initialReportState.currentTestCasePath
            }
        }

        case StateActionTypes.NEXT_TEST_CASE: {
            const nextTestCase = findNextCyclicItem(state.report.metadata, metadata => metadata.jsonpFileName === state.currentTestCasePath);

            return {
                ...state,
                currentTestCasePath: nextTestCase ? nextTestCase.jsonpFileName : initialReportState.currentTestCasePath
            }
        }

        case StateActionTypes.PREV_TEST_CASE: {
            const prevTestCase = findPrevCyclicItem(state.report.metadata, metadata => metadata.jsonpFileName === state.currentTestCasePath);

            return {
                ...state,
                currentTestCasePath: prevTestCase ? prevTestCase.jsonpFileName : initialReportState.currentTestCasePath
            }
        }

        default: {
            return state;
        }
    }
}
