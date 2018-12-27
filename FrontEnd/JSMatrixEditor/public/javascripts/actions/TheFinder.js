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

import * as c from 'consts.js';

import BaseAction from './Base.js';
import TheStoreList from 'state/TheMatrixList.js';
import TheAppState from 'state/TheAppState.js';
import TheHelper from 'actions/TheHelper.js';

import {
    CURRENT_SEARCH_RESULT,
    CONTAINS_CURRENT_SEARCH_RESULT,
    SEARCH_RESULT,
    CONTAINS_SEARCH_RESULT
} from 'consts';

// re-export:
export {
    CURRENT_SEARCH_RESULT,
    CONTAINS_CURRENT_SEARCH_RESULT,
    SEARCH_RESULT,
    CONTAINS_SEARCH_RESULT
} from 'consts';

class Finder extends BaseAction {
    constructor(...args) {
        super(...args);
        this.__searchCache = null;
    }

    /**
     *
     * @param {string} panelId left|right|...
     * @param {string} pattern
     * @param {array}  fields to search in
     */
    find(panelId, searchFor, searchIn) {
        const cursor = TheAppState.select('node', panelId, 'search');

        this.cleanSearchResults(panelId);

        // if (!searchFor)
        //     return;
        //
        // if (!Array.isArray(searchIn))
        //     throw 'IllegalArgument: searchIn must be array';

        let results = [];

        const storeId = TheAppState.get('panels', 'items', panelId, 'storeId');
        const blocks = TheStoreList[storeId].select('data').get();

        const blocksCount = blocks.length;
        for (let i=0; i<blocksCount; i++) {
            let block = blocks[i];
            // search in Block fields:
            let values = block[c.VALUES_FIELD];
            for (let key in values) {
                if (searchIn.length > 0 && searchIn.indexOf(key) === -1) {
                    continue;
                }
                let value = values[key];
                if (value.indexOf(searchFor) !== -1) {
                    results.push(TheHelper.createPath(i, c.VALUES_FIELD, key));
                }
            }

            let actions = block[c.CHILDREN_FIELD];
            let actionsCount = actions.length;
            for (let j=0; j<actionsCount; j++) {
                let action = actions[j];
                let path = [i, c.CHILDREN_FIELD, j];
                results = results.concat(this._getActionSearchResult(action, path, searchIn, searchFor));
            }
        }

        cursor.set({
            results: results,
            path: '',
            pathPointer: 0
        });

        TheAppState.commit(); // FIXME: don't store it to history?
        this.gotoNextResult(panelId);
    }

    _getActionSearchResult(action, path, searchIn, searchFor) {
        var results = [];
        let values = action[c.VALUES_FIELD];
        for (let key in values) {
            if (searchIn.length > 0 && searchIn.indexOf(key) === -1) {
                continue;
            }
            let value = values[key];
            if (value.indexOf(searchFor) !== -1) {
                results.push(TheHelper.createPath(path.concat([c.VALUES_FIELD, key])));
            }
        }
        let items = action[c.CHILDREN_FIELD];
        if (items && items.length) {
            for (let k=0; k< items.length; k++) {
                results = results.concat(this._getActionSearchResult(items[k], path.concat(c.CHILDREN_FIELD, k),  searchIn, searchFor))
            }
        }
        return results;
    }


    /**
     *
     * @param {string} panelId left|right|...
     */
    cleanSearchResults(panelId) {
        const cursor = TheAppState.select('node', panelId, 'search');

        cursor.set({
            results: [],
            path: '',
            pathPointer: 0
        });
    }


