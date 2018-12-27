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
import async from 'async';
import * as _ from 'lodash';

import BaseAction from './Base.js';
import TheHelper from 'actions/TheHelper.js';
import TheServer from 'actions/TheServer.js';
import TheAppState, {getMainStoreId} from 'state/TheAppState.js';
import TheStoreList from 'state/TheMatrixList.js';
import {EpsAppError} from 'utils.js';
const DICT_PATH = ['editor', 'dict'];
const ACTIONS_PATH = ['editor', 'actions'];
const UTILS_PATH = ['editor', 'utils']; // for actions
const SERVICES_PATH = ['editor', 'services'];
const LANGUAGE_PATH = ['editor', 'language'];
const SYSTEM_ACTION_PREFIX = '#';
const emptyFn = Function.prototype;

import {MessageDefinition, FieldDefinition} from './__models__.js';

const requiredField = new FieldDefinition({
    type: 'STRING',
    req: true
});

const requiredBooleanField = new FieldDefinition({
    type: 'BOOLEAN',
    values: ['y', 'n'],
    req: true
});

const optionalField = new FieldDefinition({
    type: 'STRING'
});

const optionalLongField = new FieldDefinition({
    type: 'LONG'
});

const tabooField = new FieldDefinition({
    type: 'STRING',
    taboo: true
});

class DictManager extends BaseAction {
    constructor(...args) {
        super(...args);
        this._actionsLoaded = false;
        this._dictionariesLoaded = false;
    }

    isActionsLoaded() {
        return this._actionsLoaded;
    }

    isDictionariesLoaded() {
        return this._dictionariesLoaded;
    }

    loadDictionariesList(callback = emptyFn) {
        TheServer.loadDictionariesList(
            (err) => {
                TheHelper.error('Failed to load dictionries list', err.message + '. ' + err.rootCause);
                callback(err, undefined);
            },
            (list) => {
                list.dictionaries.forEach((dictName) => {
                    const oldValue = TheAppState.select(DICT_PATH).select(dictName).get();
                    TheAppState.select(DICT_PATH).select(dictName).set(oldValue || { loaded: false });
                });
                callback(null, list);
            }
        );
    }

    preloadDictionariesFromMatrix(storeId = getMainStoreId(), callback = emptyFn) {
        const store = TheStoreList[storeId];
        // create task pool
        const dictNames = this.getDictNames(storeId);
        const fns = _.uniq(dictNames).map( x => this.load.bind(this, x, false) );
        // run tasks. than check AML version
        async.parallelLimit(fns, 3, err => {
            if (err) {
                return TheHelper.error('Failed to load dictionary', err.message + '. ' + err.rootCause);
            }
            this._dictionariesLoaded = true;
            this.__checkAML2();
            callback();
        });
        this.loadActionsDefinitionsOnce( () => this.__checkAML2() );
    }

    loadServicesInfo(environmentName, callback = emptyFn) {
        TheServer.loadServices(
            environmentName,
            (err) => {
                TheHelper.error('Failed to load services list', err.message + '. ' + err.rootCause);
                callback(err, undefined);
            },
            (list) => {
                TheAppState.select(SERVICES_PATH).set(list);
                callback(null, list);
            }
        );
    }

    getServiceDescription(serviceName) {
        const list = TheAppState.select(SERVICES_PATH).get();
        return list.find((item) => {
            return item.name === serviceName;
        });
    }

    resolveServiceName(storeId, pathAsArr) {
        const action = TheStoreList[storeId].select('data').select(TheHelper.getPathToLastAction(pathAsArr)).get();
        const serviceName = action.values['#service_name'];

        if (!serviceName) {
            return undefined;
        }

        if (!serviceName.startsWith('%{')) {
            return serviceName;
        }

        // resolve it:
        var mapping = this.getAllServiceAliases(storeId, pathAsArr);
        return mapping[serviceName];
    }

    loadLanguage(callback = emptyFn) {
        TheServer.loadLanguage(
            (err) => {
                TheHelper.error('Failed to load language info', err.message + '. ' + err.rootCause);
                callback(err, undefined);
            },
            (list) => {
                TheAppState.select(LANGUAGE_PATH).set(list);
                callback(null, list);
            }
        );
    }

    getLanguage() {
        return {
            columns: TheAppState.select(LANGUAGE_PATH).get('columns').map((column) => {
                return column.name;
            })
        };
    }

