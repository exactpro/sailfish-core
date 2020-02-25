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

import { Dispatch, Middleware } from "redux";
import AppState from "../../state/models/AppState";
import StateAction from "../../actions/stateActions";

type Dependency<STATE, RESULT = unknown> = (state: STATE) => RESULT;

type AutorunMiddleware = {
    <D1>(
        deps: [
            Dependency<AppState, D1>
        ],
        handler: (d1: D1) => Promise<StateAction>
    ): Middleware<never, AppState, Dispatch<StateAction>>

    <D1, D2>(
        deps: [
            Dependency<AppState, D1>,
            Dependency<AppState, D2>
        ],
        handler: (d1: D1, d2: D2) => Promise<StateAction>
    ): Middleware<never, AppState, Dispatch<StateAction>>

    <D1, D2, D3>(
        deps: [
            Dependency<AppState, D1>,
            Dependency<AppState, D2>,
            Dependency<AppState, D3>
        ],
        handler: (d1: D1, d2: D2, d3: D3) => Promise<StateAction>
    ): Middleware<never, AppState, Dispatch<StateAction>>

    <D1, D2, D3, D4>(
        deps: [
            Dependency<AppState, D1>,
            Dependency<AppState, D2>,
            Dependency<AppState, D3>,
            Dependency<AppState, D4>
        ],
        handler: (d1: D1, d2: D2, d3: D3, d4: D4) => Promise<StateAction>
    ): Middleware<never, AppState, Dispatch<StateAction>>

    <D1, D2, D3, D4, D5>(
        deps: [
            Dependency<AppState, D1>,
            Dependency<AppState, D2>,
            Dependency<AppState, D3>,
            Dependency<AppState, D4>,
            Dependency<AppState, D5>
        ],
        handler: (d1: D1, d2: D2, d3: D3, d4: D4, d5: D5) => Promise<StateAction>
    ): Middleware<never, AppState, Dispatch<StateAction>>
}

const createAutorunMiddleware: AutorunMiddleware = (
    dependencies,
    handler
) => {
    return store => next => action => {
        const prevState = store.getState(),
            result = next(action),
            currentState = store.getState();

        if (dependencies.some(selector => selector(prevState) != selector(currentState))) {

            handler(...dependencies.map(dep => dep(currentState)))
                 .then(action => {
                     const nextState = store.getState();

                     // check for store changes, if the store is the same we can dispatch action
                     if (dependencies.every(selector => selector(currentState) == selector(nextState))) {
                         store.dispatch(action);
                     }
                 });
        }

        return result;
    }
};

export default createAutorunMiddleware;