    /**
     * @param reactComponent
     * @param {Cursor} [nodeCursor] cursor with path node/panelId
     * @returns {number} srStatus
     */
    getSearchStatus(reactComponent, panelId, nodeCursor = TheAppState.select('node', panelId)) {
        // pathPointer point to start target node path
        // This method can be called in any order (? from BaseNode.componentDidMount->update->_updateVisibility ?)
        const reactCompProps = reactComponent.props;
        const nodePath = reactCompProps.path;

        const isServiceNode = reactCompProps.isServiceNode;
        let startIdx, endIdx;
        if (isServiceNode) {
            startIdx = reactCompProps.body && reactCompProps.body.props && reactCompProps.body.props.startIdx;
            endIdx = reactCompProps.body && reactCompProps.body.props && reactCompProps.body.props.endIdx;
        }

        const result = this.__getSearchStatus(panelId, nodePath, startIdx, endIdx);
        if (result >= CURRENT_SEARCH_RESULT && !isServiceNode) {
            const searchCursor = nodeCursor.select('search');
            const searchCursorValue = searchCursor.get();
            const pathPointer = searchCursorValue.pathPointer;
            this.__increasePathPointer(searchCursor, nodePath, pathPointer);
        }

        return result;
    }

    /**
     *
     * @param {string} panelId left|right
     * @param {boolean} [back]
     */
    gotoNextResult(panelId, back) {
        const searchCursor = TheAppState.select('node', panelId, 'search');
        const searchValue = searchCursor.get();
        const lastResultIdx = searchValue.results.length - 1;
        let result;
        if (searchValue.results.length > 0) {
            if (searchValue.path === '') {
                result = back ? searchValue.results[lastResultIdx] : searchValue.results[0];
            } else {
                let nextIdx = searchValue.results.indexOf(searchValue.path) + (back ? -1 : 1);
                if (nextIdx < 0) {
                    nextIdx = lastResultIdx;
                } else if (nextIdx > lastResultIdx) {
                    nextIdx = 0;
                }
                result = searchValue.results[nextIdx];
            }
        } else {
            result = '';
        }
        if (searchValue.path !== result) {
            console.debug('Next search result: ' + result);
            searchCursor.merge({
                path: result,
                pathPointer: 0
            });
        }
    }

        /**
     *
     * @param {string} panelId left|right
     */
    getCurrentSearchPath(panelId) {
        const searchCursor = TheAppState.select('node', panelId, 'search');
        const searchValue = searchCursor.get();
        return searchValue.path;
    }

    __increasePathPointer(searchCursor, nodePath, pathPointer) {
        if (nodePath.length > pathPointer) {
            const newPathPointer = nodePath.length + 1;
            searchCursor.set('pathPointer', newPathPointer);
            return true;
        } else {
            return false;
        }
        //note than searchResultPath always points to propGrid
    }

    __updateSearchCache(panelId) {
        if (this.__searchCache === null || panelId !== this.__searchCache.panelId) {
            const state = TheAppState.select('node', panelId, 'search').get();
            const ITEM_SEP = c.ITEM_SEP;
            const PATH_SEP = c.PATH_SEP;
            const CHILDREN_FIELD = c.CHILDREN_FIELD + PATH_SEP;
            const SEP = PATH_SEP + ITEM_SEP;
            this.__searchCache = {
                panelId: panelId,
                curr: state.path,
                results: state.results,
                state: state,
                resultString: ITEM_SEP + state.results.join(SEP) + SEP,
                ITEM_SEP: ITEM_SEP,
                PATH_SEP: PATH_SEP,
                CHILDREN_FIELD: CHILDREN_FIELD,
                CHILDREN_FIELD_LEN: CHILDREN_FIELD.length,
                PROPS_PATH_START: TheHelper.createPath('', c.VALUES_FIELD, '')
            };
            setTimeout(() => this.__searchCache = null, 0);
        }
        return this.__searchCache;
    }

