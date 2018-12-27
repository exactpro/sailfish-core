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

import * as c from 'consts';
import * as _ from 'lodash';
import {AutoCompleteOption} from 'actions/__models__.js';
import BaseAction from './Base.js';
import TheHelper from 'actions/TheHelper.js';
import {TheServer} from 'actions/TheServer.js';
import {TheDictManager} from 'actions/TheDictManager.js';
import TheAppState, {getMainStoreId} from 'state/TheAppState.js';
import TheStoreList from 'state/TheMatrixList.js';

const STRING_LIMIT = 50;
const BEGIN_REFERENCE = '${';
const END_REFERENCE = '}';
const BEGIN_STATIC = '%{';
const END_STATIC = '}';
const BEGIN_FUNCTION = '#{';
const END_FUNCTION = '}';
const BEGIN_REPEATING = '[';
const END_REPEATING = ']';

//import {MessageDefinition, FieldDefinition, DataItem} from './__models__.js';

function limitString(str) {
    if (str.length > STRING_LIMIT) {
        return str.substr(0, STRING_LIMIT) + '...';
    } else {
        return str;
    }
}

class Render extends BaseAction {
    constructor(...args) {
        super(...args);
    }

    /**
     *
     * @param path
     * @param value
     * @param {FieldDefinition} definition
     * @returns {string}
     */
    getRenderedType(path, value, definition) {
        if (!definition) {
            return '';
        }
        var typ = {
            SUBBLOCK: 'block:',
            SUBMESSAGE: 'msg:' + definition.targetRefType,
            BOOLEAN: 'bool',
            SHORT: 'short',
            INT: 'int',
            LONG: 'long',
            BYTE: 'byte',
            FLOAT: 'float',
            DOUBLE: 'double',
            STRING: 'str',
            DATE: 'date',
            CHAR: 'char',
            DECIMAL: 'dec',
            DATETIMEONLY: 'hh:mm',
            DATEWITHOUTTIME: 'd.m.y',
            JAVA_LANG_STRING: 'str',
            JAVA_LANG_BOOLEAN: 'bool',
            JAVA_LANG_CHARACTER: 'char',
            JAVA_LANG_BYTE: 'byte',
            JAVA_UTIL_DATE: 'date',
            JAVA_MATH_BIG_DECIMAL: 'bdec',
            JAVA_LANG_DOUBLE: 'double',
            JAVA_LANG_FLOAT: 'float',
            JAVA_LANG_INTEGER: 'int',
            JAVA_LANG_LONG: 'long',
            JAVA_LANG_SHORT: 'short',
            COM_EXACTPROSYSTEMS_COMMON_UTIL_DATE_WITHOUT_TIME: 'd.m.y',
            COM_EXACTPROSYSTEMS_COMMON_UTIL_DATE_TIME_ONLY: 'hh:mm'
        }[definition.type];

        if (definition.isCollection) {
            typ = '[' + typ + ']';
        }
        return typ;
    }

    /**
     *
     * @param value
     * @param {FieldDefinition} definition
     * @returns {string}
     */
    castValue(value, definition) {
        if (!definition || !definition.type) {
            return value;
        }

        if (definition.isCollection) {
            return value;
        }
        if (typeof value !== 'string') {
            return value;
        }

        // com.exactpro.sf.aml.generator.NewImpl.REGEX_MVEL_DELIMETER
        // let REGEX_MVEL_STRING = '(\\"[^\\"]*\\")+|(\'[^\']*\')+';
        // let REGEX_MVEL_DELIMETER = '(\\+|-|\\*|\\/| |\\(|\\)|=|>|<|!|&|\\||~|%|,|\\?|:)+';
        // let REGEX_MVEL_NOT_VARIABLE = new RegExp('(' + REGEX_MVEL_STRING + '|' + REGEX_MVEL_DELIMETER + '|\\$)+');
        // if (value.match(REGEX_MVEL_NOT_VARIABLE) !== null) {
        //     return value;
        // }


        switch (definition.type) {
        case 'SUBBLOCK':
        case 'SUBMESSAGE':
        case 'STRING':
        case 'DATE':
        case 'DATETIMEONLY':
        case 'DATEWITHOUTTIME':
            return value;

        case 'BOOLEAN':
        case 'SHORT':
        case 'INT':
        case 'LONG':
        case 'BYTE':
        case 'FLOAT':
        case 'DOUBLE':
        case 'CHAR':
            return '(' + definition.type.toLowerCase() + ') (' + value + ')';

        case 'DECIMAL':
            return 'toBigDecimal(' + value + ')';
        default:
            throw new Error('Unknown field type ' + definition.type);
        }
    }

