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

import {ITEM_SEP} from 'consts.js';

module.exports = {
    /**
     *
     * @returns {string}
     */
    getEmptyExpanded: function() {
        return '';
    },

    /**
     *
     * @param nodeKey
     * @param {string} expandedCfg
     * @returns {boolean}
     */
    isRowExpanded: function(nodeKey, expandedCfg) {
        return expandedCfg.indexOf(ITEM_SEP + nodeKey) >= 0;
    },

    /**
     *
     * @param nodeKey
     * @param {string} expandedCfg
     * @returns {string}
     */
    collapseRow: function(nodeKey, expandedCfg) {
        if (expandedCfg.indexOf(ITEM_SEP + nodeKey) >= 0) {
            var array = expandedCfg.split(ITEM_SEP).slice(1); //skip first empty string
            var result = ITEM_SEP + array.filter((value) => !value.startsWith(nodeKey)).join(ITEM_SEP);
            return (result.length === 1) ? this.getEmptyExpanded() : result; // remove last ITEM_SEP
        }
        return expandedCfg;
    },

    /**
     *
     * @param nodeKey
     * @param {string} expandedCfg
     * @returns {string}
     */
    expandRow: function(nodeKey, expandedCfg) {
        nodeKey = '' + nodeKey;
        var result = expandedCfg;
        if (result) {
            var array = result.split(ITEM_SEP).slice(1); //skip first empty string
            var parentNodeIdx = -1;
            if (array.indexOf(nodeKey) === -1) {
                if (array.some(function (x, idx) {
                    if (nodeKey.indexOf(x + ITEM_SEP) === 0) {
                        parentNodeIdx = idx;
                        return true;
                    } else {
                        return false;
                    }
                })) {
                    array.splice(parentNodeIdx, 1);
                    result = ITEM_SEP + array.join(ITEM_SEP);
                } else {
                    result += ITEM_SEP + nodeKey;
                }
            }
        } else {
            result = ITEM_SEP + nodeKey;
        }
        return result;
    }
};