    /**
     *
     * @param panelId
     * @param path
     * @param [startIdx]
     * @param [endIdx]
     * @returns number 0, 1, 2 or 3 (0, SEARCH_RESULT, CONTAINS_SEARCH_RESULT or CURRENT_SEARCH_RESULT)
     */
    __getSearchStatus(panelId, path, startIdx, endIdx) {
        const cache = this.__updateSearchCache(panelId);
        let result = 0;
        if (cache.curr !== '') {
            // service node couldn't be current result
            if (startIdx === undefined && this.__isCurrentSearchResult(cache, path)) {
                result = CURRENT_SEARCH_RESULT;
            } else if (this.__isContainsCurrentSearchResult(cache, path, startIdx, endIdx)) {
                result = CONTAINS_CURRENT_SEARCH_RESULT;
            } else if (cache.results.indexOf(path) > -1) {
                result = SEARCH_RESULT;
            } else if (this.__isContainsSearchResult(cache, path, startIdx, endIdx)) {
                result = CONTAINS_SEARCH_RESULT;
            }
        }

        return result;
    }

    /**
     *
     * @param cache
     * @param path
     * @param [startIdx]
     * @param [endIdx]
     * @returns {boolean}
     * @private
     */
    __isContainsCurrentSearchResult(cache, path, startIdx, endIdx) {
        let result = false;
        if (cache.curr.startsWith(path)) {
            const pathLen = path.length + 1;
            //if startIdx - service node
            if (startIdx === undefined) {
                result = true;
            } else {
                if (cache.curr.substring(pathLen).startsWith(cache.CHILDREN_FIELD)) {
                    const startPos = pathLen + cache.CHILDREN_FIELD_LEN;
                    const idx = cache.curr.substring(startPos, cache.curr.indexOf(cache.PATH_SEP, startPos));
                    result = idx >= startIdx && idx <= endIdx;
                } else {
                    result = false;
                }
            }
        }
        return result;
    }

    __isCurrentSearchResult(cache, path) {
        let result;
        if (cache.curr === path) {
            result = true;
        } else {
            const lastIdx = cache.curr.lastIndexOf(cache.PROPS_PATH_START);
            result = cache.curr.substring(0, lastIdx) === path;
        }
        return result;
    }

    //__checkLastNestedNode(cache, searchResultPath, newPathPointer) {
    //    return searchResultPath.indexOf(cache.PROPS_PATH_START) === newPathPointer;
    //}

    __isContainsSearchResult(cache, path, startIdx, endIdx) {
        let result;
        if (startIdx === undefined) {
            result = cache.resultString.indexOf(cache.ITEM_SEP + path + cache.PATH_SEP) > -1;
        } else {
            result = this.__checkTargetServiceNode(cache, path, startIdx, endIdx);
        }
        return result;
    }

    __checkTargetServiceNode(cache, path, startIdx, endIdx) {
        const items_path = TheHelper.createPath(path, cache.CHILDREN_FIELD);
        const SEP = cache.PATH_SEP;
        const firstIdx = items_path.length;

        const indexes = cache.results.
            map(x => {
                if (x.startsWith(path)) {
                    const sepIdx = x.indexOf(SEP, firstIdx);
                    return +x.substring(firstIdx, sepIdx);
                }
                return false;
            }).filter( x => x );

        return indexes.some( x => x >= startIdx && x <= endIdx );
    }

    /**
     * check that node includes defined path
     * @param reactComponent node component
     * @param {string} path
     */
    isOnPath(reactComponent, path) {
        let result = false;
        if (path) {
            const props = reactComponent.props;
            const nodePath = props.path + c.PATH_SEP;
            if (path.startsWith(nodePath)) {
                if (props.isServiceNode) {
                    const actionSetProps = props.body && props.body.props;
                    if (actionSetProps) {
                        const startIdx = props.body.props.startIdx;
                        const endIdx = props.body.props.endIdx;
                        const startFrom = nodePath.length + c.CHILDREN_FIELD.length + 1;
                        const end = path.indexOf(c.PATH_SEP, startFrom);
                        const idx = end === -1 ?
                            + path.substring(startFrom) :
                            + path.substring(startFrom, end);
                        result = startIdx <= idx && endIdx >= idx;
                    }
                } else {
                    result = true;
                }
            }
        }
        return result;
    }
}

export const TheFinder = new Finder();
export default TheFinder;
