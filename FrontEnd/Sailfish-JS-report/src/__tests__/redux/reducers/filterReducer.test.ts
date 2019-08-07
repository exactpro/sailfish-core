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
import { switchActionsFilter, switchFieldsFilter } from "../../../actions/actionCreators";
import { StatusType } from "../../../models/Status";
import { filterReducer } from "../../../reducers/filterReducer";
import initialFilterState from '../../../state/initial/initialFilterState';
import FilterState from '../../../state/models/FiltersState';

describe('[Redux] Filter reducer', () => {
    test('SWITCH_ACTIONS_FILTER: enable filter', () => {
        const action = switchActionsFilter(StatusType.PASSED),
            initialState: FilterState = {
                ...initialFilterState,
                actionsFilter: []
            };

        const state = filterReducer(initialState, action);

        const expectedState: FilterState = {
            ...initialFilterState,
            actionsFilter: [StatusType.PASSED]
        }

        expect(state).toEqual(expectedState);
    });

    test('SWITCH_ACTIONS_FILTER: disable filter', () => {
        const action = switchActionsFilter(StatusType.PASSED),
            initialState: FilterState = {
                ...initialFilterState,
                actionsFilter: [StatusType.PASSED]
            };

        const state = filterReducer(initialState, action);

        const expectedState: FilterState = {
            ...initialFilterState,
            actionsFilter: []
        }

        expect(state).toEqual(expectedState);
    });

    test('SWITCH_ACTIONS_FILTER: enable, then disable filter', () => {
        const action = switchActionsFilter(StatusType.PASSED),
            initialState: FilterState = {
                ...initialFilterState,
                actionsFilter: []
            };

        const enabledFilterState = filterReducer(initialState, action),
            disabledFilterState = filterReducer(enabledFilterState, action);

        const expectedState: FilterState = {
            ...initialFilterState,
            actionsFilter: []
        }

        expect(disabledFilterState).toEqual(expectedState);
    });

    test('SWITCH_FIELDS_FILTER: enable, then disable filter', () => {
        const action = switchFieldsFilter(StatusType.PASSED),
            initialState: FilterState = {
                ...initialFilterState,
                fieldsFilter: []
            };

        const enabledFilterState = filterReducer(initialState, action),
            disabledFilterState = filterReducer(enabledFilterState, action);

        const enabledExcpectedState: FilterState = {
            ...initialFilterState,
            fieldsFilter: [StatusType.PASSED]
        };

        expect(enabledFilterState).toEqual(enabledExcpectedState);
        expect(disabledFilterState).toEqual(initialState);
    });
    
});
