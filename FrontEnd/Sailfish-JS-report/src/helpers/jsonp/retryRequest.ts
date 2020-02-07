/******************************************************************************
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
 * limitations under the License.
 ******************************************************************************/

/**
 * This function retries request N times until it succeeds.
 * @param jsonpRequest request callback for jsonp file.
 * @param retry number of times it will retry request.
 */
export const retryRequest = <T>(jsonpRequest: () => Promise<T>, retry: number = 3): Promise<T> => {
    return new Promise<T>((resolve, reject) => {
        const onSuccess = response =>  resolve(response);
        const onFailure = error => {
            retry--;
            if (retry > 0) {
                makeRequest();
            } else {
                reject(error);
            }
        }

        const makeRequest = () => {
            return jsonpRequest()
                .then(onSuccess)
                .catch(onFailure)
        }
        makeRequest();
    });
};
