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

//
import * as c from 'consts';
let __uniqId = 0;
function _getNextNodeKey() {
    return (__uniqId++).toString(36);
}

 /*
 * @param {array|object} item(s)
 */
export function assignNodeKeys(items) {
    if (Array.isArray(items)) {
        items.forEach( el => {
            el.key = el.key ||_getNextNodeKey();
            if (el.items) {
                assignNodeKeys(el.items);
            }
        });
    } else if (typeof items === 'object') {
        if (!items.key) {
            items.key = items.key || _getNextNodeKey();
        }
        if (items.items) {
            assignNodeKeys(items.items);
        }
    }
    return items;
}

//
export function polymorphCfg(scope, args, applyToScope = false) {
    let result;
    if (args.length === 1) {
        if (applyToScope) {
            result = Object.assign(scope, args[0]);
        } else {
            result = args[0];
        }
    } else {
        result = createCtrConfig(applyToScope ? scope : {},
                        scope.constructor.ctrParams, args);
    }
    return result;
}
function createCtrConfig(dest, params, args) {
    for (let i = 0, len = params.length; i < len; i++) {
        dest[params[i]] = args[i];
    }
    return dest;
}

//
const stripper = document.createElement('div');

export function stripText(text) {
    stripper.innerHTML = text;
    const result = stripper.textContent || stripper.innerText || '';
    stripper.innerHTML = '';
    return result;
}

//
let ref_i = 0;
export function getNextRefName() {
    return 'ref_' + (ref_i++) + '_' + Date.now().toString(36);
}

//
export class DelayedTask {
    _getMaxDelayEnd(maxDelay) {
        return maxDelay > 0 ? (new Date()).getMilliseconds() + maxDelay : -1;
    }

    /**
     *
     * @param {function} task ms
     * @param {number} [maxDelay]
     */
    constructor(task, maxDelay = -1) {
        this.maxDelay = maxDelay;
        this._maxDelayEndTime = null;
        this.task = () => {
            task();
            this._maxDelayEndTime = null;
        };
        this.worker = null;
        this.data = {};
    }

    /**
     *
     * @param {number} ms or -1 to cancel
     */
    delay(ms) {
        if (this.worker !== null) {
            clearTimeout(this.worker);
            this.worker = null;
        }

        if (ms < 0) {
            return;
        }

        if (this._maxDelayEndTime === null) {
            this._maxDelayEndTime = this._getMaxDelayEnd(this.maxDelay);
        } else if (this._maxDelayEndTime > 0) {
            const now = (new Date()).getMilliseconds();
            const remain = this._maxDelayEndTime - now;
            if (remain < ms) {
                ms = remain;
            }
        }

        if (ms > 0) {
            this.worker = setTimeout(this.task, ms);
        } else {
            this.task();
        }
    }

    release() {
        this.delay(-1);
        this.task = null;
        this.data = {};
    }
}

//
export function EpsAppError() {}
EpsAppError.prototype = Object.create(Error.prototype);

const hashCode = function(str){
    var hash = 0;
    if (str.length == 0) return hash;
    for (let i = 0; i < str.length; i++) {
        let char = str.charCodeAt(i);
        hash = 31*hash + char;
        hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
}


export function getColorClass(msgName) {
    let i = hashCode(msgName) % c.COLOR_LIBRARY.length;
    if (i<0) {
        i *= -1;
    }
    return c.COLOR_LIBRARY[i];
}
