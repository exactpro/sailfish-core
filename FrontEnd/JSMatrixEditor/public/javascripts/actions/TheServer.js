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

import _ from 'lodash';
import $ from 'jquery';

import * as c from 'consts.js';
import Base from './Base.js';

import {getMainStoreId} from 'state/TheAppState.js';

const REST_EDIT_PREFIX = 'sfapi/editor/';
const REST_DICT_PREFIX = 'sfapi/dictionary/';
const REST_ACTIONS_PREFIX = 'sfapi/actions/';
const REST_SERVICES_PREFIX = 'sfapi/services/';

import {TheHelper} from 'actions/TheHelper.js';

const emptyFn = Function.prototype;

$.ajaxSetup({ cache: false });

function errorUnwrapper (fn) {
    return function(jqXHR, textStatus, errorThrown) {
        //console.debug(arguments);
        var error = jqXHR.responseJSON;
        if (!error) {
            error = {
                message: textStatus,
                rootCause: errorThrown
            };
        }
        fn.call(null, error);
    };
}

function toNodeLikeCallback(fn) {
    return function(data) {
        fn.call(null, null, data);
    };
}

class Server extends Base {
    constructor(...args) {
        super(...args);
        this.removeMetaData = this.removeMetaData.bind(this);
    }

    /**
     *
     * @param {function} callback(error, response)
     */
    listOfMatrixes(callback) {
        $.ajax(REST_EDIT_PREFIX, {
            method: 'GET',
            success: toNodeLikeCallback(callback),
            error: errorUnwrapper(callback)
        });
    }

    /**
     * @param {number|string} matrixId
     * @param {function} callback(error, response)
     */
    load(matrixId, callback) {
        $.ajax(REST_EDIT_PREFIX + matrixId, {
            method: 'POST',
            data: JSON.stringify({
                environment: 'default',
                encoding: 'UTF-8'
            }),
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: toNodeLikeCallback(callback),
            error: errorUnwrapper(callback)
        });
    }

    reset(matrixId, callback = emptyFn) {
        $.ajax(REST_EDIT_PREFIX + matrixId + '/reset', {
            method: 'POST',
            success: toNodeLikeCallback(callback),
            error: errorUnwrapper(callback)
        });
    }

