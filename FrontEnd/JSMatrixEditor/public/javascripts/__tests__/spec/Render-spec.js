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

 import TheRender from 'actions/TheRender.js';
 const render = TheRender;

 import * as c from "consts.js";

 const Baobab = require('baobab');

 describe("Check render actions", function() {

     /*
     * getEncodings
     * getEnvironments
     */

    it("getRenderedType", function() {

        const def1 = {
            type: "BOOLEAN",
            targetRefType: "ref"
        };

        const def2 = {
            type: "SUBMESSAGE",
            targetRefType: "r",
            isCollection: true
        };

        expect(render.getRenderedType).toBeDefined();

        const result0 = render.getRenderedType(null, null, null);
        const result1 = render.getRenderedType(null, null, def1);
        const result2 = render.getRenderedType(null, null, def2);

        expect(result0).toEqual("");
        expect(result1).toEqual("bool");
        expect(result2).toEqual("[msg:r]");

    });

    it("castValue", () => {
        let def1 = {};
        let def2 = {isCollection: true};
        let def3 = {isCollection: false};
        let def4 = {type: "SUBBLOCK"};
        let def5 = {type: "SUBMESSAGE"};
        let def6 = {type: "STRING"};
        let def7 = {type: "DATE"};
        let def8 = {type: "DATETIMEONLY"};
        let def9 = {type: "DATEWITHOUTTIME"};
        let def10 = {type: "BOOLEAN"};
        let def11 = {type: "SHORT"};
        let def12 = {type: "INT"};
        let def13 = {type: "LONG"};
        let def14 = {type: "BYTE"};
        let def15 = {type: "FLOAT"};
        let def16 = {type: "DOUBLE"};
        let def17 = {type: "CHAR"};
        let def18 = {type: "DECIMAL"};
        let def19 = {type: "NONEXISTENT_TYPE"};
        let def20 = {type: "DECIMAL", isCollection: true};

        expect(TheRender.castValue()).toBeUndefined();

        expect(TheRender.castValue("222")).toEqual("222");
        expect(TheRender.castValue(222)).toEqual(222);

        expect(TheRender.castValue("222", def1)).toEqual("222");
        expect(TheRender.castValue(222, def1)).toEqual(222);
        expect(TheRender.castValue(null, def1)).toBeNull();
        expect(TheRender.castValue("222", def2)).toEqual("222");
        expect(TheRender.castValue(222, def2)).toEqual(222);
        expect(TheRender.castValue(null, def2)).toBeNull();
        expect(TheRender.castValue("222", def3)).toEqual("222");
        expect(TheRender.castValue(222, def3)).toEqual(222);
        expect(TheRender.castValue(null, def3)).toBeNull();
        expect(TheRender.castValue("222", def4)).toEqual("222");
        expect(TheRender.castValue(222, def4)).toEqual(222);
        expect(TheRender.castValue(null, def4)).toBeNull();
        expect(TheRender.castValue("222", def5)).toEqual("222");
        expect(TheRender.castValue(222, def5)).toEqual(222);
        expect(TheRender.castValue(null, def5)).toBeNull();
        expect(TheRender.castValue("222", def6)).toEqual("222");
        expect(TheRender.castValue(222, def6)).toEqual(222);
        expect(TheRender.castValue(null, def6)).toBeNull();
        expect(TheRender.castValue("222", def7)).toEqual("222");
        expect(TheRender.castValue(222, def7)).toEqual(222);
        expect(TheRender.castValue(null, def7)).toBeNull();
        expect(TheRender.castValue("222", def8)).toEqual("222");
        expect(TheRender.castValue(222, def8)).toEqual(222);
        expect(TheRender.castValue(null, def8)).toBeNull();
        expect(TheRender.castValue("222", def9)).toEqual("222");
        expect(TheRender.castValue(222, def9)).toEqual(222);
        expect(TheRender.castValue(null, def9)).toBeNull();

        expect(TheRender.castValue("222", def10)).toEqual("(boolean) (222)");
        expect(TheRender.castValue(222, def10)).toEqual(222);
        expect(TheRender.castValue(null, def10)).toBeNull();
        expect(TheRender.castValue("222", def11)).toEqual("(short) (222)");
        expect(TheRender.castValue(222, def11)).toEqual(222);
        expect(TheRender.castValue(null, def11)).toEqual(null);
        expect(TheRender.castValue("null", def11)).toEqual("(short) (null)");
        expect(TheRender.castValue("222", def12)).toEqual("(int) (222)");
        expect(TheRender.castValue(222, def12)).toEqual(222);
        expect(TheRender.castValue(null, def12)).toEqual(null);
        expect(TheRender.castValue("null", def12)).toEqual("(int) (null)");
        expect(TheRender.castValue("222", def13)).toEqual("(long) (222)");
        expect(TheRender.castValue(222, def13)).toEqual(222);
        expect(TheRender.castValue(null, def13)).toEqual(null);
        expect(TheRender.castValue("null", def13)).toEqual("(long) (null)");
        expect(TheRender.castValue("222", def14)).toEqual("(byte) (222)");
        expect(TheRender.castValue(222, def14)).toEqual(222);
        expect(TheRender.castValue(null, def14)).toEqual(null);
        expect(TheRender.castValue("null", def14)).toEqual("(byte) (null)");
        expect(TheRender.castValue("222", def15)).toEqual("(float) (222)");
        expect(TheRender.castValue(222, def15)).toEqual(222);
        expect(TheRender.castValue(null, def15)).toEqual(null);
        expect(TheRender.castValue("null", def15)).toEqual("(float) (null)");
        expect(TheRender.castValue("222", def16)).toEqual("(double) (222)");
        expect(TheRender.castValue(222, def16)).toEqual(222);
        expect(TheRender.castValue(null, def16)).toEqual(null);
        expect(TheRender.castValue("null", def16)).toEqual("(double) (null)");
        expect(TheRender.castValue("222", def17)).toEqual("(char) (222)");
        expect(TheRender.castValue(222, def17)).toEqual(222);
        expect(TheRender.castValue(null, def17)).toEqual(null);
        expect(TheRender.castValue("null", def17)).toEqual("(char) (null)");
        expect(TheRender.castValue("222", def18)).toEqual("toBigDecimal(222)");
        expect(TheRender.castValue(222, def18)).toEqual(222);
        expect(TheRender.castValue(null, def18)).toEqual(null);
        expect(TheRender.castValue("null", def18)).toEqual("toBigDecimal(null)");
        expect(() => {TheRender.castValue("222", def19)}).toThrow();
        expect(TheRender.castValue("222", def20)).toEqual("222");
        expect(TheRender.castValue(222, def20)).toEqual(222);
        expect(TheRender.castValue(null, def20)).toEqual(null);
        expect(TheRender.castValue("null", def20)).toEqual("null");
    });

    it("testCaseName", () => {
        let tc1 = {};
        let tc2 = {values:{}};
        let tc3 = {values: {"#description": "qwerty"}};
        let tc4 = {values: {"#reference": "qwerty"}};
        let tc5 = {values: {"#action": "qwerty"}};
        let tc6 = {
            values: {
                "#description": "qwerty",
                "#reference": "qwerty",
                "#action": "qwerty"
            }
        };
        let tc7 = {values: {"#description": "qwerty12345678901234567890123456789012345678901234567890"}};
        let tc8 = {values: {"#reference": "qwerty12345678901234567890123456789012345678901234567890"}};
        let tc9 = {values: {"#action": "qwerty12345678901234567890123456789012345678901234567890"}};

        expect(() => {TheRender.testCaseName()}).toThrow();
        expect(() => {TheRender.testCaseName(tc1)}).toThrow();

        expect(TheRender.testCaseName(tc2)).toEqual('0. undefined');
        expect(TheRender.testCaseName(tc2, 2)).toEqual('2. undefined');
        expect(TheRender.testCaseName(tc3)).toEqual('0. qwerty');
        expect(TheRender.testCaseName(tc3, 2)).toEqual('2. qwerty');
        expect(TheRender.testCaseName(tc4)).toEqual('0. qwerty');
        expect(TheRender.testCaseName(tc4, 2)).toEqual('2. qwerty');
        expect(TheRender.testCaseName(tc5)).toEqual('0. qwerty');
        expect(TheRender.testCaseName(tc5, 2)).toEqual('2. qwerty');
        expect(TheRender.testCaseName(tc6)).toEqual('0. qwerty');
        expect(TheRender.testCaseName(tc6, 2)).toEqual('2. qwerty');
        expect(TheRender.testCaseName(tc7)).toEqual('0. qwerty12345678901234567890123456789012345678901...');
        expect(TheRender.testCaseName(tc7, 2)).toEqual('2. qwerty12345678901234567890123456789012345678901...');
        expect(TheRender.testCaseName(tc8)).toEqual('0. qwerty12345678901234567890123456789012345678901...');
        expect(TheRender.testCaseName(tc8, 2)).toEqual('2. qwerty12345678901234567890123456789012345678901...');
        expect(TheRender.testCaseName(tc9)).toEqual('0. qwerty12345678901234567890123456789012345678901...');
        expect(TheRender.testCaseName(tc9, 2)).toEqual('2. qwerty12345678901234567890123456789012345678901...');
    });

    it('actionName', () => {
        let a1 = {};
        let a2 = {values:{}};
        let a3 = {values: {"#description": "qwerty"}};
        let a4 = {values: {"#reference": "qwerty"}};
        let a5 = {values: {"#action": "qwerty"}};
        let a6 = {
            values: {
                "#description": "qwerty",
                "#reference": "qwerty",
                "#action": "qwerty"
            }
        };
        let a7 = {values: {"#description": "qwerty12345678901234567890123456789012345678901234567890"}};
        let a8 = {values: {"#reference": "qwerty12345678901234567890123456789012345678901234567890"}};
        let a9 = {values: {"#action": "qwerty12345678901234567890123456789012345678901234567890"}};

        let tc1 = [{items:[a1], values:{}}];
        let tc2 = [{items:[a2], values:{}}];
        let tc3 = [{items:[a3], values:{}}];
        let tc4 = [{items:[a4], values:{}}];
        let tc5 = [{items:[a5], values:{}}];
        let tc6 = [{items:[a6], values:{}}];
        let tc7 = [{items:[a7], values:{}}];
        let tc8 = [{items:[a8], values:{}}];
        let tc9 = [{items:[a9], values:{}}];

        expect(() => {TheRender.testCaseName()}).toThrow();

        expect(TheRender.testCaseName(tc1[0])).toEqual('0. undefined');
        expect(TheRender.actionName(tc2, ["0", "items", "0"])).toEqual('0. undefined');
        expect(TheRender.actionName(tc3, ["0", "items", "0"])).toEqual('0. qwerty');
        expect(TheRender.actionName(tc4, ["0", "items", "0"])).toEqual('0. qwerty');
        expect(TheRender.actionName(tc5, ["0", "items", "0"])).toEqual('0. qwerty');
        expect(TheRender.actionName(tc6, ["0", "items", "0"])).toEqual('0. qwerty');
        expect(TheRender.actionName(tc7, ["0", "items", "0"])).toEqual('0. qwerty12345678901234567890123456789012345678901...');
        expect(TheRender.actionName(tc8, ["0", "items", "0"])).toEqual('0. qwerty12345678901234567890123456789012345678901...');
        expect(TheRender.actionName(tc9, ["0", "items", "0"])).toEqual('0. qwerty12345678901234567890123456789012345678901...');
    });

    it('prettyPrintPath', () => {
        const testCases = [{
            values: {"#action": "qwerty"},
            items: [{
                values: {
                        "#description": "rewq"
                }
            }]
        }];
        let path1 = "0";
        let path2 = "0";
        let path3 = "0" + c.PATH_SEP + "items" + c.PATH_SEP + "0";
        let path4 = ["0", "items", "0"];

        expect(TheRender.prettyPrintPath(testCases, path1)).toEqual("0. qwerty");
        expect(TheRender.prettyPrintPath(testCases, path2)).toEqual("0. qwerty");
        expect(TheRender.prettyPrintPath(testCases, path3)).toEqual("0. qwerty / 0. rewq");
        expect(TheRender.prettyPrintPath(testCases, path4)).toEqual("0. qwerty / 0. rewq");
        expect(() => {TheRender.prettyPrintPath(null, path4)}).toThrow();
        expect(() => {TheRender.prettyPrintPath({}, path4)}).toThrow();
    });
 });
