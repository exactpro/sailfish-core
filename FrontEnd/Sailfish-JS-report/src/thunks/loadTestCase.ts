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
import { AnyAction } from "redux";
import { fetchTestCase } from "../helpers/jsonp";
import { setTestCase, setIsLoading } from "../actions/actionCreators";
import StateActionType from "../actions/stateActions";
import AppState from '../state/models/AppState';
import { findNextCyclicItem, findPrevCyclicItem } from "../helpers/array";

export function loadTestCase(testCasePath: string): ThunkAction<void, {}, {}, AnyAction> {
    return (dispatch: ThunkDispatch<{}, {}, StateActionType>) => {
        dispatch(setIsLoading(true));

        fetchTestCase(testCasePath)
            .then(report => dispatch(setTestCase(report)))
            .catch(err => console.error(err))
            .finally(() => dispatch(setIsLoading(false)));
    }
}

export function loadNextTestCase(): ThunkAction<void, {}, {}, AnyAction> {
    return (dispatch: ThunkDispatch<{}, {}, StateActionType>, getState: () => AppState) => {
        const { report, selected } = getState(),
            nextMetadata = findNextCyclicItem(report.metadata, metadata => metadata.id === selected.testCase.id),
            testCasePath = nextMetadata.jsonpFileName;

        dispatch(setIsLoading(true));

        fetchTestCase(testCasePath)
            .then(report => dispatch(setTestCase(report)))
            .catch(err => console.error(err))
            .finally(() => dispatch(setIsLoading(false)));
    }
}

export function loadPrevTestCase(): ThunkAction<void, {}, {}, AnyAction> {
    return (dispatch: ThunkDispatch<{}, {}, StateActionType>, getState: () => AppState) => {
        const { report, selected } = getState(),
            nextMetadata = findPrevCyclicItem(report.metadata, metadata => metadata.id === selected.testCase.id),
            testCasePath = nextMetadata.jsonpFileName;

        dispatch(setIsLoading(true));

        fetchTestCase(testCasePath)
            .then(report => dispatch(setTestCase(report)))
            .catch(err => console.error(err))
            .finally(() => dispatch(setIsLoading(false)));
    }
}
