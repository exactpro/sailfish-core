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

import configureStore from "redux-mock-store";
import { switchActionsTransparencyFilter, switchFieldsTransparencyFilter } from "../../../actions/actionCreators";
import { StatusType } from "../../../models/Status";
import { filterReducer } from "../../../reducers/filterReducer";
import initialFilterState from '../../../state/initial/initialFilterState';
import FilterState from '../../../state/models/FiltersState';

describe('[Redux] Filter reducer', () => {
    test('SWITCH_ACTIONS_FILTER: enable filter', () => {
        const action = switchActionsTransparencyFilter(StatusType.PASSED),
            initialState: FilterState = {
                ...initialFilterState,
                actionsTransparencyFilter: new Set()
            };

        const state = filterReducer(initialState, action);

        const expectedState: FilterState = {
            ...initialFilterState,
            actionsTransparencyFilter: new Set([StatusType.PASSED])
        }

        expect(state).toEqual(expectedState);
    });

    test('SWITCH_ACTIONS_FILTER: disable filter', () => {
        const action = switchActionsTransparencyFilter(StatusType.PASSED),
            initialState: FilterState = {
                ...initialFilterState,
                actionsTransparencyFilter: new Set([StatusType.PASSED])
            };

        const state = filterReducer(initialState, action);

        const expectedState: FilterState = {
            ...initialFilterState,
            actionsTransparencyFilter: new Set()
        }

        expect(state).toEqual(expectedState);
    });

    test('SWITCH_ACTIONS_FILTER: enable, then disable filter', () => {
        const action = switchActionsTransparencyFilter(StatusType.PASSED),
            initialState: FilterState = {
                ...initialFilterState,
                actionsTransparencyFilter: new Set()
            };

        const enabledFilterState = filterReducer(initialState, action),
            disabledFilterState = filterReducer(enabledFilterState, action);

        const expectedState: FilterState = {
            ...initialFilterState,
            actionsTransparencyFilter: new Set()
        }

        expect(disabledFilterState).toEqual(expectedState);
    });

    test('SWITCH_FIELDS_FILTER: enable, then disable filter', () => {
        const action = switchFieldsTransparencyFilter(StatusType.PASSED),
            initialState: FilterState = {
                ...initialFilterState,
                fieldsTransparencyFilter: new Set()
            };

        const enabledFilterState = filterReducer(initialState, action),
            disabledFilterState = filterReducer(enabledFilterState, action);

        const enabledExcpectedState: FilterState = {
            ...initialFilterState,
            fieldsTransparencyFilter: new Set([StatusType.PASSED])
        };

        expect(enabledFilterState.fieldsTransparencyFilter).toEqual(enabledExcpectedState.fieldsTransparencyFilter);
        expect(disabledFilterState).toEqual(initialState);
    });
    
});