    undo(onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_EDIT_PREFIX + getMainStoreId() + '/undo', {
            method: 'POST',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    redo(onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_EDIT_PREFIX + getMainStoreId() + '/redo', {
            method: 'POST',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    loadDictionariesList(onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_DICT_PREFIX, {
          success: onSuccess,
          error: errorUnwrapper(onError)
        });
    }

    loadDict(dictName, onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_DICT_PREFIX + encodeURIComponent(dictName) + '?deep=y&utils=y', {
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    loadServices(environmentName, onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_SERVICES_PREFIX + '?environment=' + encodeURIComponent(environmentName), {
            success: (response) => {
                var result = [];
                // don't use map() - it will return JQuery object
                $(response).find('service').each((idx, item) => {
                    var $item = $(item);
                    result.push({
                        'name': $item.find('serviceName').text(),
                        'dict': $item.find('settings>dictionaryName').text(),
                        'type': $item.find('type').text()
                    });
                });
                onSuccess(result);
            },
            error: errorUnwrapper(onError),
            dataType: 'xml'
        });
    }

    loadLanguage(onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_EDIT_PREFIX + 'language/aml/3/columns', {
            success: (response) => {
                onSuccess({
                    columns: response
                });
            },
            error: errorUnwrapper(onError)
        });
    }

    /**
     * @param {string} path
     * @param {objext} value
            for actions: { values:{} }
            for testCases: {values:{} , items:[] } ('items' field is optional)
     * @param {function} [onError]
     * @param {function} [onSuccess]
     */
    create(path, action, onError = emptyFn, onSuccess = emptyFn) {
        const url = this.pathToUrl(path);

        $.ajax(url, {
            method: 'POST',
            data: JSON.stringify(this.removeMetaData(action)),
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    /**
     * @param {string} path
     * @param {function} [onError]
     * @param {function} [onSuccess]
     */
    read(path, onError = emptyFn, onSuccess = emptyFn) {
        // =)
    }

    /**
     * @param {string} path
     * @param {objext} value
            for actions: { values:{} }
            for testCases: {values:{} , items:[] } ('items' field is optional)
     * @param {function} [onError]
     * @param {function} [onSuccess]
     */
    update(path, action, onError = emptyFn, onSuccess = emptyFn) {
        const url = this.pathToUrl(path);

        $.ajax(url, {
            method: 'PUT',
            data: JSON.stringify(action),
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    /**
     * @param {string} path
     * @param {function} [onError]
     * @param {function} [onSuccess]
     */
    remove(path, onError = emptyFn, onSuccess = emptyFn) {
        const url = this.pathToUrl(path);

        $.ajax(url, {
            method: 'DELETE',
            success: function(data) {
                onSuccess(data);
            },
            error: errorUnwrapper(onError)
        });
    }


    // server will do following steps:
    // 1) remove deleteCount blocks/actions (starting from start)
    // 2) insert new blocks/actions (starting from start)
    splice(spec, onError = emptyFn, onSuccess = emptyFn) {
        const specCopy = _.cloneDeep(spec); // FIXME: speedup!
        Object.keys(specCopy).forEach(level => {
            specCopy[level].forEach(record => {
                const processedData = record.data.map(d => this.removeMetaData(d));
                record.data = processedData;
            });
        });

        $.ajax(REST_EDIT_PREFIX + getMainStoreId() + '/update', {
            method: 'POST',
            data: JSON.stringify(specCopy),
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    save(onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_EDIT_PREFIX + getMainStoreId() + '/save', {
            method: 'POST',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    pathToUrl(path) {
        var result = [];
        for (var i=0; i<path.length; i++) {
            var element = path[i];
            if (!isNaN(+element)) {
                result.push(encodeURIComponent(element));
            }
        }
        let str = (result.length <= 2) ? result.join('/') : result[0] + '/' + result.slice(1).join("_");
        return REST_EDIT_PREFIX + getMainStoreId() + '/' + str;
    }


    loadActionsDefinitions(onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_ACTIONS_PREFIX + '3' + '?' + 'utils=y', { // AML3 actions
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    execute(params, onError = emptyFn, onSuccess = emptyFn) {
        // REST API:
        $.ajax(REST_EDIT_PREFIX + getMainStoreId() + '/run', {
            method: 'POST',
            data: JSON.stringify(params),
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    getExecutionResult(unexpectedOnly, onError = emptyFn, onSuccess = emptyFn) {
        var url = REST_EDIT_PREFIX + getMainStoreId() + '/run';
        url += '?toAML=true';
        url += '&unexpectedOnly=' + (unexpectedOnly ? 'true': 'false');
        $.ajax(url, {
            method: 'GET',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    releaseExecutionResult(onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_EDIT_PREFIX + getMainStoreId() + '/run/', {
            method: 'DELETE',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    configure(config, onError = emptyFn, onSuccess = emptyFn) {
        $.ajax(REST_EDIT_PREFIX + getMainStoreId() + '/configure', {
            method: 'POST',
            data: JSON.stringify(config),
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    configuration(onError = emptyFn, onSuccess = emptyFn) {
        var url = REST_EDIT_PREFIX + getMainStoreId() + '/configuration';
        $.ajax(url, {
            method: 'GET',
            success: onSuccess,
            error: errorUnwrapper(onError)
        });
    }

    removeMetaData(node) {
        let res = {
            [c.VALUES_FIELD]: node[c.VALUES_FIELD]
        }
        let actionName = (res.values) ? res.values[c.ACTION_FIELD]: null;
        if (node[c.CHILDREN_FIELD] != null || (actionName && (TheHelper.isBlockAction(actionName) || TheHelper.isIfContainer(node[c.VALUES_FIELD])))) {
            res[c.CHILDREN_FIELD] = node[c.CHILDREN_FIELD].map(i => this.removeMetaData(i)) || [];
        }
        return res;
    }
}

export const TheServer = new Server();
export default TheServer;
