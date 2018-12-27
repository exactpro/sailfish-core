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
// support async/await in runtime:
import "regenerator-runtime/runtime";

import FakeWindow from "../mock/FakeWindow.js";;
import TheMatrixList from 'state/TheMatrixList.js';
const $ = require('jquery');
import TheServer from '../../actions/TheServer.js';
const server = TheServer;

import * as c from 'consts.js';

const successHandler = function() {console.log("Completed with success")};
const errorHandler = function() {console.log("Completed with error")};

describe("Server actions test", function() {

    it("listOfMatrixes correct url", function() {
        spyOn($, "ajax");
        server.listOfMatrixes();
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/');
    });

    it("loadActionsDefinitions correct url", function() {
        spyOn($, "ajax");
        server.loadActionsDefinitions();
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/actions/3?utils=y');
    });

    it("LOAD correct url", function() {
        spyOn($, "ajax");
        server.load("0");
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0');
    });

    it("LOAD correct data", function() {
        spyOn($, "ajax");
        server.load();
        const data = $.ajax.calls.mostRecent().args[1]["data"];
        expect(data).toEqual('{"environment":"default","encoding":"UTF-8"}');
    });

    it("RESET correct url", function() {
        spyOn($, "ajax");
        server.reset(0);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/reset');
    });

    it("UNDO correct url", function() {
        spyOn($, "ajax");
        server.undo(errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/undo');
    });

    it("REDO correct url", function() {
        spyOn($, "ajax");
        server.redo(errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/redo');
    });

    it("LOAD DICT correct url", function() {
        const dictName = "custom";
        spyOn($, "ajax");
        server.loadDict(dictName, errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/dictionary/custom?deep=y&utils=y');
    });

    it("LOAD DICT correct url", function() {
        spyOn($, "ajax");
        server.loadDictionariesList(errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/dictionary/');
    });

    it("LOAD SERVICES correct url", function() {
        spyOn($, "ajax");
        server.loadServices("default", errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/services/?environment=default');
    });

    it("LOAD LANGUAGE correct url", function() {
        spyOn($, "ajax");
        server.loadLanguage(errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/language/aml/3/columns');
    });

    it("CREATE correct url", function() {
        const path = [4,5,6];
        const action = "action";
        spyOn($, "ajax");
        server.create(path, action, errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/4/5_6');
    });

    it("UPDATE correct url", function() {
        const path = [4,5,6];
        const action = "action";
        spyOn($, "ajax");
        server.update(path, action, errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/4/5_6');
    });

    it("REMOVE correct url", function() {
        const path = [4,5,6];
        spyOn($, "ajax");
        server.remove(path, errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/4/5_6');
    });

    it("SPLICE correct url", function() {
        const spec = {};
        spyOn($, "ajax");

        server.splice(spec, errorHandler, successHandler);

        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/update');
    });

    it("SPLICE correct data", function() {
        const spec = {
            1: [{data: ["qwer", "wert"]}, {data: ["yuiop", "tyuio"]}],
            2: [{data: ["asdfg", "sdfgh"]}, {data: ["hjkl", "ghjk"]}],
            3: [{data: ["zxcvb", "xcvbn"]}, {data: ["nm", "vbnh"]}]
        };
        spyOn($, "ajax");

        server.splice(spec, errorHandler, successHandler);
        const data = $.ajax.calls.mostRecent().args[1]["data"];

        expect(data).toEqual('{"1":[{"data":[{},{}]},{"data":[{},{}]}],' +
                             '"2":[{"data":[{},{}]},{"data":[{},{}]}],' +
                             '"3":[{"data":[{},{}]},{"data":[{},{}]}]}');
    });

    it("SAVE correct url", function() {
        spyOn($, "ajax");
        server.save(errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/save');
    });

    it("EXECUTE correct url", function() {
        spyOn($, "ajax");
        const params = {
            continueOnFailed: true,
            autoStart: true
        };
        server.execute(params, errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/run');
    });

    it("EXECUTE correct data", function() {
        spyOn($, "ajax");
        const params = {
            continueOnFailed: true,
            autoStart: true
        };
        server.execute(params, errorHandler, successHandler);
        const data = $.ajax.calls.mostRecent().args[1]["data"];
        expect(data).toEqual('{"continueOnFailed":true,"autoStart":true}');
    });

    it("GETEXECUTIONRESULT correct url unexpected true", function() {
        spyOn($, "ajax");
        const unexpectedOnly = true;
        server.getExecutionResult(unexpectedOnly, errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/run?toAML=true&unexpectedOnly=true');
    });

    it("GETEXECUTIONRESULT correct url unexpected false", function() {
        spyOn($, "ajax");
        const unexpectedOnly = false;
        server.getExecutionResult(unexpectedOnly, errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/run?toAML=true&unexpectedOnly=false');
    });

    it("RELEASEEXECUTIONRESULT correct url", function() {
        spyOn($, "ajax");
        server.releaseExecutionResult(errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/run/');
    });

    it("CONFIGURE correct url", function() {
        spyOn($, "ajax");
        const config = {};
        server.configure(config, errorHandler, successHandler);
        expect($.ajax.calls.mostRecent().args[0]).toEqual('sfapi/editor/0/configure');
    });

    it("CONFIGURE correct data", function() {
        spyOn($, "ajax");
        const config = {field: "value", key: "value2"};
        server.configure(config, errorHandler, successHandler);
        const data = $.ajax.calls.mostRecent().args[1]["data"];
        expect(data).toEqual('{"field":"value","key":"value2"}');
    });

    it('pathToUrl', () => {
        let p1 = undefined;     // path must be an array
        let p2 = null;
        let p3 = [];
        let p4 = ["0"];
        let p5 = ["0", "values"];
        let p6 = ["0", "items"];
        let p7 = ["0", "items", "0"];
        let p8 = ["0", "items", "0", "values"];
        let p9 = ["0", "items", "0", "values", "#id"];

        expect(server.pathToUrl(p3)).toEqual('sfapi/editor/0/');
        expect(server.pathToUrl(p4)).toEqual('sfapi/editor/0/0');
        expect(server.pathToUrl(p5)).toEqual('sfapi/editor/0/0');
        expect(server.pathToUrl(p6)).toEqual('sfapi/editor/0/0');
        expect(server.pathToUrl(p7)).toEqual('sfapi/editor/0/0/0');
        expect(server.pathToUrl(p8)).toEqual('sfapi/editor/0/0/0');
        expect(server.pathToUrl(p9)).toEqual('sfapi/editor/0/0/0');
    });

    it('removeMetaData', () => {
        let node1 = {
            values:{
                "#id": "150"
            },
            errors:[],
            items: [],
            metadata: {
                dictionary: "ASDF"
            }
        };
        let node2 = {
            values:{
                "#id": "150"
            },
            items: [],
            errors: []
        };
        let node3 = {
            values:{
                "#id": "150"
            },
            items: [],
        };
        let node4 = {
            values:{
                "#id": "150"
            },
            items: [{
                values:{
                    "#id": "150"
                },
                errors:[],
                items: []
            }],
            errors: []
        };
        let node5 = {
            values:{
                "#id": "150"
            },
            items: [{
                values:{
                    "#id": "150"
                },
                errors:[],
                items: [],
                metadata: {
                    dictionary: "ASDF"
                }
            }],
            errors: []
        };
        let node6 = {
            values:{
                "#id": "150"
            },
            items: [{
                values:{
                    "#id": "150"
                },
                items: []
            }],
        };
        expect(server.removeMetaData(node1)).toEqual(node3);
        expect(server.removeMetaData(node2)).toEqual(node3);
        expect(server.removeMetaData(node3)).toEqual(node3);
        expect(server.removeMetaData(node4)).toEqual(node6);
        expect(server.removeMetaData(node5)).toEqual(node6);
        expect(server.removeMetaData(node6)).toEqual(node6);
    });

});