    getHelpString(name) {
        let res = null;
        if (name && name.length && name[0] === SYSTEM_ACTION_PREFIX) {
            res = TheAppState.select(LANGUAGE_PATH).get('columns')
                .filter((column) => {
                    return column.name === name;
                })
                .map((column) => {
                    return column.help;
                })[0] || null;
        }
        return res;
    }

    /**
     * @callback loadCallback
     * @param {*} error
     * @param {*} result
     */

    /**
     *
     * @param dictName
     * @param {loadCallback} [callback]
     **/
    load(dictName, reload = false, callback = emptyFn) {
        if (!dictName) {
            return callback(null); // 'No dictionary specified'
        }

        const cursor = TheAppState.select(DICT_PATH).select(dictName);
        var record = cursor.get();
        if (!record) {
            record = {};
            cursor.set(record);
        }

        if (!reload && (record.loaded || record.pending)) {
            return callback(null);
        }
        record.pending = true;
        record.loaded = false;
        cursor.set(record);

        TheServer.loadDict(dictName,
            (err) => {
                record.pending = false;
                cursor.set(record);

                TheHelper.error(`Failed to load dictionary ${dictName}`, err.message + '. ' + err.rootCause);
                callback(null);
            },
            (data) => {
                record = data;
                record.pending = false;
                record.loaded = true;
                cursor.set(record);

                callback(null);
            }
        );
    }

    __getDictNamesFromCache() {
        const dictCache = TheAppState.select(DICT_PATH).get();
        return Object.keys(dictCache);
    }

    /**
     *
     * @param {string} msgType definition type -- message type or special started from $
     * @param {...string} [dictNames]
     * @returns {MessageDefinition}
     */
    getMsgDefinition(msgType, ...dictNames) {
        if (dictNames.length === 0) {
            dictNames = this.__getDictNamesFromCache();
        }
        let messageDefn;
        let dictionaries = TheAppState.select(DICT_PATH).get();
        let targetDictionary;
        dictNames.some((name) => {
            if (dictionaries[name] && dictionaries[name].loaded) {
                targetDictionary = name;
                messageDefn = dictionaries[name].messages[msgType];
                return (messageDefn !== undefined);
            }
            return false;
        });

        let result;
        if (messageDefn) {
            result = new MessageDefinition({
                name: messageDefn.name,
                namespace: messageDefn.namespace,
                dict: targetDictionary
            });
            result.fields = messageDefn.fields && Object.keys(messageDefn.fields).reduce((acc, x) => {
                acc[x] = new FieldDefinition(messageDefn.fields[x]);
                acc[x].owner = result;
                return acc;
            }, {});
        } else {
            result = new MessageDefinition();
        }

        return result;
    }

    /**
     * @param  {any}    value   only applied for actions
     * @param  {[type]} storeId only applied for actions
     * @param  {[type]} path
     * @return {[type]}
     */
    findDefinition(value, storeId, path) {
        let defn = {
            fields: {

            }
        };
        const nodeType = TheHelper.getNodeTypeByPath(path);
        if (nodeType === 'testCaseValues') {
            defn = {
                fields: {
                    '#id': Object.create(requiredField),
                    '#execute': Object.create(requiredBooleanField),
                    '#description': Object.create(requiredField),
                    '#action': Object.create(requiredField),
                    '#reference' : Object.create(requiredField),
                    '#fail_on_unexpected_message' : Object.create(requiredBooleanField)
                }
            };
        } else if (nodeType === 'actionValues' || nodeType === 'action' || nodeType === 'action+') {
            let action;
            let actionValues;
            if (nodeType === 'action' || nodeType === 'action+') {
                actionValues = value.values; // assign to 'value' nested field 'values'
                action = value;
            } else /*(nodeType === 'actionValues')*/ {
                actionValues = value;
                action = {
                    values: actionValues
                };
            }
            if (actionValues['#message_type']) {
                const msgType = actionValues['#message_type'];
                const dictionary = this.getDictionaryNameFromAction(storeId, TheHelper.getPathToLastAction(path));
                Object.assign(defn, this.getMsgDefinition(msgType, dictionary));
                if (!defn.fields) {
                    defn.fields = {};
                }
                defn.fields['#message_type'] = Object.create(optionalField);
            }
            if (actionValues['#action']) {
                defn.fields = Object.assign(defn.fields || {}, this.getActionDefinition(actionValues['#action']).fields);
                if (!defn.fields) {
                    defn.fields = {};
                }
                defn.fields['#action'] = Object.create(optionalField);
            }
            if (!defn.fields) {
                defn.fields = {};
            }
            if (actionValues['#reference']) {
                defn.fields['#reference'] = Object.create(optionalField);
            }
            if (actionValues['#timeout']) {
                defn.fields['#timeout'] = Object.create(optionalLongField);
            }
            if (actionValues['#action'] === 'Include block') {
                defn.fields['#template'] = new FieldDefinition({
                    include: true,
                    name: '#template',
                    ref: '#reference',
                    req: true,
                    type: 'SUBBLOCK'
                });
                defn.fields['#reference'] = Object.create(requiredField);
            }
        }
        return defn && (new MessageDefinition(defn));
    }

