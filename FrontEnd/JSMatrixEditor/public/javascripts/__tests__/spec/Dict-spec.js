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

 window.callBack = function(param) {console.log("Hello from callback with param " + param)};
 import FakeWindow from "../mock/FakeWindow.js";
 import * as c from '../../consts.js';
 import dicts from 'actions/TheDictManager.js';
 import TheHelper from 'actions/TheHelper.js';
 import TheAppState, {getMainStoreId} from 'state/TheAppState.js';
 import TheStoreList from 'state/TheMatrixList.js';

describe("Check dictionary actions", function() {

    /*
    * FIXME: getAllUtilFunctionsForDict
    */

    window.getContextPath = window.getContextPath || function() {return "sfgui";}

    const _ = require('lodash');
    const emptyFn =  Function.prototype;

    it("loadDictionariesList", function(done) {
        expect(dicts.loadDictionariesList).toBeDefined();
        expect(dicts.getAllDictionaries).toBeDefined();
        expect(dicts.isDictionariesLoaded).toBeDefined();

        expect(dicts.isDictionariesLoaded()).toEqual(false);

        dicts.loadDictionariesList(
            () => {
            expect(dicts.isDictionariesLoaded()).toEqual(false); // loadDictionariesList doesn't affects isDictionariesLoaded
            expect(Object.keys(dicts.getAllDictionaries()).length).toBeGreaterThan(0);
            done();
        });
    });

    it("preloadDictionariesFromMatrix, getAllActions", function(done) {
        expect(dicts.preloadDictionariesFromMatrix).toBeDefined();
        expect(dicts.getAllActions).toBeDefined();
        expect(dicts.isDictionariesLoaded()).toEqual(false);
        dicts.preloadDictionariesFromMatrix(getMainStoreId(), () => {
            expect(dicts.isDictionariesLoaded()).toEqual(true);
            setTimeout(() => {
                expect(TheAppState.get('editor', 'actions', 'caseSensitive')).toBeDefined();
                expect(TheAppState.get('editor', 'actions', 'caseInsensitive')).toBeDefined();
                expect(dicts.getAllActions().length).toBeGreaterThan(0);
                done();
            }, 20);
        });
    })

    it("load(null, cb)", function() {
        expect(dicts.load).toBeDefined();
        spyOn(window, 'callBack');
        dicts.load(null, false, window.callBack);
        expect(window.callBack).toHaveBeenCalled();
    });

    it("load(dict, cb)", function(done) {
        const dictionary = "DICT_r86";
        const dictCursor = TheAppState.select('editor', 'dict').select(dictionary);
        expect(dictCursor.get('name')).toBeUndefined(); //not loaded
        expect(dicts.load).toBeDefined();
        dicts.load(dictionary, false, (err) => {
            expect(err).toEqual(null);
            expect(dictCursor.get('name')).toEqual(dictionary);
            done();
        });
    });

    it("findDefinition", function() {
        TheStoreList[getMainStoreId()].select('data').set([
            {
                values: {
                    "#action": "Test Case Start",
                },
                items:[{
                    values:{
                        '#action': "send",
                        '#message_type': 'Statistics',
                        '#dictionary': 'TestAML',
                    },
                    errors:[]
                }, {
                    values:{
                        '#action': 'Include block',
                        '#reference': 'ref1'
                    },
                    errors:[]
                }],
                errors: []
            }
        ]);

        const cursor = TheStoreList[getMainStoreId()].select('data');

        const result1 = dicts.findDefinition(cursor.select([0, 'values']).get(), getMainStoreId(), [0, 'values']);
        const result2 = dicts.findDefinition(cursor.select([0, 'items', 0, 'values']).get(), getMainStoreId(), [0, 'items', 0, 'values']);
        const result3 = dicts.findDefinition(cursor.select([0, 'items', 1]).get(), getMainStoreId(), [0, 'items', 1]);

        expect(result1).toBeDefined();
        expect(JSON.stringify(result1.fields)).toEqual('{"#id":{},"#execute":{},"#description":{},"#action":{},"#reference":{},"#fail_on_unexpected_message":{}}');

        expect(result2).toBeDefined();
        expect(result2.fields).toBeDefined();
        // expect(result2.fields['#service_name']).toBeDefined();
        expect(result2.fields['Turnover']).toBeDefined();
        expect(result2.fields['VWAP']).toBeDefined();
        expect(result2.fields['Volume']).toBeDefined();

        expect(result3).toBeDefined();
        expect(JSON.stringify(result3.fields)).toEqual('{"#action":{},"#reference":{},"#template":{"includeBlock":true,"name":"#template","type":"SUBBLOCK","targetRefType":"#reference","isRequired":true}}');
    });

    it('getDictNames', () => {
        TheStoreList[getMainStoreId()].select('data').set([
            {
                values: {
                    "#action": "Test Case Start",
                },
                items:[{
                    values:{
                        "#fdg": "gdh",
                        "#dictionary": "TestAML",
                    },
                    errors:[]
                }],
                errors: []
            }, {
                values: {
                    "#action": "Test Case Start",
                },
                items:[{
                    values:{
                        "#fdg": "gdh",
                        "#dictionary": "TestAML",
                    },
                    errors:[]
                }, {
                    values:{
                        "#fdg": "gdh",
                        "#dictionary": "TestAML23",
                    },
                    errors:[]
                }],
                errors: []
            }
        ]);
        const dictNames = dicts.getDictNames(getMainStoreId());
        expect(dictNames).toContain("TestAML23");
        expect(dictNames).toContain("TestAML");
    });

    //TODO
    it('getMessages', () => {
        let dictName = "TestAML";
        let fakeDictName = "Qwerty";
        let trueMessages = dicts.getMessages(dictName);
        let noMessages = dicts.getMessages(fakeDictName);
        expect(trueMessages).not.toEqual({});
        expect(noMessages).toEqual({});
    });

    it("loadServicesInfo calls callback", function(done) {
        expect(dicts.loadServicesInfo).toBeDefined();

        dicts.loadServicesInfo("default", (err, list) => {
            expect(err).toBeNull();
            expect(dicts.getServiceDescription("fake")).toEqual({name: 'fake', dict: 'TestAML', type: 'FakeType'});
            expect(dicts.getServiceDescription("veryFake")).toBeUndefined();
            expect(Object.keys(dicts.getAllServices()).length).toBeGreaterThan(0);
            done();
        });
    });

    it("getActionDefinition", () => {
        let expected = {
            fields: {
                "#service_name": {}
            },
            isAML2: false,
            isAML3: true
        }
        expect(JSON.stringify(dicts.getActionDefinition("send"))).toEqual(JSON.stringify(expected));
    });

    it("loadLanguage", () => {
        dicts.loadLanguage((err,res) => {
            expect(err).toBeNull();
            expect(res).toBeDefined();
            expect(res.columns.length).toBeGreaterThan(0);
        });
    });

    it("getActionDefinition", () => {
        let def1 = dicts.getActionDefinition("send");
        let def2 = dicts.getActionDefinition("");
        let def3 = dicts.getActionDefinition("very_fake_action");

        expect(JSON.stringify(def1.fields)).toEqual('{"#service_name":{}}');
        expect(def1.isAML3).toBeTruthy();
        expect(JSON.stringify(def2.fields)).toEqual('{}');
        expect(def2.isAML3).toBeFalsy();
        expect(JSON.stringify(def3.fields)).toEqual('{}');
        expect(def3.isAML3).toBeFalsy();
    });

    it('getMsgDefinition', () => {
        const testDictName = "TestAML";
        let ref1 = dicts.getMsgDefinition("OrderMassCancelReport", testDictName);   //loaded dictionary, good message_name
        let ref2 = dicts.getMsgDefinition("VeryFakeMessage", testDictName);         //loaded dictionary, bad message_name
        let ref3 = dicts.getMsgDefinition("OrderMassCancelReport");                 //dictionary not specified, good message_name
        let ref4 = dicts.getMsgDefinition("VeryFakeMessage");                       //dictionary not specified, bad message_name
        let ref5 = dicts.getMsgDefinition("OrderMassCancelReport", "FakeDict");     // not loaded dictionary, good message_name
        let ref6 = dicts.getMsgDefinition("VeryFakeMessage", "FakeDict");           //not loaded dictionary, bad message_name

        const emptyRes = {
            name: undefined,
            namespace: undefined,
            fields: undefined,
            isAML2: false,
            isAML3: true,
            dictionary: undefined
        };

        const emptyJSON = JSON.stringify(emptyRes);

        expect(JSON.stringify(ref2)).toEqual(emptyJSON);
        expect(JSON.stringify(ref4)).toEqual(emptyJSON);
        expect(JSON.stringify(ref5)).toEqual(emptyJSON);
        expect(JSON.stringify(ref6)).toEqual(emptyJSON);
        expect(ref1).toEqual(ref3);
        expect(ref1.fields).toBeDefined();
        expect(ref1.name).toEqual("OrderMassCancelReport");
        expect(ref1.namespace).toEqual(testDictName);
        expect(ref1.isAML3).toBeTruthy();
    });

});
