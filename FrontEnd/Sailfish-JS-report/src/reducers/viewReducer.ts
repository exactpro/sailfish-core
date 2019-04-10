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

import ViewState from '../state/models/ViewState';
import initialViewState from '../state/initial/initialViewState';
import { StateActionType, StateActionTypes } from '../actions/stateActions';
import { Panel } from '../helpers/Panel';

export function viewReducer(state : ViewState = initialViewState, stateAction: StateActionType) : ViewState {
    switch(stateAction.type) {
        case StateActionTypes.SET_ADMIN_MSG_ENABLED: {
            return {
                ...state,
                adminMessagesEnabled: stateAction.adminEnabled
            }
        }

        case StateActionTypes.SWITCH_SPLIT_MODE: {
            if (state.splitMode) {
                return {
                    ...state,
                    splitMode: false
                }
            }

            return {
                ...state,
                splitMode: true,
                leftPanel: Panel.Actions
            }
        }

        case StateActionTypes.SET_LEFT_PANE: {
            return {
                ...state,
                leftPanel: stateAction.pane
            }
        }

        case StateActionTypes.SET_RIGHT_PANE: {
            return {
                ...state,
                rightPanel: stateAction.pane
            }
        }

        case StateActionTypes.SET_TEST_CASE: {
            return  {
                ...state, 

                // reset active panel to default when there is no status info to show
                leftPanel: stateAction.testCase.status.cause ? state.leftPanel : initialViewState.leftPanel
            }
        }

        case StateActionTypes.RESET_TEST_CASE: {
            return {
                ...state,
                leftPanel: initialViewState.leftPanel,
                rightPanel: initialViewState.rightPanel
            }
        }

        default: {
            return state
        }
    }
}
