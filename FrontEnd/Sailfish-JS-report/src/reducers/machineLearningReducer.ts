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

import StateActionType, { StateActionTypes } from '../actions/stateActions';
import MachineLearningState from '../state/models/MachineLearningState';
import initialMachineLearningState from '../state/initial/initialMachineLearningState';

export function machineLearningReducer(state: MachineLearningState = initialMachineLearningState, stateAction: StateActionType): MachineLearningState {
    switch (stateAction.type) {

        case StateActionTypes.SET_ML_TOKEN: {
            return {
                ...state,
                token: stateAction.token
            }
        }

        case StateActionTypes.ADD_SUBMITTED_ML_DATA: {
            return {
                ...state,
                submittedData: state.submittedData.concat(stateAction.data)
            }
        }

        case StateActionTypes.REMOVE_SUBMITTED_ML_DATA: {
            return {
                ...state,
                submittedData: state.submittedData.filter((entry) => {
                    return !(entry.actionId === stateAction.data.actionId && entry.messageId === stateAction.data.messageId)
                })
            }

        }

        case StateActionTypes.SET_SUBMITTED_ML_DATA: {
            return {
                ...state,
                submittedData: stateAction.data
            }
        } 

        case StateActionTypes.SAVE_ML_DATA: {
            return {
                ...state,
                predictionData: state.predictionData.concat(stateAction.data.filter((newItem) => {
                    return (!state.predictionData.some((existingItem) => {
                        return (existingItem.actionId === newItem.actionId && existingItem.messageId === newItem.messageId)
                    }))    
                }))
            }
        }

        case StateActionTypes.TOGGLE_PREDICTIONS: {
            return {
                ...state,
                predictionsEnabled: !state.predictionsEnabled
            }
        }

        default: {
            return state;
        }
    }
}
