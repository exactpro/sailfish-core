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

import SearchResult from "./SearchResult";

export function getParamsExpandPath(searchResults: SearchResult, index: number, actionId: number): number[] {
    const [currentKey] = searchResults.getByIndex(index);

    if (!currentKey) {
        return [];
    }

    const [resultType, id, field, ...path] = currentKey.split('-');

    if (resultType === 'action' && +id === actionId && field === 'parameters' && path != null ) {
        return path.map(Number);
    } else {
        return [];
    }
}

export function getVerificationExpandPath(searchResults: SearchResult, index: number, actionId: number, msgId: number): number[] {
    const [currentKey] = searchResults.getByIndex(index);

    if (!currentKey) {
        return [];
    }

    const [resultType, resultId, actionType, resultMsgId, ...path] = currentKey.split('-');

    if (resultType === 'action' && +resultId === actionId && actionType === 'verification' && +resultMsgId === msgId && path != null) {
        return path.map(Number);
    } else {
        return [];
    }
}
