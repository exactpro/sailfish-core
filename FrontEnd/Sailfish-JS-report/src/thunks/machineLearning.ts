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

import {InitResponse, PredictionResponse, SubmittedData} from "../models/MlServiceResponse"
import {ThunkAction, ThunkDispatch} from "redux-thunk";
import {AnyAction} from 'redux';
import StateAction from "../actions/stateActions";
import AppState from "../state/models/AppState";
import {addSubmittedMlData, removeSubmittedMlData, saveMlData, setMlToken, setSubmittedMlData} from "../actions/actionCreators";
import {batch} from "react-redux";

const API_PATH_SEARCH_KEY = "mlapi";

export const EMPTY_MESSAGE_ID = -1;

export function mlSubmitEntry(dataToSubmit: SubmittedData): ThunkAction<void, never, never, AnyAction> {
    return (dispatch: ThunkDispatch<never, never, StateAction>, getState: () => AppState) => {

        const { token } = getState().machineLearning;

        if (dataToSubmit.messageId !== EMPTY_MESSAGE_ID) {
            dispatch(mlDeleteEntry({ 
                actionId: dataToSubmit.actionId, 
                messageId: EMPTY_MESSAGE_ID 
            }));
        }

        fetch(`${getApiPath()}${token}`, {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify([
                {
                    actionId: dataToSubmit.actionId,
                    messageId: dataToSubmit.messageId
                }
            ])
        })
            .then(response => {
                if (response.ok) {
                    dispatch(addSubmittedMlData(dataToSubmit));
                }
                else {
                    throw new Error(`ml service responded with (${response.status})`);
                }
            })
            .catch(err => console.error("unable to submit ml data entry\n" + err));
    }
}

export function mlDeleteEntry(dataToDelete: SubmittedData): ThunkAction<void, never, never, AnyAction> {
    return (dispatch: ThunkDispatch<never, never, StateAction>, getState: () => AppState) => {

        const { token } = getState().machineLearning;

        fetch(`${getApiPath()}${token}`, {
            method: 'DELETE',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify([
                {
                    actionId: dataToDelete.actionId,
                    messageId: dataToDelete.messageId
                }
            ])
        })
            .then(response => {
                if (response.ok) {
                    dispatch(removeSubmittedMlData(dataToDelete));
                }
                else {
                    throw new Error(`ml service responded with (${response.status})`);
                }
            })
            .catch(err => console.error("unable to remove ml data entry\n" + err));
    }
}

export function fetchToken(): ThunkAction<void, never, never, AnyAction> {
    return (dispatch: ThunkDispatch<never, never, StateAction>, getState: () => AppState) => {
        const { workFolder } = getState().report.reportProperties;

        const currentUrl = new URL(window.location.href);
        const reportZipUrl = currentUrl.href.replace(/index\.html[^/]*$/g, currentUrl.pathname.endsWith(".zip/index.html") ?  "" :  (workFolder + ".zip"));
        const apiPath = getApiPath();

        if (!apiPath) {
            console.warn('Unable to connect to ml service - ml API path not found at URL\'s search params.');
            return;
        }

        fetch(`${apiPath}init?reportLink=${encodeURIComponent(reportZipUrl)}`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => {
                if (response.ok) {
                    return response.json();
                }
                else {
                    throw new Error(`ml service responded with (${response.status})`);
                }
            })
            .then((data: InitResponse) => {
                batch(() => {
                    dispatch(setMlToken(data.token));
                    dispatch(setSubmittedMlData(data.active));
                });
            })
            .catch(err => console.error("unable to fetch ml token\n" + err));
    }
}

export function fetchPredictions(actionId: number): ThunkAction<void, never, never, AnyAction> {
    return (dispatch: ThunkDispatch<never, never, StateAction>, getState: () => AppState) => {
        const { token } = getState().machineLearning;

        const actionIdParameter = actionId != null ? `?actionId=${actionId}` : null;

        fetch(`${getApiPath()}${token}${actionIdParameter}`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json',
            },
        })
            .then(response => {
                if (response.ok) {
                    return response.json()
                }
                else {
                    throw new Error(`ml service responded with (${response.status})`);
                }
            })
            .then((data: PredictionResponse) => {
                dispatch(saveMlData(data.predictions));
            })
            .catch(err => console.error("unable to get ml predictions\n" + err));
    }
}

function getApiPath(): string | null {
    const url = window.location.href,
        searchParamsString = url.substring(url.lastIndexOf('?')),
        searchParams = new URLSearchParams(searchParamsString),
        apiPathParam = searchParams.get(API_PATH_SEARCH_KEY);

    if (apiPathParam != null) {
        return decodeURI(apiPathParam);
    } else {
        return null;
    }
}
