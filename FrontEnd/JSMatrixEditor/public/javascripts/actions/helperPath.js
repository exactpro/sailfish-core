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
const _ = require('lodash');

const helpers = {
    /**
     *
     * @param {Array|string} path
     * @returns {*}
     */
    getNodeTypeByPath(path) {
        let result = 'notExists'; // default value; for not exist nodes
        if (path !== undefined) {
            const pathAsArr = Array.isArray(path) ? path : this.pathAsArr(path);
            //TODO cover with test
            const lengthToFn = {
                1: function (pathAsArr) {
                    return 'testCase';
                },
                2: function (pathAsArr) {
                    const last = pathAsArr[1];
                    if (last === c.CHILDREN_FIELD) {
                        return 'testCaseItems';
                    } else if (last === c.VALUES_FIELD) {
                        return 'testCaseValues';
                    } else {
                        return 'testCase+';
                    }
                },
                3: function (pathAsArr) {
                    if (lengthToFn[2](pathAsArr) === 'testCaseItems') {
                        return 'action';
                    } else {
                        return 'testCaseField';
                    }
                },
                4: function (pathAsArr) {
                    const last = pathAsArr[3];
                    if (last === c.CHILDREN_FIELD) {
                        return 'actionItems';
                    } else if (last === c.VALUES_FIELD) {
                        return 'actionValues';
                    } else {
                        return 'action+';
                    }
                },
                5: function (pathAsArr) {
                    return 'actionField';
                }
            };

            switch (pathAsArr.length) {
                case 0: result = undefined; break;
                case 1:
                case 2:
                case 3:
                case 4: {
                    const fn = lengthToFn[pathAsArr.length];
                    result = fn && fn(pathAsArr);
                }; break;
                default: {
                    const last = pathAsArr[pathAsArr.length-1];
                    if (last === c.CHILDREN_FIELD) {
                        return 'actionItems';
                    } else if (last === c.VALUES_FIELD) {
                        return 'actionValues';
                    } else {
                        let prev = pathAsArr[pathAsArr.length-2];
                        if (prev === c.CHILDREN_FIELD) {
                            return 'action+';
                        } else if (prev === c.VALUES_FIELD) {
                            return 'actionField';
                        } else {
                            return undefined;
                        }

                    }
                }
            }
        }
        return result;
    },

    getPathToAction(path) {
        const pathAsArr = Array.isArray(path) ? path : this.pathAsArr(path);
        return pathAsArr.slice(0, 3);
    },

    getPathToLastAction(path) {
        const pathAsArr = Array.isArray(path) ? path : this.pathAsArr(path);
        let nodeType = this.getNodeTypeByPath(path);
        if (nodeType == 'actionField') {
            return pathAsArr.slice(0, -2);
        } else if (nodeType == 'action' || nodeType == 'action+') {
            return pathAsArr;
        } else if (nodeType == 'actionValues' || nodeType == 'actionItems' || nodeType == 'testCaseField') {
            return pathAsArr.slice(0, -1);
        } else {
            return pathAsArr.slice(0, 3);
        }
    },

    isInnerAction(path) {
        let nodeType = this.getNodeTypeByPath(getPathToLastAction(path));
        return nodeType == 'action+';
    },

    /**
     *
     * @returns {string}
     */
    createPath() {
        let arr = Array.prototype.slice.call(arguments);
        arr = _.flatten(arr, true);
        return arr.join(c.PATH_SEP);
    },

    /**
     *
     * @param {string} path
     * @returns {Array}
     */
    pathAsArr(path) {
        if (!path && path!==0) {
            return [];
        } else if (Array.isArray(path)) {
            return path;
        }
        return path.split(c.PATH_SEP);
    },

    /**
     *
     * @param {string|Array} path
     * @returns {string}
     */
    pathLastKey(path) {
        const pathAsArr = Array.isArray(path) ? path : this.pathAsArr(path);
        return pathAsArr[pathAsArr.length - 1];
    },

    /**
     *
     * @param {string|Array} path
     * @returns {string}
     */
    pathFirstKey(path) {
        const pathAsArr = Array.isArray(path) ? path : this.pathAsArr(path);
        return pathAsArr[0];
    },

    /**
     *
     * @param {string|Array} path
     * @returns {bool}
     */
    pathIsSingle(path) {
        const pathAsArr = Array.isArray(path) ? path : this.pathAsArr(path);
        return pathAsArr.length === 1;
    },

    /**
     * @param {array} path
     * @returns {string} Expanded path to Block/Action
     */
    expandShortPath(path) {
        if (!path) {
            return null;
        }
        if (!Array.isArray(path)) {
            throw 'path is not array!';
            return;
        }
        // don't modify original array!
        switch (path.length) {
        case 1:
            // path to Block
            return this.createPath(path);
        case 2:
            // path to Action
            return this.createPath(path[0], c.CHILDREN_FIELD, path[1]);
        default:
            let arr = [];
            for (let i = 0; i<path.length; i++) {
                arr.push(path[i])
                if (i != path.length-1) {
                    arr.push(c.CHILDREN_FIELD)
                }
            }
            return this.createPath(arr);
        }
    },

    toShortPath(path) {
        const pathAsArr = Array.isArray(path) ? path : this.pathAsArr(path);

        switch (pathAsArr.length) {
        case 0:
            return [];
        case 1: // [0]
        case 2: // [0, items]
            return [pathAsArr[0]];
        case 3: // [0, items, 0]
        case 4: // [0, items, 0, values]
            return [pathAsArr[0], pathAsArr[2]];
        default:
            let arr = [];
            for (let i = 0; i<pathAsArr.length;) {
                arr.push(pathAsArr[i])
                i+=2;
            }
            return arr;
        }
    },

    /**
     *
     * @param {string|Array} path
     * @returns {string}
     */
    parentPath(path) {
        if (Array.isArray(path)) {
            return path.slice(0, -1);
        } else {
            return path.substring(0, path.lastIndexOf(c.PATH_SEP));
        }
    }
};

helpers.pathToAction = helpers.getPathToAction;
helpers.pathParent = helpers.parentPath;
helpers.pathShort = helpers.toShortPath;
helpers.pathExpandShort = helpers.expandShortPath;
helpers.pathToAction = helpers.getPathToAction;
helpers.pathCreate = helpers.createPath;

export default helpers;
