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
import StateActionType, {StateActionTypes} from '../actions/stateActions';
import Panel from "../util/Panel";

export function viewReducer(state : ViewState = initialViewState, stateAction: StateActionType) : ViewState {
    switch(stateAction.type) {
        case StateActionTypes.SET_ADMIN_MSG_ENABLED: {
            return {
                ...state,
                adminMessagesEnabled: new Boolean(stateAction.adminEnabled)
            }
        }

        case StateActionTypes.SET_LEFT_PANE: {
            return {
                ...state,
                leftPanel: stateAction.panel
            }
        }

        case StateActionTypes.SET_RIGHT_PANE: {
            return {
                ...state,
                rightPanel: stateAction.panel
            }
        }

        case StateActionTypes.SET_PANEL_AREA: {
            return {
                ...state,
                panelArea: stateAction.panelArea
            }
        }

        case StateActionTypes.SET_TEST_CASE: {
            let nextLeftPanel = state.leftPanel;

            // set active panel to status when testCase has ended with exception
            if (state.leftPanel == Panel.ACTIONS &&
                stateAction.testCase.files.action.count == 0 &&
                stateAction.testCase.status?.cause != null
            ) {
                nextLeftPanel = Panel.STATUS;
            }

            // set active panel to actions list when there is no status info to show
            if (state.leftPanel == Panel.STATUS && stateAction.testCase.status?.cause == null) {
                nextLeftPanel = Panel.ACTIONS;
            }

            return  {
                ...state,
                leftPanel: nextLeftPanel
            }
        }

        case StateActionTypes.RESET_TEST_CASE: {
            return {
                ...state,
                leftPanel: initialViewState.leftPanel,
                rightPanel: initialViewState.rightPanel
            }
        }

        case StateActionTypes.SET_IS_LOADING: {
            return {
                ...state,
                isLoading: stateAction.isLoading
            }
        }

        case StateActionTypes.TOGGLE_MESSAGE_BEAUTIFIER: {
            if (state.beautifiedMessages.includes(stateAction.messageId)) {
                return {
                    ...state,
                    beautifiedMessages: state.beautifiedMessages.filter(msgId => msgId !== stateAction.messageId)
                }
            }

            return {
                ...state,
                beautifiedMessages: [...state.beautifiedMessages, stateAction.messageId]
            }
        }

        case StateActionTypes.UGLIFY_ALL_MESSAGES: {
            return {
                ...state,
                beautifiedMessages: initialViewState.beautifiedMessages
            }
        }

        case StateActionTypes.SET_IS_CONNECTION_ERROR: {
            return {
                ...state,
                isConnectionError: stateAction.isConnectionError,
            }
        }

        default: {
            return state
        }
    }
}
