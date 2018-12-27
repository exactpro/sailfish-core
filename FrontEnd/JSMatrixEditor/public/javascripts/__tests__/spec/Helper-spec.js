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

import * as c from '../../consts.js';
import TheHelper from '../../actions/TheHelper.js';
import TheAppState from 'state/TheAppState.js';
import TheMatrixList from 'state/TheMatrixList.js';

 describe("Helpers check", function() {
     let date = new Date(2015, 9, 15, 14, 15, 20);
     let dateString = date.toString();

    describe("Common", function() {

        it("STRIPTEXT", function() {
            const nullExample = null;
            const textExample = "  hello   ";
            const htmlExample = "<div class='eps-custom-div'>  Content  </div>";

            expect(TheHelper.stripText(nullExample)).toEqual("");
            expect(TheHelper.stripText(textExample)).toEqual(textExample);
            expect(TheHelper.stripText(htmlExample)).toEqual("  Content  ");
        });

        it("GETNEXTREFNAME", function() {

            spyOn(Date, "now").and.callFake(function(){
                return date;
            });

            expect(TheHelper.getNextRefName()).toEqual(`ref_0_${dateString}`);
            expect(TheHelper.getNextRefName()).toEqual(`ref_1_${dateString}`);
            expect(TheHelper.getNextRefName()).toEqual(`ref_2_${dateString}`);
        });

    });

    describe("Data", function() {

        /*
        * findIncludeBlockPath
         */

        const fakeDefinitionCollection = {
            isCollection: true,
        }

        const fakeDefinitionIncludeBlock = {
            includeBlock: true
        }

        const fakeDefinitionSubmsg = {
            type: 'SUBMESSAGE'
        }

        it("getNestedData for testCaseValues", () => {
            expect(TheHelper.getNestedData).toBeDefined();
            let parentPath = "0"+c.PATH_SEP+c.VALUES_FIELD;
            let value = {
                "#execute": "true",
                "#reference": "Test_Case",
                "#description": "TC",
                "#action": "Test Case Start"
            }
            let testCaseValuesExpected = '[{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#action","data":"Test Case Start"},{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#description","data":"TC"},{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#execute","data":"true"},{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#reference","data":"Test_Case"}]';
            let testCaseValuesExpected2 = '[{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#magic","definition":{"isRequired":true}},{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#action","data":"Test Case Start"},{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#description","data":"TC"},{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#execute","data":"true"},{"path":"0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#reference","data":"Test_Case"}]';
            let fakeDefinition = {
                fields: {
                    "#magic": {
                        isRequired: true
                    }
                }
            }
            const testCaseValuesData = TheHelper.getNestedData(parentPath, value, {}, "0");     //without definition
            expect(JSON.stringify(testCaseValuesData)).toEqual(testCaseValuesExpected);
            const testCaseValuesData2 = TheHelper.getNestedData(parentPath, value, fakeDefinition, "0");    //with definition
            expect(JSON.stringify(testCaseValuesData2)).toEqual(testCaseValuesExpected2);
        });

        it("getNestedData for actionField and action", () => {
            expect(TheHelper.getNestedData).toBeDefined();
            let parentPath = "0"+c.PATH_SEP+c.CHILDREN_FIELD + c.PATH_SEP + "1" + c.PATH_SEP + c.VALUES_FIELD + c.PATH_SEP + "#magic";
            let actionPath = "0"+c.PATH_SEP+c.CHILDREN_FIELD + c.PATH_SEP + "0";

            //fake dictionary load
            TheAppState.select("editor", "dict", "TestAML").set({
                loaded: true,
                namespace: ["TestAML"],
                name: "TestAML",
                messages:{
                    "IncludedMessage": {
                        "name": "IncludedMessage",
                        "fields": {
                            "RequiredField": {
                                "name": "RequiredField",
                                "idx": 0,
                                "type": "STRING",
                                "coll": false,
                                "req": true
                            },
                            "NotRequiredField": {
                                "name": "NotRequiredField",
                                "idx": 0,
                                "type": "STRING",
                                "coll": false,
                                "req": false
                            }
                        },
                        "namespace": "TestAML"
                    }
                }
            });
            let subMessage = {
                "values": {
                    "#dictionary": "TestAML",
                    "#id": "26",
                    "#reference": "SubReference"
                },
                "errors": []
            };
            const tcCursor = TheMatrixList["0"].select('data', '0', c.CHILDREN_FIELD);
            const oldCursorState = tcCursor.get();
            tcCursor.unshift(subMessage);

            let fakeDefinition = {
                isCollection: false,
                isRequired: true,
                isTaboo: undefined,
                name: "magic",
                owner: {
                    "dictionary": "TestAML"
                },
                targetRefType: "IncludedMessage",
                type: "SUBMESSAGE"
            };
            let fakeDefinitionString = {
                isCollection: false,
                isRequired: true,
                isTaboo: undefined,
                name: "magic",
                owner: {
                    "dictionary": "TestAML"
                },
                type: "STRING"
            };

            //asserts for actionField
            const actionFieldData = TheHelper.getNestedData(parentPath, '[SubReference]', {}, "0");     //without definition
            expect(actionFieldData).toBeUndefined();
            const actionFieldData2 = TheHelper.getNestedData(parentPath, '[SubReference]', fakeDefinition, "0");    //with submessage definition
            expect(actionFieldData2[0].data.values).toEqual(subMessage.values);
            expect(actionFieldData2[0].definition.fields.RequiredField).toBeDefined;
            expect(actionFieldData2[0].definition.fields.RequiredField.isRequired).toBeTruthy;
            expect(actionFieldData2[0].definition.fields.NotRequiredField).toBeDefined;
            expect(actionFieldData2[0].definition.fields.NotRequiredField.isRequired).toBeFalsy;
            const actionFieldData3 = TheHelper.getNestedData(parentPath, 'qwerty', fakeDefinitionString, "0");    //with string definition
            expect(actionFieldData3).toBeUndefined();

            //asserts for action
            const actionData = TheHelper.getNestedData(actionPath, actionFieldData2[0].data, actionFieldData2[0].definition, "0");
            expect(actionData.length).toEqual(4);                                                       //#dictionary, #id, #reference + required field
            expect(actionData.some(x => x.path === "0" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "0" + c.PATH_SEP + c.VALUES_FIELD + c.PATH_SEP + "RequiredField")).toBeTruthy();     //contains required field
            expect(actionData.some(x => x.path === "0" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "0" + c.PATH_SEP + c.VALUES_FIELD + c.PATH_SEP + "NotRequiredField")).toBeFalsy();     // not contains empty and not required field

            TheAppState.select("editor", "dict", "TestAML").set({
                loaded: false
            });
            tcCursor.set(oldCursorState);
        });

        it("getNestedData for actionValues", () => {
            expect(TheHelper.getNestedData).toBeDefined();
            let parentPath = "0"+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+"0"+c.PATH_SEP+c.VALUES_FIELD;
            let value = {
                "#execute": "y",
                "#dictionary": "TestAML",
                "#service_name": "fake",
                "#action": "receive",
                "#message_type": "Instrument",
                "#reference": "ref1"
            }
            let fakeDefinition = {
                fields: {
                    "#magic": {
                        isRequired: true
                    }
                }
            }
            let expectedData1 = '[{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#action","data":"receive"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#dictionary","data":"TestAML"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD
            +c.PATH_SEP+'#execute","data":"y"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#message_type","data":"Instrument"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP
                +'#reference","data":"ref1"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#service_name","data":"fake"}]';
            let expectedData2 = '[{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#magic","definition":{"isRequired":true}},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#action","data":"receive"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD
                +c.PATH_SEP+'#dictionary","data":"TestAML"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD
                +c.PATH_SEP+'#execute","data":"y"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#message_type","data":"Instrument"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP
                +c.VALUES_FIELD+c.PATH_SEP+'#reference","data":"ref1"},{"path":"0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD+c.PATH_SEP+'#service_name","data":"fake"}]';
            const actionValuesData = TheHelper.getNestedData(parentPath, value, {}, "0");
            expect(JSON.stringify(actionValuesData)).toEqual(expectedData1);
            const actionValuesData2 = TheHelper.getNestedData(parentPath, value, fakeDefinition, "0");
            expect(JSON.stringify(actionValuesData2)).toEqual(expectedData2);
        });

        it("getNestedData for another node type", () => {
            expect(TheHelper.getNestedData).toBeDefined();
            const testCaseItemsData = TheHelper.getNestedData("", "qwerty", {}, "0");
            const testCaseData = TheHelper.getNestedData("0", "qwerty", {}, "0");
            const testCaseFieldData = TheHelper.getNestedData("0" + c.PATH_SEP + c.VALUES_FIELD + c.PATH_SEP + "#id", "qwerty", {}, "0");
            const testCasePlusData = TheHelper.getNestedData("0" + c.PATH_SEP + "qwerty", "qwerty", {}, "0");
            const actionItemsData = TheHelper.getNestedData("0" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "send" + c.PATH_SEP + c.CHILDREN_FIELD, "qwerty", {}, 0);
            const actionPlusData = TheHelper.getNestedData("0" + c.PATH_SEP + "0" + c.PATH_SEP + "send1" + c.PATH_SEP + "#action", "qwerty", {}, 0);

            expect(testCaseItemsData).toBeUndefined();
            expect(testCaseData).toBeUndefined();
            expect(testCaseFieldData).toBeUndefined();
            expect(testCasePlusData).toBeUndefined();
            expect(actionItemsData).toBeUndefined();
            expect(actionPlusData).toBeUndefined();
        });

        it("getActionTemplate", () => {
            expect(TheHelper.getActionTemplate).toBeDefined();
            spyOn(Date, "now").and.callFake(function(){
                return date;
            });
            expect(TheHelper.getActionTemplate()).toEqual({
                values:{
                    "#reference": `ref_3_${dateString}`
                },
                errors: [],
                metadata: {}
            });
            expect(TheHelper.getActionTemplate("send")).toEqual({
                values:{
                    "#action": "send"
                },
                errors: [],
                metadata: {}
            });
            expect(TheHelper.getActionTemplate("send", "fakeReferenceRef")).toEqual({
                values:{
                    "#action": "send"
                },
                errors: [],
                metadata: {}
            });
            expect(TheHelper.getActionTemplate(null, null, "0")).toEqual({
                values:{
                    "#reference": `ref_4_${dateString}`
                },
                errors: [],
                metadata: {}
            });
            expect(TheHelper.getActionTemplate(null, "fakeReferenceRef")).toEqual({
                values:{
                    "#reference": "fakeReferenceRef"
                },
                errors: [],
                metadata: {}
            });
        });

        it("getValuesByPath", () => {
            let path1 = "0";
            let path2 = [0];
            let path3 = '0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0';
            let path4 = [0, c.CHILDREN_FIELD, 0];
            let path5 = [200];
            let path6 = "abrakadabra";
            let path7 = '0'+c.PATH_SEP+c.CHILDREN_FIELD+c.PATH_SEP+'0'+c.PATH_SEP+c.VALUES_FIELD;

            let res1 = TheHelper.getValuesByPath(path1, "0");
            let res2 = TheHelper.getValuesByPath(path2, "0");
            let res3 = TheHelper.getValuesByPath(path3, "0");
            let res4 = TheHelper.getValuesByPath(path4, "0");
            let res5 = TheHelper.getValuesByPath(path5, "0");
            let res6 = TheHelper.getValuesByPath(path6, "0");
            let res7 = TheHelper.getValuesByPath(path7, "0");

            let res12_expected = {
                "#execute": "true",
                "#reference": "Test_Case",
                "#description": "TC",
                "#action": "Test Case Start"
            };
            let res34_expected = {
                "#execute": "y",
                "#dictionary": "TestAML",
                "#service_name": "fake",
                "#action": "receive",
                "#message_type": "Instrument",
                "#reference": "ref1"
            };

            expect(res1.values).toEqual(res12_expected);
            expect(res2.values).toEqual(res12_expected);
            expect(res3.values).toEqual(res34_expected);
            expect(res4.values).toEqual(res34_expected);
            expect(res5).toBeUndefined();
            expect(res6).toBeUndefined();
            expect(res7.values).toBeUndefined();
        });

        it("isRefCollection", function() {

            expect(TheHelper.isRefCollection).toBeDefined();

            const result1 = TheHelper.isRefCollection(null, null, fakeDefinitionCollection);
            expect(result1).toBeTruthy();

            const result2 = TheHelper.isRefCollection(null, null, null);
            expect(result2).toBeFalsy();

            const result3 = TheHelper.isRefCollection(null, null, fakeDefinitionIncludeBlock);
            expect(result3).toBeFalsy();

        });

        it("isBlockInclude", function() {

            expect(TheHelper.isBlockInclude).toBeDefined();

            const result1 = TheHelper.isBlockInclude(null, null, fakeDefinitionIncludeBlock);
            expect(result1).toBeTruthy();

            const result2 = TheHelper.isBlockInclude(null, null, null);
            expect(result2).toBeFalsy();

            const result3 = TheHelper.isBlockInclude(null, null, fakeDefinitionCollection);
            expect(result3).toBeFalsy();

        });

        it("isSubMessage", function() {

            expect(TheHelper.isSubMessage).toBeDefined();

            const result1 = TheHelper.isSubMessage(null, null, fakeDefinitionSubmsg);
            expect(result1).toBeTruthy();

            const result2 = TheHelper.isSubMessage(null, null, null);
            expect(result2).toBeFalsy();

            const result3 = TheHelper.isSubMessage(null, null, fakeDefinitionCollection);
            expect(result3).toBeFalsy();

        });

        it("getFieldReferences", function() {
            expect(TheHelper.getFieldReferences).toBeDefined();

            const value1 = "";
            const value2 = "hello";
            const value3 = "[1]";
            const value4 = "[1,2,3]";

            const result1 = TheHelper.getFieldReferences(null, value1, fakeDefinitionCollection);
            expect(result1).toEqual([]);

            const result2 = TheHelper.getFieldReferences(null, value4, fakeDefinitionCollection);
            expect(result2).toEqual(['1','2','3']);

            const result3 = TheHelper.getFieldReferences(null, value3, fakeDefinitionCollection);
            expect(result3).toEqual(['1']);

            const result4 = TheHelper.getFieldReferences(null, value2, fakeDefinitionSubmsg);
            expect(result4).toEqual(['ll']);

        });

        it("eachActionAbove", () => {
            var i = 0;
            var i1, i2, i3, i4, i5, i6, i7;
            const storeCursor = TheMatrixList["0"].select('data');
            const falsePredicate = function(x) {
                i++;
                return false;
            };
            const truePredicate = function(x) {
                i++;
                return true;        //first element returns true
            }
            const path1 = ["0", c.CHILDREN_FIELD, "0"];
            const path2 = ["0", c.CHILDREN_FIELD, "1"];
            const path3 = ["0", c.CHILDREN_FIELD, "2"];
            const path4 = ["0", c.CHILDREN_FIELD, "3"];

            i = 0;
            TheHelper.eachActionAbove(storeCursor, path1, falsePredicate);
            i1 = i;
            i = 0;
            TheHelper.eachActionAbove(storeCursor, path2, falsePredicate);
            i2 = i;
            i = 0;
            TheHelper.eachActionAbove(storeCursor, path3, falsePredicate);
            i3 = i;
            i = 0;
            TheHelper.eachActionAbove(storeCursor, path4, falsePredicate);
            i4 = i;
            i = 0;
            TheHelper.eachActionAbove(storeCursor, path2, truePredicate);
            i5 = i;
            i = 0;
            TheHelper.eachActionAbove(storeCursor, path3, truePredicate);
            i6 = i;
            i = 0;
            TheHelper.eachActionAbove(storeCursor, path4, truePredicate);
            i7 = i;
            i = 0;

            expect(i1).toEqual(0);
            expect(i2).toEqual(1);
            expect(i3).toEqual(2);
            expect(i4).toEqual(3);
            expect(i5).toEqual(1);
            expect(i6).toEqual(1);
            expect(i7).toEqual(1);
        });

    });

    describe("Path", function() {

        it("getNodeTypeByPath", function() {

            const path00 = "";
            const path10 = "tcName";
            const path20 = "tcName" + c.PATH_SEP + "tcValue";
            const path21 = "tcName" + c.PATH_SEP + c.CHILDREN_FIELD;
            const path22 = "tcName" + c.PATH_SEP + c.VALUES_FIELD;
            const path30 = "tcName" + c.PATH_SEP + "tcValue" + c.PATH_SEP + "actionName";
            const path31 = "tcName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName";
            const path40 = "tcName" + c.PATH_SEP + "tcValue" + c.PATH_SEP + "actionName" + c.PATH_SEP + "actionValues";
            const path41 = "tcName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName" + c.PATH_SEP + c.CHILDREN_FIELD;
            const path42 = "tcName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName" + c.PATH_SEP + c.VALUES_FIELD;
            const path50 = "tcName" + c.PATH_SEP + "tcValue" + c.PATH_SEP + "actionName" + c.PATH_SEP + c.VALUES_FIELD + c.PATH_SEP + "actionField";
            const path51 = "tcName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName"
                + c.PATH_SEP + c.VALUES_FIELD;
            const path52 = "tcName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName"
                + c.PATH_SEP + c.CHILDREN_FIELD+ c.PATH_SEP + "innerAction";
            const path53 = "tcName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName" + c.PATH_SEP + c.CHILDREN_FIELD + c.PATH_SEP + "actionName"
                + c.PATH_SEP + c.VALUES_FIELD+ c.PATH_SEP + "actionField";

            expect(TheHelper.getNodeTypeByPath).toBeDefined();

            const result00 = TheHelper.getNodeTypeByPath(path00);
            const result10 = TheHelper.getNodeTypeByPath(path10);
            const result20 = TheHelper.getNodeTypeByPath(path20);
            const result21 = TheHelper.getNodeTypeByPath(path21);
            const result22 = TheHelper.getNodeTypeByPath(path22);
            const result30 = TheHelper.getNodeTypeByPath(path30);
            const result31 = TheHelper.getNodeTypeByPath(path31);
            const result40 = TheHelper.getNodeTypeByPath(path40);
            const result41 = TheHelper.getNodeTypeByPath(path41);
            const result42 = TheHelper.getNodeTypeByPath(path42);
            const result50 = TheHelper.getNodeTypeByPath(path50);
            const result51 = TheHelper.getNodeTypeByPath(path51);
            const result52 = TheHelper.getNodeTypeByPath(path52);
            const result53 = TheHelper.getNodeTypeByPath(path53);

            expect(result00).toBeUndefined();
            expect(result10).toEqual("testCase");
            expect(result20).toEqual("testCase+");
            expect(result21).toEqual("testCaseItems");
            expect(result22).toEqual("testCaseValues");
            expect(result30).toEqual("testCaseField");
            expect(result31).toEqual("action");
            expect(result40).toEqual("action+");
            expect(result41).toEqual("actionItems");
            expect(result42).toEqual("actionValues");
            expect(result50).toEqual("actionField");
            expect(result51).toEqual("actionValues");
            expect(result52).toEqual("action+");
            expect(result53).toEqual("actionField");

        });

        it("createPath", () => {

            let res_0 = "0";
            let res_empty = "";
            let res_224445 = '23'+c.PATH_SEP+'44'+c.PATH_SEP+'45';

            let path1 = TheHelper.createPath();
            let path2 = TheHelper.createPath(0);
            let path3 = TheHelper.createPath(null);
            let path4 = TheHelper.createPath([23,44,45]);
            let path5 = TheHelper.createPath(23,44,45);
            let path6 = TheHelper.createPath({path:[0]});

            expect(path1).toEqual(res_empty);
            expect(path2).toEqual(res_0);
            expect(path3).toEqual(res_empty);
            expect(path4).toEqual(res_224445);
            expect(path5).toEqual(res_224445);
            expect(path6).not.toEqual(res_0);
        });

        it("pathAsArr", () => {
            expect(TheHelper.pathAsArr('23'+c.PATH_SEP+'44'+c.PATH_SEP+'45')).toEqual(['23','44','45']);
            expect(TheHelper.pathAsArr("25")).toEqual(['25']);
            expect(TheHelper.pathAsArr(['23','44','45'])).toEqual(['23','44','45']);
            expect(TheHelper.pathAsArr()).toEqual([]);
        });

        it("pathLastKey", () => {
            expect(TheHelper.pathLastKey('23'+c.PATH_SEP+'44'+c.PATH_SEP+'45')).toEqual('45');
            expect(TheHelper.pathLastKey(['23','44','45'])).toEqual('45');
            expect(TheHelper.pathLastKey(null)).toBeUndefined();
            expect(TheHelper.pathLastKey("")).toBeUndefined();
            expect(TheHelper.pathLastKey([])).toBeUndefined();
        });

        it("pathFirstKey", () => {
            expect(TheHelper.pathFirstKey('23'+c.PATH_SEP+'44'+c.PATH_SEP+'45')).toEqual('23');
            expect(TheHelper.pathFirstKey(['23','44','45'])).toEqual('23');
            expect(TheHelper.pathFirstKey(null)).toBeUndefined();
            expect(TheHelper.pathFirstKey("")).toBeUndefined();
            expect(TheHelper.pathFirstKey([])).toBeUndefined();
        });

        it("pathIsSingle", () => {
            expect(TheHelper.pathIsSingle('23')).toBeTruthy();
            expect(TheHelper.pathIsSingle(['23'])).toBeTruthy();
            expect(TheHelper.pathIsSingle('23'+c.PATH_SEP+'44'+c.PATH_SEP+'45')).toBeFalsy();
            expect(TheHelper.pathIsSingle(['23','44','45'])).toBeFalsy();
            expect(TheHelper.pathIsSingle(null)).toBeFalsy();
            expect(TheHelper.pathIsSingle("")).toBeFalsy();
            expect(TheHelper.pathIsSingle([])).toBeFalsy();
        });

        it("expandShortPath", () => {
            expect(TheHelper.expandShortPath(['23'])).toEqual('23');
            expect(TheHelper.expandShortPath(['23', '44'])).toEqual('23' + c.PATH_SEP + c.CHILDREN_FIELD +c.PATH_SEP + '44');
            expect(() => {TheHelper.expandShortPath('23')}).toThrow();
            expect(() => {TheHelper.expandShortPath('23'+ c.PATH_SEP + '44')}).toThrow();
            expect(TheHelper.expandShortPath(['23', '44', '45'])).toEqual('23' + c.PATH_SEP + c.CHILDREN_FIELD +c.PATH_SEP + '44' + c.PATH_SEP + c.CHILDREN_FIELD +c.PATH_SEP + '45');
        });

        it("toShortPath", () => {
            expect(TheHelper.toShortPath()).toEqual([]);
            expect(TheHelper.toShortPath("23")).toEqual(['23']);
            expect(TheHelper.toShortPath("23" + c.PATH_SEP + "44")).toEqual(['23']);
            expect(TheHelper.toShortPath("23" + c.PATH_SEP + "items" + c.PATH_SEP + "45")).toEqual(['23', '45']);
            expect(TheHelper.toShortPath("23" + c.PATH_SEP + "items" + c.PATH_SEP + "45" + c.PATH_SEP + "values")).toEqual(['23', '45']);
            expect(TheHelper.toShortPath("23" + c.PATH_SEP + "items" + c.PATH_SEP + "45" + c.PATH_SEP + "items" + c.PATH_SEP + "57")).toEqual(['23', '45', '57']);
            expect(TheHelper.toShortPath(["23"])).toEqual(['23']);
            expect(TheHelper.toShortPath(["23", "44"])).toEqual(['23']);
            expect(TheHelper.toShortPath(["23", "items", "45"])).toEqual(['23', '45']);
            expect(TheHelper.toShortPath(["23", "items", "45", "values"])).toEqual(['23', '45']);
            expect(TheHelper.toShortPath(["23", "items", "45", "items", "57"])).toEqual(['23', '45', '57']);
        });

        it("parentPath", () => {
            expect(TheHelper.parentPath("23")).toEqual('');
            expect(TheHelper.parentPath("23" + c.PATH_SEP + "44")).toEqual('23');
            expect(TheHelper.parentPath("23" + c.PATH_SEP + "44" + c.PATH_SEP + "45")).toEqual('23' + c.PATH_SEP + '44');
            expect(TheHelper.parentPath("23" + c.PATH_SEP + "44" + c.PATH_SEP + "45" + c.PATH_SEP + "48")).toEqual('23' + c.PATH_SEP + '44' + c.PATH_SEP + '45');
            expect(TheHelper.parentPath(["23"])).toEqual([]);
            expect(TheHelper.parentPath(["23", "44"])).toEqual(['23']);
            expect(TheHelper.parentPath(["23", "44", "45"])).toEqual(['23', '44']);
            expect(TheHelper.parentPath(["23", "44", "45", "48"])).toEqual(['23', '44', '45']);
        });

        it("getPathToAction", () => {
            expect(TheHelper.getPathToAction("23")).toEqual(["23"]);
            expect(TheHelper.getPathToAction("23" + c.PATH_SEP + "44")).toEqual(["23", "44"]);
            expect(TheHelper.getPathToAction("23" + c.PATH_SEP + "44" + c.PATH_SEP + "45")).toEqual(["23", "44", "45"]);
            expect(TheHelper.getPathToAction("23" + c.PATH_SEP + "44" + c.PATH_SEP + "45" + c.PATH_SEP + "48")).toEqual(["23", "44", "45"]);
            expect(TheHelper.getPathToAction(["23"])).toEqual(["23"]);
            expect(TheHelper.getPathToAction(["23", "44"])).toEqual(["23", "44"]);
            expect(TheHelper.getPathToAction(["23", "44", "45"])).toEqual(["23", "44", "45"]);
            expect(TheHelper.getPathToAction(["23", "44", "45", "48"])).toEqual(['23', '44', '45']);
            expect(TheHelper.getPathToAction()).toEqual([]);
        });
    });

    describe("Node", function() {

        /*
        * removeEnvironmentFromService
        */

        const NODE_EXPANDABLE = 1;
        const NODE_COLLAPSED = 2;
        const NODE_EXPANDED = 4;
        const NODE_LEAF = 8;

       it("node expand state", () => {
           expect(() => {TheHelper.getNodeExpandState()}).not.toThrow();
           expect(TheHelper.isNodeLeaf(TheHelper.getNodeExpandState(true, true))).toBeTruthy();
           expect(TheHelper.isNodeLeaf(TheHelper.getNodeExpandState(false, true))).toBeTruthy();
           expect(TheHelper.isNodeLeaf(TheHelper.getNodeExpandState(true, false))).toBeFalsy();
           expect(TheHelper.isNodeLeaf(TheHelper.getNodeExpandState(false, false))).toBeFalsy();

           expect(TheHelper.isNodeExpandable(TheHelper.getNodeExpandState(true, false))).toBeTruthy();
           expect(TheHelper.isNodeExpandable(TheHelper.getNodeExpandState(false, false))).toBeTruthy();
           expect(TheHelper.isNodeExpandable(TheHelper.getNodeExpandState(true, true))).toBeFalsy();
           expect(TheHelper.isNodeExpandable(TheHelper.getNodeExpandState(false, true))).toBeFalsy();

           expect(TheHelper.isNodeExpanded(TheHelper.getNodeExpandState(true, false))).toBeTruthy();
           expect(TheHelper.isNodeExpanded(TheHelper.getNodeExpandState(false, false))).toBeFalsy();
           expect(TheHelper.isNodeExpanded(TheHelper.getNodeExpandState(true, true))).toBeFalsy();
           expect(TheHelper.isNodeExpanded(TheHelper.getNodeExpandState(false, true))).toBeFalsy();

           expect(TheHelper.isNodeCollapsed(TheHelper.getNodeExpandState(true, false))).toBeFalsy();
           expect(TheHelper.isNodeCollapsed(TheHelper.getNodeExpandState(false, false))).toBeTruthy();
           expect(TheHelper.isNodeCollapsed(TheHelper.getNodeExpandState(true, true))).toBeFalsy();
           expect(TheHelper.isNodeCollapsed(TheHelper.getNodeExpandState(false, true))).toBeFalsy();
       });

       it("set and unset MouseOverNode", (done) => {
           const nodeCursor = TheAppState.select('node', 'left', 'overPath');
           expect(nodeCursor.get()).toEqual(null);

           TheHelper.setMouseOverNode("0", "left");
           setTimeout(() => {
               expect(nodeCursor.get()).toEqual("0");
               TheHelper.setMouseOverNode("1", "left");
               setTimeout(() => {
                   expect(nodeCursor.get()).toEqual("1");
                   TheHelper.unsetMouseOverNode("left");
                   expect(nodeCursor.get()).toEqual(null);
                   done();
               }, 60);
           }, 60);
       });

    });

 });