    getBaseFieldDefinition(fieldName) {
        return {
            "#message_type": Object.create(optionalField),
            "#action": Object.create(optionalField),
            "#reference": Object.create(optionalField),
            "#timeout": Object.create(optionalLongField),
            '#id': Object.create(optionalField)
        }[fieldName];
    }

    findNestedDefinition(ownerRowDefinition) {
        const defn = (ownerRowDefinition && ownerRowDefinition.type === 'SUBMESSAGE') ?
            this.getMsgDefinition(ownerRowDefinition.targetRefType, ownerRowDefinition.owner.dictionary)
            : undefined;
        return defn;
    }

    /**
     *
     * @param callback
     */
    loadActionsDefinitionsOnce(callback = emptyFn) {
        TheServer.loadActionsDefinitions(
            () => { callback('Failed to load actions'); },
            (data) => {
                TheAppState.select(ACTIONS_PATH).set({
                    caseSensitive: data.actions || {},
                    caseInsensitive: data.statements || {}
                });
                TheAppState.select(UTILS_PATH).set(data.utils || {});
                this._actionsLoaded = true;
                callback(null, data);
            }
        );
        this.loadActionsDefinitionsOnce = emptyFn;
    }

    reloadActionsDefinitions(callback = emptyFn) {
        TheServer.loadActionsDefinitions(
            () => { callback('Failed to load actions'); },
            (data) => {
                TheAppState.select(ACTIONS_PATH).set({
                    caseSensitive: data.actions || {},
                    caseInsensitive: data.statements || {}
                });
                TheAppState.select(UTILS_PATH).set(data.utils || {});
                this._actionsLoaded = true;
                callback(null, data);
            }
        );
    }

    getActionDefinition(actionName) {
        let record = TheAppState.select(ACTIONS_PATH).select('caseSensitive', actionName).get();
        if (record == undefined) {
            record = TheAppState.select(ACTIONS_PATH).select('caseInsensitive', actionName.toUpperCase()).get();
        }
        return this.__getMsgLikeDefinition(record);
    }

    __getMsgLikeDefinition(actionDefinition) {
        let fields = {};
        let isAML2 = false, isAML3 = false, utilClasses = [];

        if (actionDefinition instanceof Object) {
            if (actionDefinition.direction === 'RECEIVE') {
                // It is desirable to specify timeout for such fields... but not required
                fields['#timeout'] = Object.assign({}, fields['#timeout'], requiredField);
            }
            actionDefinition.required.forEach(name => fields[name] = Object.create(requiredField));
            actionDefinition.optional.forEach(name => fields[name] = Object.create(optionalField));
            isAML2 = actionDefinition.isAML2;
            isAML3 = actionDefinition.isAML3;
            utilClasses = actionDefinition.utilClasses;
        }

        return new MessageDefinition({
            fields: fields,
            isAML2: isAML2,
            isAML3: isAML3,
            utilClasses: utilClasses
        });
    }

    getDictNames(storeId = getMainStoreId()) {
        let result = [];
        const testCases = TheStoreList[storeId].get('data');
        testCases.forEach((testCase, tcIdx) => {
            result = result.concat(this.__getDictNamesFromItems(testCase[c.CHILDREN_FIELD], [tcIdx, c.CHILDREN_FIELD], storeId));
        });
        return _.uniq(result);
    }

    __getDictNamesFromItems(items, parentPath, storeId = getMainStoreId()) {
        let result = [];
        items.forEach((action, aIdx)=> {
            var resolveServiceName = false; // don't resolve service name (it is much faster)
            result.push(this.getDictionaryNameFromAction(storeId, parentPath.concat([aIdx]), resolveServiceName));
            if (action.items && Array.isArray(action.items)) {
                result = result.concat(this.__getDictNamesFromItems(action.items, parentPath.concat([aIdx, c.CHILDREN_FIELD]), storeId));
            }
        });
        return result;
    }

    getAllDictionaries() {
        return TheAppState.select(DICT_PATH).get();
    }

