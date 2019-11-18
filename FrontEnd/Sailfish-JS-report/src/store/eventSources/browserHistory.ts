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

import { Store } from 'redux';
import AppState from '../../state/models/AppState';
import StateAction, { isStateAction } from '../../actions/stateActions';
import { resetTestCase } from '../../actions/actionCreators';

/**
 * Handles goBack and goForward browser history actions - 
 * just returns appliction in previous state using 
 * redux aciton passed with window.history.pushState 
 * (see urlHandler redux middleware). 
 * @param store target redux store
 */
export default function initBroserHistoryEventSource(store: Store<AppState, StateAction>) {
    window.onpopstate = (e: PopStateEvent) => {
        const action = e.state;

        if (!action) {
            store.dispatch(resetTestCase());
        }

        if (isStateAction(action)) {
            store.dispatch(action);
        }
    }
}
