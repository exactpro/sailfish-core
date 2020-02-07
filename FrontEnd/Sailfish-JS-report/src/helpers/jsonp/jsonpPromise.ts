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

export interface CancelableJsonpPromise<T> {
	promise: Promise<T>;
	cancel: () => void;
};

/**
 * This function creates cancelable jsonp promise. Also takes takes optional parameter for retry. It will retry N times if jsonp 
 * request is failed.
 * @param jsonpRequest request callback for jsonp file.
 * @param filePath path to jsonp file.
 * @param retry number of times it will retry request.
 */
export const makeCancelableJsonpPromise = <T>(jsonpRequest: () => Promise<T>, filePath: string, retry: number = 1) => {
    let hasCanceled = false;
  
    const wrappedPromise: Promise<T> = new Promise((resolve, reject) => {
        const onSuccess = response => hasCanceled ? reject({ isCanceled: true }) : resolve(response);
        const onFailure = error => {
            retry--;
            if (retry > 0 && !hasCanceled) {
                makeRequest();
            } else {
                hasCanceled ? reject({ isCanceled: true }) : reject(error);
            }
        }

        const makeRequest = () => {
            return jsonpRequest()
                .then(onSuccess)
                .catch(onFailure)
        }
        makeRequest();
    });
    
    return {
        promise: wrappedPromise,
        cancel() {
            hasCanceled = true;
            const jsonpScript = document.querySelector(`script[src="${filePath}"]`);
            jsonpScript && document.body.removeChild(jsonpScript);
        },
    };
};