    getAllServices() {
        const services = TheAppState.select(SERVICES_PATH).get();
        let result = {};

        if (services != null) {
            services.forEach(item => {
                result[item.name] = 1;
            });
        }
        return result;
    }

    /**
     * @param  {string} storeId
     * @param  {Array} pathAsArr
     * @return {object} mapping from alias to service name
     */
    getAllServiceAliases(storeId, pathAsArr) {
        var mapping = {};
        TheHelper.allActionAbove(storeId, pathAsArr, (action) => {
            if (action.values['#action'] && action.values['#reference'] && action.values['#service_name'] && action.values['#action'].toLowerCase() == 'DefineServiceName'.toLowerCase()) {
                mapping[action.values['#reference']] = action.values['#service_name'];
            }
            return false; // itterate
        });
        return mapping;
    }

    /**
     * @return {Array} which contains  objects with name and help for action
     */
    getAllActions() {
        const actions = TheAppState.select(ACTIONS_PATH).get();
        const result = [];
        let a;

        for (a in actions.caseSensitive) {
            result.push({name: actions.caseSensitive[a].name, help: actions.caseSensitive[a].help});
        }
        for (a in actions.caseInsensitive) {
            result.push({name: actions.caseInsensitive[a].name});
        }

        return result;
    }

    getAllUtilFunctionsForDict(dictName) {
        const utils = TheAppState.select(DICT_PATH).select(dictName, 'utils').get();
        if (utils == null) {
            return;
        }

        let result = []
        _.forOwn(utils, function(value, key) {
            // join actions on dictionaries
            let arr = (value == null) ? TheAppState.select(UTILS_PATH).get(key) : value;
            if (arr)
                result.push(...arr);
            return true;
        });

        return result;
    }

    getAllUtilFunctionsFromClasses(utilClasses) {
        const utils = TheAppState.select(UTILS_PATH).get();
        var result = [];
        utilClasses.forEach((ucls) => result.push(...utils[ucls]));
        return result;
    }

    getMessages(dictName) {
        if (dictName == null) {
            throw new EpsAppError('Illegal argument exception');
        }

        this.load(dictName, false); // lazy load
        this.loadActionsDefinitionsOnce(); // lazy load

        const messages = TheAppState.select('editor', 'dict', dictName, 'messages').get();

        return messages || {};
    }

    getDictionaryNameFromAction(storeId, pathAsArr, isResolveServiceName = true) {
        const baobab = TheStoreList[storeId];
        const action = baobab.select('data').select(pathAsArr).get();

        let result = action.values['#dictionary'];
        if (result !== undefined) {
            return result;
        }
        const serviceName = isResolveServiceName ? this.resolveServiceName(storeId, pathAsArr) : action.values['#service_name'];
        if (serviceName) {
            const serviceDescription = this.getServiceDescription(serviceName);
            if (serviceDescription) {
                return serviceDescription.dict;
            }
        }
        // last chance:
        const def = TheDictManager.getMsgDefinition(action.values['#message_type']);
        if (def !== undefined) {
            return def.namespace;
        }

        return undefined; // not found
    }

    __checkAML2(storeId = getMainStoreId()) {
        if (!this.isDictionariesLoaded() || !this.isActionsLoaded()) {
            return;
        }

        let actions_count = 0;
        let known_actions_count = 0;
        let actions = [];
        const baoabab = TheStoreList[storeId];
        if (baoabab) {
            let blocks = baoabab.get('data');
            let all_actions = this.getAllActions();
            for (var i=0; i<blocks.length; i++) {
                var block = blocks[i];
                for (var j=0; j<block.items.length; j++) {
                    var action = block.items[j];
                    var action_field = action.values['#action'];

                    // action_field can be empty (leg)...
                    if (!action_field) {
                        continue;
                    }
                    action_field = action_field.trim();
                    actions.push(action_field);

                    var isAML3Action = all_actions.indexOf(action_field) !== -1;
                    actions_count ++;
                    if (isAML3Action) {
                        known_actions_count++;
                    }
                }
            }
            console.log('AML version detection: ' + known_actions_count + '/' + actions_count);
            if (actions_count === 0) {
                return;
            }
            if (known_actions_count / actions_count < 0.5) {
                TheHelper.error('Incorrect AML version', 'This editor supports only AML3 matrixes');
            }
        }
    }
}

export const TheDictManager = new DictManager();
export default TheDictManager;

// load services and dicts concurently
TheDictManager.loadServicesInfo('default');
TheDictManager.loadDictionariesList();
TheDictManager.loadLanguage();
