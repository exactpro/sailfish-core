/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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

import Baobab from 'baobab';

export function storeFactory({storeId, data = undefined}) {
    const result = new Baobab(
        {
            data: data, // will be [] after matrix load
            isHistoryBlocked: false
        },
        {
            syncwrite: true,  // Applying modifications immediately
            asynchronous: true, // commit on next tick
            storeId: storeId
        }
    );
    // hasHistory && result.startRecording(10);

    Object.defineProperty(result, 'isLoadingInProgress', {
        configurable: false,
        get: function() {
            return this.get('data') === null;
        },
        set: function(value) {
            if (value === true) {
                this.set('data', null);
            }
        }
    });

    Object.defineProperty(result, 'isLoaded', {
        configurable: false,
        get: function() {
            return this.get('data') !== undefined;
        }
    });

    return result;
}

export default storeFactory;
