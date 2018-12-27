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

import FakeWindow from "../mock/FakeWindow.js";
import TheMatrixList from 'state/TheMatrixList.js';
import TheAppState from 'state/TheAppState.js';
import {
    CURRENT_SEARCH_RESULT,
    CONTAINS_CURRENT_SEARCH_RESULT,
    SEARCH_RESULT,
    CONTAINS_SEARCH_RESULT
} from 'consts';
import TheFinder from 'actions/TheFinder.js';
import TheHelper from 'actions/TheHelper.js';

describe ('find text in script', function() {
    var _ = require('lodash');
    var emptyFn = Function.prototype;

    const nodeCursor = TheAppState.select("node", "left");

    it('search results are correct for curent matrix', function(done) {
        const searchResultCursor = TheAppState.select("node", "left", "search");
        searchResultCursor.on('update', (newState) => {
            if (newState.data.data.path) {
                const results = newState.data.data.results;
                expect(results.length).toEqual(4);
                expect(_.difference(TheHelper.pathAsArr(results[0]), ["0", "values", "#reference"])).toEqual([]);
                expect(_.difference(TheHelper.pathAsArr(results[1]), ["0", "values", "#action"])).toEqual([]);
                expect(_.difference(TheHelper.pathAsArr(results[2]), ["5", "items", "0", "values", "#description"])).toEqual([]);
                expect(_.difference(TheHelper.pathAsArr(results[3]), ["5", "items", "2", "values", "#description"])).toEqual([]);
                searchResultCursor.off('update');
                done();
            }
        });
        TheFinder.find("left", "Case", []);
    });

    it('first expected path contains in left node', function() {
        const searchResultCursor = TheAppState.select("node", "left", "search");
        const anotherNodeCursor = TheAppState.select("node", "right", "search");
        const firstResult = searchResultCursor.get("results", "0");
        const results = searchResultCursor.get("results");
        expect(_.difference(searchResultCursor.get("path"), firstResult)).toEqual([]);
        expect(_.difference(results, anotherNodeCursor.get("results"))).not.toEqual([]);
        expect(anotherNodeCursor.get("path")).toEqual("");
    });

    it('should return correct search result for first tc node (current result)', function(done) {
        const searchResultCursor = TheAppState.select("node", "left");
        expect(searchResultCursor.get("search","results").length).toEqual(4);
        const component = {
            props: {
                path: TheHelper.createPath(0)
            }
        };
        const result = TheFinder.getSearchStatus(component, "left", searchResultCursor);
        expect(result).toEqual(CURRENT_SEARCH_RESULT);
        expect(searchResultCursor.get().search.pathPointer).toEqual(component.props.path.length + 1); // position after first separator
        setTimeout(done, 10);
    });

    it('should return correct search result for node (search result, but not current), tc node should change its status to CONTAINS_CURRENT_SEARCH_RESULT', function(done) {
        // parent test case node
        const tcNode = {props: {path: TheHelper.createPath(0, "values")}};

        // child action node
        const actNode = {props: {path: TheHelper.createPath(5, 'items', 0)}};

        //tc node contains search result
        const result1 = TheFinder.getSearchStatus(tcNode, "left", nodeCursor);
        expect(result1).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        //
        // //action node, defined in search results, but not current
        const result2 = TheFinder.getSearchStatus(actNode, "left", nodeCursor);
        expect(result2).toEqual(CONTAINS_SEARCH_RESULT);
        //
        // // position should be after test case node
        expect(nodeCursor.get().search.pathPointer).toEqual(tcNode.props.path.length + 1);

        //needs to clean search cache
        setTimeout(done, 10);
    });

    it('should return correct search result for node (not search result)', function(done) {
        expect(nodeCursor.get().search.pathPointer).toEqual(9);
        // nodeCursor.select('search', 'pathPointer').set(0);
        TheFinder.gotoNextResult("left");
        TheFinder.gotoNextResult("left");

        expect(nodeCursor.get().search.path).toEqual(TheHelper.createPath(5, "items", 0, "values", "#description"));

        // parent test case node
        const proper_tc_node = {props: {path: TheHelper.createPath(5)}};

        const act_node = {props: {path: TheHelper.createPath(5, "items", 2, "values", "#description")}};

        const act_node_current = {props: {path: TheHelper.createPath(5, "items", 0, "values", "#description")}};

        const wrong_act_node = {props: {path: TheHelper.createPath(3, "items", 0)}};

        const wrong_tc_node = {props: {path: TheHelper.createPath(3)}};

        //ts doesn't contain search results
        const result0 = TheFinder.getSearchStatus(wrong_tc_node, "left", nodeCursor);
        expect(result0).toEqual(0);
        expect(nodeCursor.get().search.pathPointer).toEqual(0);

        //tc node contains search result
        const result00 = TheFinder.getSearchStatus(proper_tc_node, "left", nodeCursor);
        expect(result00).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(proper_tc_node.props.path.length + 1);

        //finded action, but not current
        const result1 = TheFinder.getSearchStatus(act_node, "left", nodeCursor);
        expect(result1).toEqual(SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(proper_tc_node.props.path.length + 1);

        //current search result
        const result2 = TheFinder.getSearchStatus(act_node_current, "left", nodeCursor);
        expect(result2).toEqual(CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(act_node_current.props.path.length + 1);

        //not search result
        const result3 = TheFinder.getSearchStatus(wrong_act_node, "left", nodeCursor);
        expect(result3).toEqual(0);
        expect(nodeCursor.get().search.pathPointer).toEqual(act_node_current.props.path.length + 1);

        //needs to clean search cache
        setTimeout(done, 10);
    });

    it('should return correct search result for target service node', function(done) {

        nodeCursor.select('search','pathPointer').set(0);

        const parentTcNode = {props: {path: TheHelper.createPath(5)}};

        const serviceNodeContains  = {
            props: {
                path: TheHelper.createPath(5),
                isServiceNode: true,
                body: {
                    props: {
                        startIdx: 2,
                        endIdx: 99
                    }
                }
            }
        };

        const wrongServiceNode2  = {
            props: {
                path: TheHelper.createPath(5),
                isServiceNode: true,
                body: {
                    props: {
                        startIdx: 200,
                        endIdx: 299
                    }
                }
            }
        };

        const targetServiceNode = {
            props: {
                path: TheHelper.createPath(5),
                isServiceNode: true,
                body: {
                    props: {
                        startIdx: 0,
                        endIdx: 1
                    }
                }
            }
        };

        //tc node which contains search result
        expect(TheFinder.getSearchStatus(parentTcNode, "left", nodeCursor)).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(parentTcNode.props.path.length + 1);

        const result0 = TheFinder.getSearchStatus(wrongServiceNode2, "left", nodeCursor);
        expect(result0).toEqual(0);
        expect(nodeCursor.get().search.pathPointer).toEqual(parentTcNode.props.path.length + 1);

        //service node which contains results
        const result1 = TheFinder.getSearchStatus(serviceNodeContains, "left", nodeCursor);
        expect(result1).toEqual(CONTAINS_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(parentTcNode.props.path.length + 1);

        //service node which contains current results
        const result2 = TheFinder.getSearchStatus(targetServiceNode, "left", nodeCursor);
        expect(result2).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(parentTcNode.props.path.length + 1);


        const result3 = TheFinder.getSearchStatus(wrongServiceNode2, "left", nodeCursor);
        expect(result3).toEqual(0);
        expect(nodeCursor.get().search.pathPointer).toEqual(parentTcNode.props.path.length + 1);

        //needs to clean search cache
        setTimeout(done, 10);
    });

    it('should return correct search result for node nested in service node', function(done) {

        nodeCursor.select('search','pathPointer').set(0);

        const parentTcNode = {
            context: {
                side: 'left'
            },
            props: {
                path: TheHelper.createPath("5")
            }
        };

        const targetServiceNode = {
            context: {
                side: 'left'
            },
            props: {
                path: TheHelper.createPath("5", "items", "150"),
                isServiceNode: true,
                body: {
                    props: {
                        startIdx: 100,
                        endIdx: 199
                    }
                }
            }
        };

        const lastNode = {
            context: {
                side: 'left'
            },
            props: {
                path: TheHelper.createPath("5", "items", "0")
            }
        };

        //tc node which contains search result
        expect(TheFinder.getSearchStatus(parentTcNode, "left", nodeCursor)).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(parentTcNode.props.path.length + 1);

        //service node which contains current results
        const result2 = TheFinder.getSearchStatus(targetServiceNode, "left", nodeCursor);
        // expect(result2).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        // expect(nodeCursor.get().search.pathPointer).toEqual(targetServiceNode.props.path.length + 1);

        //proper action node
        const result3 = TheFinder.getSearchStatus(lastNode, "left", nodeCursor);
        expect(result3).toEqual(CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(lastNode.props.path.length + 1);

        //needs to clean search cache
        setTimeout(done, 10);
    });

    it('should return correct search result after redrawing', function(done) {

        nodeCursor.select('search','pathPointer').set(0);

        const parentTcNode = {
            context: {
                side: 'left'
            },
            props: {
                path: TheHelper.createPath("5")
            }
        };

        const targetServiceNode = {
            context: {
                side: 'left'
            },
            props: {
                path: TheHelper.createPath("5"),

                isServiceNode: true,
                body: {
                    props: {
                        startIdx: 0,
                        endIdx: 1
                    }
                }
            }
        };

        const lastNode = {
            context: {
                side: 'left'
            },
            props: {
                path: TheHelper.createPath("5", "items", "0")
            }
        };

        //tc node which contains search result
        expect(TheFinder.getSearchStatus(parentTcNode, "left", nodeCursor)).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(parentTcNode.props.path.length + 1);

        //service node which contains current results
        const result2 = TheFinder.getSearchStatus(targetServiceNode, "left", nodeCursor);
        expect(result2).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(targetServiceNode.props.path.length + 1);

        //proper action node
        const result3 = TheFinder.getSearchStatus(lastNode, "left", nodeCursor);
        expect(result3).toEqual(CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(lastNode.props.path.length + 1);

        //path pointer should not change

        //tc node which contains search result
        expect(TheFinder.getSearchStatus(parentTcNode, "left", nodeCursor)).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(lastNode.props.path.length + 1);

        //service node which contains current results
        const result4 = TheFinder.getSearchStatus(targetServiceNode, "left", nodeCursor);
        expect(result4).toEqual(CONTAINS_CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(lastNode.props.path.length + 1);

        //proper action node
        const result5 = TheFinder.getSearchStatus(lastNode, "left", nodeCursor);
        expect(result5).toEqual(CURRENT_SEARCH_RESULT);
        expect(nodeCursor.get().search.pathPointer).toEqual(lastNode.props.path.length + 1);

        //needs to clean search cache
        setTimeout(done, 10);
    });
});
