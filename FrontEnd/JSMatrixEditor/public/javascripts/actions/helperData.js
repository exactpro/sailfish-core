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
import * as _ from 'lodash';
import {DataItem} from './__models__.js';
import TheDictHelper from 'actions/TheDictManager.js';
import TheStoreList, {getMainStoreId} from 'state/TheMatrixList.js';
import TheHelper from 'actions/TheHelper.js';
import {getNextRefName} from 'utils.js';

export const helpers = {
    isRefCollection(path, value, definition) {
        return definition && definition.isCollection;
    },

    isBlockInclude(path, value, definition) {
        return definition && definition.includeBlock;
    },

    isSubMessage(path, value, definition) {
        return definition && definition.type === 'SUBMESSAGE';
    },

    getFieldReferences(path, value, definition) {
        if (this.isRefCollection(path, value, definition)) {
            return this.getValuesAsArr(value);
        } else if (this.isBlockInclude(path, value, definition)) {
            return [value];
        } else if (this.isSubMessage(path, value, definition)) {
            if (value!=undefined) {
                if (value[0]=='[') {
                    return [value.slice(1,-1)];
                } else {
                    return [value.slice(2,-1)];
                }
            } else {
                return [];
            }
        }
        return [];
    },

    /**
     * get nested data for node at the path
     * @param {string} path
     * @param value
     * @param definition
     * @returns {*}
     */
    getNestedData(path, value, definition, storeId) {
        let result;
        if (path !== undefined) {
            const nodeTypeToFn = {
                'actionField': this.getNestedActions,
                'action': this.getNestedFields,
                'action+': this.getNestedFields,
                'testCaseValues': this.getFields,
                'actionValues': this.actionValues
            };

            const pathAsArr = this.pathAsArr(path);
            const fn = nodeTypeToFn[this.getNodeTypeByPath(pathAsArr)];
            const storeCursor = TheStoreList[storeId].select('data');
            result = fn && fn.call(this, storeCursor, pathAsArr, value, definition, storeId);
        }
        return result;
    },

    /**
     * iterate over all actions in tescase above path:
     *  from first action in test case to defined path (exclude) or until predicate return true
     * @param  {Baobab.Cursor} storeCursor
     * @param  {Array} pathAsArr
     * @param  {function} predicate
     * @return {number}
     */
    eachActionAbove(storeCursor, pathAsArr, predicate) {
        const actionList = this.getPreviousElementaryActions(this.getPathToLastAction(pathAsArr), storeCursor, true);
        let i = 0;
        actionList.some((x, idx) => {
            i++;
            return predicate(x, idx);
        });
        return i;
    },

    allActionAbove(storeId, pathAsArr, predicate) {
        const storeCursor = TheStoreList[storeId];
        const testcases = storeCursor.select('data').get();
        const pathToAction = this.getPathToLastAction(pathAsArr);
        const tillTestCase = +pathToAction[0];
        const tillAction = +pathToAction[1];

        var tcIdx = 0,
            actionIdx = 0;
        for (var i=0; i<=tillTestCase; i++) {
            for (var j=0; j<testcases[i].items.length; j++) {
                if (i == tillTestCase && j == tillAction) {
                    return undefined;
                }

                var action = testcases[i].items[j];
                var check = predicate(action, i, j);
                if (check) {
                    return check;
                }
            }
        }
    },


    /**
     * iterate over all blocks until predicate return true
     * @param  {Baobab.Cursor} store
     * @param  {Function} fn
     * @return {boolean}
     */
    __eachBlock(storeCursor, predicate) {
        const tcList = storeCursor.get('data');
        return tcList.some((x, idx) => predicate(x, idx));
    },

    /**
     * get nested action for action field
     * @param {Baobab.Cursor} store
     * @param {Array} pathAsArr
     * @param value
     * @param {MessageDefinition} definition
     * @returns {*}
     */
    getNestedActions(storeCursor, pathAsArr, value, definition, storeId) {
        const testCaseItemsPath = this.createPath(pathAsArr[0], c.CHILDREN_FIELD);
        let results;
        const itemDefinition = TheDictHelper.findNestedDefinition(definition);

        const refs = this.getFieldReferences(pathAsArr, value, definition);
        const missed = [];
        if (refs.length > 0) {
            results = [];
            // substitute actions
            refs.forEach( ref => {
                let foundAction,
                    foundIdx;
                this.eachActionAbove(storeCursor, pathAsArr, (action, idx) => {
                    if (action[c.VALUES_FIELD][c.REFERENCE_FIELD] === ref
                        && action[c.VALUES_FIELD]['#action'] !== 'Include block') {

                        foundAction = action;
                        foundIdx = idx;
                        return true;
                    }
                });
                if (foundAction) {
                    const path = this.createPath(testCaseItemsPath, foundIdx);
                    results.push(new DataItem(
                        path,
                        foundAction,
                        itemDefinition || TheDictHelper.findDefinition(foundAction, storeId, path))
                    );
                } else {
                    missed.push(ref);
                    results.push(new DataItem(undefined, ref, itemDefinition));
                }
            });

            results.missed = missed;
        }
        return results;
    },

    getNestedFields(storeCursor, pathAsArr, value, definition, storeId) {
        const cursor =  storeCursor.select(pathAsArr);
        const record = cursor.get();
        if (!record) {
            return undefined;
        }
        const values = record[c.VALUES_FIELD];
        const errors = record['errors'];
        return this.getFields(storeCursor, pathAsArr.concat(c.VALUES_FIELD), values, definition, storeId, errors);
    },

    actionValues(storeCursor, pathAsArr, values, definition, storeId) {
        pathAsArr.pop();
        return this.getNestedFields(storeCursor, pathAsArr, undefined, definition, storeId);
    },

    //pathAsArr - path ONLY to ACTION/ACTION+.
    getPreviousElementaryActions(pathAsArr, storeCursor, onlyInSameTestcase = true) {
        var result = [];
        const testCaseCursor = (onlyInSameTestcase) ? storeCursor.select(pathAsArr[0], c.CHILDREN_FIELD) : storeCursor;
        const indexes = (onlyInSameTestcase) ? pathAsArr.slice(1) : pathAsArr;
        _.remove(indexes, (key) => {
            return Number.isNaN(+key);
        });
        let path = [];
        for (let i=0; i< indexes.length; i++) {
            for (let j=0; j < indexes[i]; j++) {
                let pathToAction = path.concat([j, c.CHILDREN_FIELD]);
                result.push(testCaseCursor.get(path.concat([j])));
                result = result.concat(this.__getInnerActions(testCaseCursor, pathToAction));
            }
            path = path.concat([indexes[i], c.CHILDREN_FIELD]);
        }
        result = _.compact(result);
        return result;

    },

    __getInnerActions(tcCursor, pathToBlock) {
        let result = [];
        const actions = tcCursor.get(pathToBlock);
        if (!actions || !actions.length)
            return [];
        for (let i=0; i< actions.length; i++) {
            if (!actions[i].items || !actions[i].items.length) {
                result.push(actions[i]);
            } else {
                result = result.concat(this.__getInnerActions(tcCursor, pathToBlock.concat(i, c.CHILDREN_FIELD)));
            }
        }
        return result;
    },

    /**
     * [getFields description]
     * @param  {[type]} storeCursor [description]
     * @param  {[type]} pathAsArr   [description]
     * @param  {[type]} values      [description]
     * @param  {[type]} definition  [description]
     * @param  {string} storeId
     * @param  {Array} errors
     * @return {[DataItem]}
     */
    getFields(storeCursor, pathAsArr, values, definition, storeId, errors = []) {
        const path = this.createPath(pathAsArr);

        const declaredFields = definition.fields || {};
        const keys = Object.keys(values);
        const declaredKeys = Object.keys(declaredFields);

        declaredKeys.forEach( key => {
            const fieldDefinition = declaredFields[key];
            if (fieldDefinition.isRequired && !Object.prototype.hasOwnProperty.call(values, key)) {
                keys.push(key);
            }
        });

        const errorsMap = {};
        errors.forEach(error => {
            errorsMap[error.column] = errorsMap[error.column] || [];
            errorsMap[error.column].push(error);
            return true;
        });

        let m4 = keys.sort((k1, k2) => {
                //system columns should be in top
                if (k1[0] == "#" && k2[0]!="#") {
                    return -1;
                }
                if (k2[0] == "#" && k1[0]!="#") {
                    return 1;
                }
                //unknown fields should be last
                if (!declaredFields[k1] && declaredFields[k2] !== undefined) {
                    return 1;
                }
                if (!declaredFields[k2] && declaredFields[k1] !== undefined) {
                    return -1;
                }
                //empty values should be in bottom
                if (!values[k1] && values[k2] !== undefined) {
                    return 1;
                }
                if (!values[k2] && values[k1] !== undefined) {
                    return -1;
                }
                //compare action names
                if (k1 > k2) {
                    return 1;
                }
                if (k1 < k2) {
                    return -1;
                }
                return 0;
        });
        return m4.map(key => new DataItem(
            this.createPath(path, key),
            values[key],
            declaredFields[key],
            errorsMap[key]
        ));
    },

    getValueAsString(valuesAsArr) {
        if (valuesAsArr.length === 0) {
            return '';
        } else {
            return '[' + valuesAsArr.join(', ') + ']';
        }
    },

    getValuesAsArr(valueAsStr) {
        if (valueAsStr == null || valueAsStr === '' || valueAsStr === '[]') {
            return [];
        } else {
            return valueAsStr.substring(1, valueAsStr.length - 1).replace(/ /g, '').split(',');
        }
    },

    getActionTemplate(actionName, referenceName, path) {
        const result = {};
        if (actionName) {
            result.values = {
                '#action': actionName
            };
            if (path && (this.getNodeTypeByPath(this.props.path) === 'testCase' || this.isBlockAction(actionName))) {
                result.items = [];
            }
        } else {
            result.values = {
                '#reference': referenceName || getNextRefName()
            };
        }

        result.errors = [];
        result.metadata = {};

        return result;
    },

    getConditionalTemplate(actionName) {
        const result = {};
        if (actionName) {
            result.values = {
                '#action': actionName
            };
            result.items = [];
        }
        result.errors = [];
        result.metadata = {};

        return result;
    },

    isConditionalAction(actionName) {
        if (typeof(actionName) == 'string') {
            return actionName.toLowerCase() === "if" || actionName.toLowerCase() === "elif" || actionName.toLowerCase() === "else";
        }
        if (typeof(actionName) == 'object') {
            actionName = actionName['#action'];
            return this.isConditionalAction(actionName);
        }
        return false;
    },

    isBlockAction(actionName) {
        return actionName && typeof(actionName) == 'string' && (actionName.toLowerCase() === "repeat" || actionName.toLowerCase() === "if"
            || actionName.toLowerCase() === "elif" || actionName.toLowerCase() === "else");
    },

    isIfContainer(values) {
        return !values["#action"] && !values["#reference"];
    },

    getValuesByPath(path, storeId) {
        const pathAsArr = Array.isArray(path) ? path : this.pathAsArr(path);
        return TheStoreList[storeId].select('data').select(pathAsArr).get();
    },

    /**
     *
     * @param  {Object} actionValues
     * @param  {string} storeId
     * @return {string}
     */
    findIncludeBlockPath(actionValues, storeId) {
        let result;

        const store = TheStoreList[storeId];
        const name = actionValues['#action'] || '';
        const template = actionValues['#template'] || '';

        if (name.toLowerCase() === 'include block' && actionValues['#template'] !== '') {
            TheHelper.__eachBlock(store, (block, idx) => {
                if (block[c.VALUES_FIELD]['#reference'] === template) {
                    result = '' + idx;
                    return true;
                }
            });
        }

        return result;
    },

    suri2string(suri) {
      let result = '';
      if (suri.pluginAlias) {
        result += suri.pluginAlias + ':';
      }
      if (suri.classAlias != null) {
        result += suri.classAlias + '.';
      }
      if (suri.resourceName != null) {
        result += suri.resourceName;
      }
      return result;
    },

    string2suri(uri) {
      let suri = {
        pluginAlias: null,
        classAlias: null,
        resourceName: null
      };

      throw new Error('Unsupported operation');
    }

};

export default helpers;
