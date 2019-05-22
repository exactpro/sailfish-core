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
import { InitResponse, SubmittedData } from "../models/MlServiceResponse"

const BASE_ML_API_PATH = "sfgui/sfapi/machinelearning/v2"

export function submitEntry(token: string, dataToSubmit: SubmittedData, updateMlDataAction: (data: SubmittedData) => any): void {

    let currentHost = new URL(window.location.href).host;

    fetch(`http://${currentHost}/${BASE_ML_API_PATH}/${token}`, {
        method: 'PUT',
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
                updateMlDataAction(dataToSubmit);
            }
            else {
                throw new Error(`ml service responded with (${response.status})`);
            }
        })
        .catch(err => console.error("unable to submit ml data entry\n" + err));
}

export function deleteEntry(token: string, dataToDelete: SubmittedData, updateMlDataAction: (data: SubmittedData) => any): void {

    let currentHost = new URL(window.location.href).host;

    fetch(`http://${currentHost}/${BASE_ML_API_PATH}/${token}`, {
        method: 'DELETE',
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
                updateMlDataAction(dataToDelete)
            }
            else {
                throw new Error(`ml service responded with (${response.status})`);
            }
        })
        .catch(err => console.error("unable to remove ml data entry\n" + err));
}

export function fetchToken(workFolder: string, updateTokenAction: (token: string) => any,
    updateMlDataAction: (data: SubmittedData[]) => any) {

    let currentUrl = new URL(window.location.href);
    let reportZipUrl = currentUrl.href.replace(/index\.html[^/]*$/g, workFolder + ".zip");

    fetch(`http://${currentUrl.host}/${BASE_ML_API_PATH}/init?reportLink=${reportZipUrl}`, {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        },
    })
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            else {
                throw new Error(`ml service responded with (${response.status})`);
            }
        })
        .then((json) => {
            let data: InitResponse = json;
            updateTokenAction(data.token);
            updateMlDataAction(data.active);
        })
        .catch(err => console.error("unable to fetch ml token\n" + err));
}