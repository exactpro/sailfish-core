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

import FakeWindow from "../../mock/FakeWindow.js";
import TheMatrixList from 'state/TheMatrixList.js';
import * as c from 'consts.js';
import * as _ from 'lodash';
import {baseContextTypes, contextFactory} from 'contextFactory.js';

import React from 'react';
import ReactDOM from 'react-dom';
import ReactTestUtils from 'react-dom/test-utils';
import InsertPopover from 'view/controls/InsertPopover.js';

import TheHistory from 'actions/TheHistory.js';

/*
* handleAddNode
*/

describe('InsertPopover', function () {

    const path1 = "0" + c.PATH_SEP + "items" +c.PATH_SEP + "0";

     var element = null;
     const component = <InsertPopover path={path1}/>;
     const Container = React.createClass({
        childContextTypes: baseContextTypes,

        getChildContext: function() {
          let ctx = contextFactory("0", "left");
          ctx.currentStore = TheMatrixList["0"];
          ctx.readonly = false;
          return ctx;
        },

        render: function() {
          return this.props.component;
        }
    });

    it('renders', () => {
        element = ReactTestUtils.renderIntoDocument(<Container component={component}/>);
        expect(element).toBeTruthy();
    });

    it('add and remove bookmark', (done) => {
        let link = ReactTestUtils.scryRenderedDOMComponentsWithClass(element, "exa-insert-popover-item exa-btn-bookmark")[1];
        expect(link.getAttribute("class")).not.toContain("btn-warning");
        ReactTestUtils.Simulate.click(link);
        expect(TheAppState.get("panels", "dataSources", "items", "0", "bookmarks").length).toBe(1);
        setTimeout(() => {
            let selectedLink = ReactTestUtils.scryRenderedDOMComponentsWithClass(element, "exa-insert-popover-item exa-btn-bookmark")[1];
            expect(selectedLink.getAttribute("class")).toContain("btn-warning");
            ReactTestUtils.Simulate.click(selectedLink);
            expect(TheAppState.get("panels", "dataSources", "items", "0", "bookmarks").length).toBe(0);
            done();
        }, 10);
    });

    it('select copy source and clear it', (done) => {
        expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').sourcePath).toBeUndefined();
        expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').destinationPath).toBeUndefined();
        let link = ReactTestUtils.scryRenderedDOMComponentsWithClass(element, "exa-insert-popover-item exa-btn-bookmark")[0];
        expect(link.children[0].getAttribute("class")).toContain("glyphicon-screenshot");
        ReactTestUtils.Simulate.click(link);
        expect(link.children[0].getAttribute("class")).toContain("glyphicon-screenshot");
        setTimeout(() => {
            let selectedLink = ReactTestUtils.scryRenderedDOMComponentsWithClass(element, "exa-insert-popover-item exa-btn-bookmark")[0];
            expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').sourcePath).toBe(path1);
            ReactTestUtils.Simulate.click(selectedLink);
            setTimeout(() => {
                expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').sourcePath).toBeUndefined();
                done();
            }, 10);
        }, 10);
    });

    it('copy source', (done) => {       //check of correct copy in TheEditor-spec.js
        const dst_path = "0" + c.PATH_SEP + "items" +c.PATH_SEP + "1";
        const dst_component = <InsertPopover path={dst_path}/>;

        expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').sourcePath).toBeUndefined();
        expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').destinationPath).toBeUndefined();

        let link = ReactTestUtils.scryRenderedDOMComponentsWithClass(element, "exa-insert-popover-item exa-btn-bookmark")[0];
        expect(link.children[0].getAttribute("class")).toContain("glyphicon-screenshot");
        ReactTestUtils.Simulate.click(link);
        setTimeout(() => {
            let selectedLink = ReactTestUtils.scryRenderedDOMComponentsWithClass(element, "exa-insert-popover-item exa-btn-bookmark")[0];
            expect(selectedLink.children[0].getAttribute("class")).toContain("glyphicon-screenshot");
            expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').sourcePath).toBe(path1);

            const dst = ReactTestUtils.renderIntoDocument(<Container component={dst_component}/>);
            const dst_link = ReactTestUtils.scryRenderedDOMComponentsWithClass(dst, "exa-insert-popover-item exa-btn-bookmark")[0];      //destination icon
            expect(dst_link.children[0].getAttribute("class")).toContain("glyphicon-ok-circle");
            ReactTestUtils.Simulate.click(dst_link);                                                                                     //click on destination button
            setTimeout(() => {
                expect(link.children[0].getAttribute("class")).toContain("glyphicon-screenshot");
                expect(dst_link.children[0].getAttribute("class")).toContain("glyphicon-screenshot");
                expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').sourcePath).toBeUndefined();
                expect(TheAppState.get('panels', 'dataSources', 'items', '0', 'copyFields').destinationPath).toBeUndefined();
                ReactDOM.unmountComponentAtNode(ReactDOM.findDOMNode(dst).parentNode);
                TheHistory.undo().then(done).catch(done);
            }, 10);
        }, 10);
    });

    it('copy node after current', (done) => {
        const dst_path = "0" + c.PATH_SEP + "items" +c.PATH_SEP + "1";
        const dst_component = <InsertPopover path={dst_path}/>;
        const dst = ReactTestUtils.renderIntoDocument(<Container component={dst_component}/>);       //not first

        const cursor = TheMatrixList["0"].select("data", "0", "items");
        var startMatrix = cursor.get();
        var matrixAfterInsert = _.cloneDeep(startMatrix);
        matrixAfterInsert.splice(2, 0, matrixAfterInsert[1]);

        let link = ReactTestUtils.scryRenderedDOMComponentsWithClass(dst, "btn btn-xs btn-default")[4];
        ReactTestUtils.Simulate.click(link);
        setTimeout(() => {
            let mapFn = (x) => {
                return {
                    values: x.values,
                    items: x.items,
                    errors: x.errors
                }
            };
            expect(cursor.get().map(mapFn)).toEqual(matrixAfterInsert.map(mapFn));
            let afterUndo = () => {
                expect(cursor.get().map(mapFn)).toEqual(startMatrix.map(mapFn));        //restored items
                ReactDOM.unmountComponentAtNode(ReactDOM.findDOMNode(dst).parentNode);
                done();
            };
            TheHistory.undo().then(afterUndo).catch(afterUndo);
        }, 10);
    });

    it('copy node before current', (done) => {
        const dst_path = "0" + c.PATH_SEP + "items" +c.PATH_SEP + "1";
        const dst_component = <InsertPopover path={dst_path}/>;
        const dst = ReactTestUtils.renderIntoDocument(<Container component={dst_component}/>);       //not first

        const cursor = TheMatrixList["0"].select("data", "0", "items");
        var startMatrix = cursor.get();
        var matrixAfterInsert = _.cloneDeep(startMatrix);
        matrixAfterInsert.splice(1, 0, matrixAfterInsert[1]);

        let link = ReactTestUtils.scryRenderedDOMComponentsWithClass(dst, "btn btn-xs btn-default")[5];
        ReactTestUtils.Simulate.click(link);
        setTimeout(() => {
            let mapFn = (x) => {
                return {
                    values: x.values,
                    items: x.items,
                    errors: x.errors
                }
            };
            expect(cursor.get().map(mapFn)).toEqual(matrixAfterInsert.map(mapFn));
            let afterUndo = () => {
                expect(cursor.get().map(mapFn)).toEqual(startMatrix.map(mapFn));        //restored items
                ReactDOM.unmountComponentAtNode(ReactDOM.findDOMNode(dst).parentNode);
                done();
            };
            TheHistory.undo().then(afterUndo).catch(afterUndo);
        }, 10);
    });

    it('delete', (done) => {
        const dst_path = "0" + c.PATH_SEP + "items" +c.PATH_SEP + "1";
        const dst_component = <InsertPopover path={dst_path}/>;
        const dst = ReactTestUtils.renderIntoDocument(<Container component={dst_component}/>);       //not first

        const cursor = TheMatrixList["0"].select("data", "0", "items");
        var startMatrix = cursor.get();
        var matrixAfterInsert = _.cloneDeep(startMatrix);
        matrixAfterInsert.splice(0, 1);

        let link = ReactTestUtils.scryRenderedDOMComponentsWithClass(element, "btn-danger")[0];
        ReactTestUtils.Simulate.click(link);
        setTimeout(() => {
            let mapFn = (x) => {
                return {
                    values: x.values,
                    items: x.items,
                    errors: x.errors
                }
            };
            expect(cursor.get().map(mapFn)).toEqual(matrixAfterInsert.map(mapFn));
            let afterUndo = () => {
                expect(cursor.get().map(mapFn)).toEqual(startMatrix.map(mapFn));        //restored items
                ReactDOM.unmountComponentAtNode(ReactDOM.findDOMNode(dst).parentNode);
                done();
            };
            TheHistory.undo().then(afterUndo).catch(afterUndo);
        }, 10);
    });

});
