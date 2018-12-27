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

import Base from 'actions/Base.js';

var emptyFn = Function.prototype;

class Server extends Base {

    // History API
    undo(onFailure, onSuccess) {
        setTimeout(() => onSuccess(''), 1);
    }

    redo(onFailure, onSuccess) {
        setTimeout(() => onSuccess(''), 1);
    }

    listOfMatrixes(callback) {
        setTimeout(() => callback(null, { matrices : [] }), 1);
    }

    load(matrixId, callback) {
        // you can edit this line. it will be overrided by FakeContext for Karma tests
        // setTimeout(() => callback(null, require('json!./aml3_tests.json')), 1);
        setTimeout(() => callback(new Error('no history'), null), 1);
    }

    reset(matrixId, callback) {
        setTimeout(() => callback(null), 1);
    }

    save() {
        setTimeout(() => arguments[arguments.length-1].call(this), 1);
    }

    create(path, value, onFailure, onSuccess) {
        onSuccess({
            values: value.values,
            errors: [],
            items: value.items
        });
    }

    read() {
        setTimeout(() => arguments[arguments.length-1].call(this), 1);
    }

    update(path, action, onFailure, onSuccess) {
        var shortPath = [path[0], path[2]];
        if (action['#action'] === 'xxx') {
            setTimeout(() => onSuccess({errors: [{
                        "paths": [shortPath],
                        "reference": "",
                        "column": null,
                        "message": "Ahtung XXX!"
                    },
                    {
                        "paths": [shortPath],
                        "reference": "",
                        "column": null,
                        "message": "VERY LOOOOOOOOOOOOOOOOOOOOOOOOOOOOOONG ERROR TEXT"
                    },
                    {
                        "paths": [shortPath],
                        "reference": "",
                        "column": null,
                        "message": "Second error"
                    }
                ]}),
                500);
        } else {
            setTimeout(
                () => {
                    onSuccess({errors: []})
                },
                1
            );
        }
    }

    remove() {
        setTimeout(() => arguments[arguments.length-1].call(this), 1);
    }

    splice() {
        setTimeout(() => arguments[arguments.length-1].call(this), 1);
    }

    loadDictionariesList(onFailure = emptyFn, onSuccess = emptyFn) {
        setTimeout(() => onSuccess(require('json!./dictionaries.json')), 1);
    }

    loadDict(dictName, onFailure, onSuccess) {
        var dictStub = {
            "name": dictName,
            "namespace": [ dictName ],
            "description": null,
            "messages": {},
            "utils": []
        };
        var result;
        if (dictName === 'TestAML') {
            result = require('json!./TestAML.json');
            result.utils = {};
            result.utils['FormatUtil'] = require('json!./format_utils.json');
        } else if (dictName === 'Qwerty') {
            result = null;
        } else {
            result = dictStub;
        }

        if (result) {
            setTimeout(() => onSuccess(result), 1);
        } else {
            setTimeout(() => onFailure(new Error("dictionary not found")), 1);
        }

    }

    loadActionsDefinitions(onFailure, onSuccess) {
        setTimeout(() => onSuccess(require('json!./actions.json')), 1);
    }

    loadServices(environmentName, onError = emptyFn, onSuccess = emptyFn) {
        setTimeout(() => onSuccess(
            [{
                'name': 'fake',
                'dict': 'TestAML',
                'type': 'FakeType',
            }]), 1);
    }

    loadLanguage(onError = emptyFn, onSuccess = emptyFn) {
        onSuccess({
            columns: [{
            name: '#action',
            help: "help #action"
        }, {
            name: '#message_type',
            help: "help #message_type"
        }, {
            name: '#dictionary',
            help: "help #dictionary"
        }, {
            name: '#service_name',
            help: "help #service_name"
        }, {
            name: '#execute',
            help: "help #execute"
        }, {
            name: '#id',
            help: "help #id"
        }, {
            name: '#messages_count',
            help: "help #messages_count"
        }, {
            name: '#outcome',
            help: "help #outcome"
        }, {
            name: '#reference',
            help: "help #reference"
        }, {
            name: '#static_type',
            help: "help #static_type"
        }, {
            name: '#static_value',
            help: "help #static_value"
        }, {
            name: '#description',
            help: "help #description"
        }, {
            name: '#timeout',
            help: "help #timeout"
        }, {
            name: '#check_point',
            help: "help #check_point"
        }
        ]});
    }
}

export default new Server();