    /**
     *
     * @param path
     * @param value
     * @param {MessageDefinition} definition
     * @param storeId
     * @param position
     * @returns {string[]}
     */
    getAutocomplete(path, value, definition, position = value.length, storeId = getMainStoreId()) {
        const nodeType = TheHelper.getNodeTypeByPath(path);
        const store = TheStoreList[storeId];

        var result = {};
        var startAutocomplete = 0;
        var endAutocomplete = value.length;
        const language = TheDictManager.getLanguage();
        var visibleTemplates = false;

        if (nodeType === 'testCaseValues') {
            // new Field in TestCase
            Object.assign(result, {
                '#id': new AutoCompleteOption({value: '#id'}),
                '#action': new AutoCompleteOption({value: '#action'}),
                '#description': new AutoCompleteOption({value: '#description'}),
                '#execute': new AutoCompleteOption({value: '#execute'}),
                '#reference': new AutoCompleteOption({value: 'reference'}),
                '#fail_on_unexpected_message': new AutoCompleteOption({value: '#fail_on_unexpected_message'})
            });
        } else if (nodeType === 'testCaseField') {
            let field = TheHelper.pathLastKey(path);
            if (field === '#action') {
                Object.assign(result, {
                    // refer to TestTools/com.exactpro.sf.aml.AMLBlockBrace
                    'Test Case Start': new AutoCompleteOption({value: 'Test Case Start'}),
                    'Global Block start': new AutoCompleteOption({value: 'Global Block start', help: 'Actions from this block will be substituted to each test case as first actions'}),
                    'Before Test Case Block start': new AutoCompleteOption({value: 'Before Test Case Block start', help: 'Actions from this block will be executed <i>before every</b> test case (as @Before)'}),
                    'After Test Case Block start': new AutoCompleteOption({value: 'After Test Case Block start', help: 'Actions from this block will be executed <i>after every</i> test case (as @After)'}),
                    'Block Start': new AutoCompleteOption({value: 'Block Start', help: 'Actions from this block can be substituted to test case by "Include block" action'}),
                    'First Block Start': new AutoCompleteOption({value: 'First Block Start', help: 'Actions from this block will be executed <i>before</i> first test case in matrix (as @BeforeClass)<br>Executed as separate test case}'}),
                    'Last Block Start': new AutoCompleteOption({value: 'Last Block Start', help: 'Actions from this block will be executed <i>after</i> last test case in matrix (as @AfterClass)<br>Executed as separate test case'})
                });
            }
        } else if (nodeType === 'actionValues') {
            // new Field in Action
            // refer to com.exactpro.sf.aml.generator.matrix.Column
            var columns = language && language.columns;
            result = {};
            if (columns) {
                columns.forEach((x) => result[x] = 1);
            }

            if (definition && definition.fields) {
                Object.assign(result, definition.fields);
            }
        } else if (nodeType == 'actionField') {
            // new Value in action
            let field = TheHelper.pathLastKey(path);
            if (field === '#action') {
                Object.assign(result, TheDictManager.getAllActions().reduce((acc, action) => {
                    acc[action.name] = new AutoCompleteOption({value: action.name, help: action.help});
                    return acc;
                }, {}));
            } else if (field === '#message_type') {
                // let action = store.select('data').select(TheHelper.pathToAction(path)).get();
                let pathAsArr = TheHelper.getPathToLastAction(path);
                let dictName = TheDictManager.getDictionaryNameFromAction(storeId, pathAsArr);
                if (dictName) {
                    Object.assign(result, TheDictManager.getMessages(dictName));
                }
            } else if (field === '#dictionary') {
                Object.assign(result, TheDictManager.getAllDictionaries());
            }  else if (field === '#service_name') {
                Object.assign(result, TheDictManager.getAllServices());
                // aliases from DefineServiceName:
                Object.assign(result, TheDictManager.getAllServiceAliases(storeId, path));
            } else if (field[0] !== '#') {
                visibleTemplates = true;
                const pathArr = TheHelper.pathAsArr(path);
                const cursor = store.select('data').select(pathArr);

                let braceIdx = -1, braceType = null, tmp = null;
                let shortValue = value.substring(0, position);
                // search last brace
                const regex = /[$#%]{1}\{/gi;
                while (tmp = regex.exec(shortValue)) {
                   braceIdx = regex.lastIndex - 2;
                   braceType = tmp[0];
                }
                if (!braceType && shortValue.indexOf(BEGIN_REPEATING) >= 0) {
                    braceIdx = shortValue.indexOf(BEGIN_REPEATING);
                    braceType = BEGIN_REPEATING;
                }
                shortValue = value.substr(braceIdx);

                const actions = TheHelper.getPreviousElementaryActions(TheHelper.getPathToLastAction(pathArr), store.select('data'), braceType != BEGIN_STATIC);
                // const currentAction = cursor.up().up().get();
                const limit = actions.length;

                const parts = shortValue.split(/[.:]{1}/);
                if (parts.length > 0) {
                    let prefix = parts[0];
                    if (braceType != null)
                        parts[0] = prefix.substring(2);
                }
                for (let idx = 1; idx < parts.length; idx++) {
                    let sfx = parts[idx];
                    if (sfx[sfx.length-1] === '}') {
                        parts[idx] = sfx.substring(0, sfx.length-2);
                    }
                }

                if (braceIdx !== -1 && value.substring(braceIdx, position).indexOf(END_REFERENCE) !== -1) { // FXIME: in theory close braces can be different
                    // skip this autocomplete (cursor outside braces)
                } else if (braceType === BEGIN_REFERENCE) {
                    // all fields with non-empty '#reference' field (look in TC only. Only before this line)
                    if (parts.length > 1) {
                        // we should autocomplete reference.field.field[].field...
                        let prefix = parts[0];

                        // add all fields from referenced group
                        let dstAction = undefined;
                        for (var i=0; i<limit; i++) {
                            let action = actions[i];
                            if (action.values['#reference'] === prefix) {
                                dstAction = action;
                                break;
                            }
                        }

                        let action = dstAction;

                        let msg = TheDictManager.getMsgDefinition(action.values['#message_type']);

                        let idx = 1;
                        while (idx+1 < parts.length && msg) {
                            let part = parts[idx];
                            prefix += '.' + part;
                            let fieldName = part.substr(0, part.indexOf('[') === -1 ? part.length : part.indexOf('[')); // truncate index
                            if (msg.fields[fieldName] === undefined || msg.fields[fieldName].targetRefType === undefined) {
                                msg = null;
                                break;
                            }
                            if (msg.fields[fieldName].isCollection && fieldName === part) {
                                prefix += '[0]';
                            }

                            msg = TheDictManager.getMsgDefinition(msg.fields[fieldName].targetRefType);

                            idx++;
                        }

                        if (msg) {
                            for (let k in msg.fields) {
                                if (k.startsWith('#')) {
                                    continue;
                                }
                                let refName = BEGIN_REFERENCE + prefix + '.' + k + END_REFERENCE;
                                result[refName] = 1;
                            }
                        }
                        if (parts.length === 2) {
                            // this is simple reference ACTION.XXX
                            // we should suggest all fields of this action:
                            for (let k in action.values) {
                                if (k.startsWith('#')) {
                                    continue;
                                }
                                let refName = BEGIN_REFERENCE + prefix + '.' + k + END_REFERENCE;
                                result[refName] = 1;
                            }
                        }
                    } else {
                        // we should autocomplete reference
                        for (let i=0; i<limit; i++) {
                            let action = actions[i];
                            if (action.values['#reference']) {
                                let refName = BEGIN_REFERENCE + action.values['#reference'] + END_REFERENCE;
                                result[refName] = 1;
                            }
                        }
                    }
                } else if (braceType === BEGIN_STATIC) {
                    // all fields with non-empty '#reference' and '#action' == SetStatic (look in TC only)
                    for (var i=0; i<limit; i++) {
                        let action = actions[i];
                        if (action.values['#reference'] && action.values['#action'] === 'SetStatic') {
                            var refName = BEGIN_STATIC +  action.values['#reference'] + END_STATIC;
                            result[refName] = 1;
                        }
                    }
                } else if (braceType === BEGIN_FUNCTION) {
                    let action = cursor.up().up().get();

                    // There are 2 ways to connect UtilFunctions to action:
                    // * from Action (AML2 and OldImpl for AML3)
                    // * from Dictionary (AML3)
                    // AML2 has more priority...
                    let isAML2 = false;
                    let actionName = action.values['#action'];
                    if (actionName) {
                        let def =  TheDictManager.getActionDefinition(actionName);
                        isAML2 = def.isAML2;

                        if (isAML2) {
                            let utilFunctions = TheDictManager.getAllUtilFunctionsFromClasses(def.utilClasses);
                            _.forOwn(utilFunctions, function(value) {
                                let fnName = BEGIN_FUNCTION + value.name + '(';
                                value.parameters.forEach((param, idx) => {
                                    fnName += param.type;
                                    if (idx != value.parameters.length-1)
                                        fnName += ', ';
                                });
                                fnName += ')' + END_FUNCTION;
                                result[fnName] = new AutoCompleteOption({value: fnName, help: value.help});
                            });
                        }
                    }
                    let dictName = definition && definition.owner && definition.owner.namespace || TheDictManager.getDictionaryNameFromAction(storeId, TheHelper.getPathToLastAction(path));
                    if (dictName && !isAML2) {
                        let utilFunctions = TheDictManager.getAllUtilFunctionsForDict(dictName);
                        _.forEach(utilFunctions, function(value) {
                            let fnName = BEGIN_FUNCTION + value.name + '(';
                            value.parameters.forEach((param, idx) => {
                                fnName += param.type;
                                if (idx != value.parameters.length-1)
                                    fnName += ', ';
                            });
                            fnName += ')' + END_FUNCTION;
                            result[fnName] = new AutoCompleteOption({value: fnName, help: value.help});
                        });
                    }
                } else if (!braceType || braceType == BEGIN_REPEATING) {
                    if (definition && definition.type === 'SUBMESSAGE') {
                        // we should autocomplete reference
                        for (let i=0; i<limit; i++) {
                            let action = actions[i];
                            if (action.values['#reference']) {

                                // if we know MsgType and it different from ours - skip it
                                if (action.values['#message_type']) {
                                    let dictionary = TheDictManager.getDictionaryNameFromAction(storeId, TheHelper.getPathToLastAction(path));
                                    let refDefinition = TheDictManager.getMsgDefinition(action.values['#message_type'], dictionary);
                                    if (refDefinition && (refDefinition.targetRefType || refDefinition.name) && (refDefinition.targetRefType || refDefinition.name) !== definition.targetRefType) {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }

                                if (definition.isCollection) {
                                    let refName = action.values['#reference'];
                                    result[refName] = 1;
                                } else {
                                    let refName = BEGIN_REPEATING + action.values['#reference'] + END_REPEATING;
                                    result[refName] = 1;
                                }

                            }
                        }
                    }
                }

                // for REFERENCE, FUNCTION and STATIC we shouldn't add any other fields
                if (braceType) {
                    startAutocomplete = braceIdx;
                    endAutocomplete = position;
                }
            }

            if (definition && definition.values) {
                Object.assign(result, definition.values);
            }
        }

        // remove existing fields from autocomplete
        if (nodeType === 'actionValues' || nodeType === 'testCaseValues') {
            const record = store.select('data').select(TheHelper.pathAsArr(path)).get();
            if (record) {
                _.forIn((column) => {
                    if (record.hasOwnProperty(column)) {
                        delete result[column];
                    }
                });
            }
        }

        let resultAsArray = [];

        _.forIn(result, (value, key) => {
            if (value instanceof AutoCompleteOption) {
                resultAsArray.push(value); // AutoComplete
            } else {
                resultAsArray.push(key); // String
            }
        });

        if (visibleTemplates) {
            resultAsArray.push(new AutoCompleteOption({value: "${reference}", help: "reference template", isTemplate: true, brace: "${"}));
            resultAsArray.push(new AutoCompleteOption({value: "%{static}", help: "static variable template", isTemplate: true, brace: "%{"}));
            resultAsArray.push(new AutoCompleteOption({value: "#{utility}", help: "utility template", isTemplate: true, brace: "#{"}));
            // if (definition && definition.type === 'SUBMESSAGE' && !definition.isCollection) {
            //     resultAsArray.push(new AutoCompleteOption({value: "[group]", help: "group template", isTemplate: true, brace: "["}));
            // }
        }

        return {
            start: startAutocomplete,
            end: endAutocomplete,
            options: resultAsArray // FIXME: sort it?
        };
        // types for #-fields

        // VALIDATION
        // no 'x>5' in direction=SEND
        //


        // update autocomplete on any change in editor
    }

    getEncodings() {
        return window.EPS.encodings;
    }

    getEnvironments() {
        return window.EPS.environments;
    }

    /**
     * Generates short string: `{idx}. TestCaseName`
     * @param  {[object]} testCase - body of TestCase {items:{}, values:{}}
     * @param  {[number]} idx = 0 - index of test case in matrix
     * @return {[string]}
     */
    testCaseName(testCase, idx = 0) {
        const value = testCase[c.VALUES_FIELD];
        return limitString(`${idx}. ${value['#description'] || value['#reference'] || value['#action']}`);
    }

    actionName(testCases, path) {
        if (typeof path === 'string') {
            path = TheHelper.pathAsArr(path);
        }

        let result = undefined;

        const testCaseIdx = path[0];
        if (testCaseIdx >= testCases.length)
            return result;

        let actions = testCases[testCaseIdx][c.CHILDREN_FIELD];

        if (path.length > 2) {
            const actionIdx = path[2];
            if (actionIdx >= actions.length)
                return result;
            const actionValues = actions[actionIdx][c.VALUES_FIELD];
            result = limitString(`${actionIdx}. ${actionValues['#description'] || actionValues['#action'] || actionValues['#reference']}`);
        }
        return result;
    }

    prettyPrintPath(testCases, path) {
        const asArray = TheHelper.pathAsArr(path);
        const testCaseIdx = +asArray[0];
        let testCaseName = this.testCaseName(testCases[testCaseIdx], testCaseIdx);
        let actionName = this.actionName(testCases, asArray);
        return testCaseName + (actionName ? ' / ' + actionName : '');
    }

}

export const TheRender = new Render();
export default TheRender;
