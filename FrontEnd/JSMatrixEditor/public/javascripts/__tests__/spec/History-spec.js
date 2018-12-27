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
import {getMainStoreId} from 'state/TheAppState.js';
import TheHistory from 'actions/TheHistory.js';
import TheServer from 'actions/TheServer.js';

describe ('apply undo/redo', function() {

    var _ = require('lodash');
    var emptyFn =  function() {};

    const baobab = TheMatrixList[getMainStoreId()];

    var path1 = ['data', '4', "historyValue", 'a'];
    var path2 = ['data', '4', "historyValue", 'b'];
    var path3 = ['data', '4', "historyValue", 'c'];

    var cursor = baobab.select(path3);
    var state_before_1 = TheHistory.log('change path3 to 222', path3);
    cursor.set(222);

    cursor = baobab.select(path3);
    var state_before_2 = TheHistory.log('change path3 to 333', path3);
    cursor.set(333);

    cursor = baobab.select(path3);
    var state_before_3 = TheHistory.log('change path3 to 444', path3);
    cursor.set(444);

    var state_before_a1, state_before_a2, state_before_a3;

    it('undo/redo should set correct state', function(done) {
        expect(baobab.select(path3).get()).toEqual(444);
        TheHistory.undo().then(() => {
            expect(baobab.select(path3).get()).toEqual(333);
            TheHistory.redo().then(() => {
                expect(baobab.select(path3).get()).toEqual(444);
                done();
            });
        });
    });

    it('should save current state after redundant call redo', function(done) {
        //TODO TheServer.__set__('load', function(fail, ok) {fail()})
        TheHistory.redo().catch(() => {
            expect(baobab.select(path3).get()).toEqual(444);
            TheHistory.redo().catch(() => {
                expect(baobab.select(path3).get()).toEqual(444);
                TheHistory.redo().catch(() => {
                    expect(baobab.select(path3).get()).toEqual(444);
                    done();
                });
            });
        });
    });

    it('should restore state after redundant call redo and one undo', function(done) {
        TheHistory.undo().then(() => {
            expect(baobab.select(path3).get()).toEqual(333);
            TheHistory.redo().then(() => {
                expect(baobab.select(path3).get()).toEqual(444);
                done();
            });
        });
    });

    it('should restore state after undo', function(done) {
        TheHistory.undo().then(() => {
            expect(baobab.select(path3).get()).toEqual(333);
            TheHistory.undo().then(() => {
                expect(baobab.select(path3).get()).toEqual(222);
                TheHistory.undo().then(() => {
                    expect(baobab.select(path3).get()).toEqual(1);
                    done();
                });
            });
        });
    });

    it('should save state after redundant call undo', function(done) {
        expect(baobab.select(path3).get()).toEqual(1);
        TheHistory.undo().catch(() => {
            expect(baobab.select(path3).get()).toEqual(1);
            TheHistory.undo().catch(() => {
                expect(baobab.select(path3).get()).toEqual(1);
                TheHistory.undo().catch(() => {
                    expect(baobab.select(path3).get()).toEqual(1);
                    done();
                });
            });
        });
    });

    it('should restore state after redundant call undo and few redo', function(done) {
        TheHistory.undo().catch(() => {
            expect(baobab.select(path3).get()).toEqual(1);
            TheHistory.undo().catch(() => {
                expect(baobab.select(path3).get()).toEqual(1);
                TheHistory.redo().then(() => {
                    expect(baobab.select(path3).get()).toEqual(222);
                    TheHistory.redo().then(() => {
                        expect(baobab.select(path3).get()).toEqual(333);
                        TheHistory.redo().then(() => {
                            expect(baobab.select(path3).get()).toEqual(444);
                            TheHistory.redo().catch(() => {
                                expect(baobab.select(path3).get()).toEqual(444);
                                TheHistory.redo().catch(() => {
                                    expect(baobab.select(path3).get()).toEqual(444);
                                    done();
                                });
                            });
                        });
                    });
                });
            });
        });
    });

    it ('should rollback state to saved point', function() {
        expect(baobab.select(path3).get()).toEqual(444);
        TheHistory.rollback(state_before_2);
        expect(baobab.select(path3).get()).toEqual(222);
    });

    it ('should not rollback state to saved point, because state already in earlier point', function() {
        expect(baobab.select(path3).get()).toEqual(222);
        TheHistory.rollback(state_before_3);
        expect(baobab.select(path3).get()).not.toEqual(333);
        expect(baobab.select(path3).get()).toEqual(222);
    });

    it ('should correctly work after rollback state to saved point', function(done) {
        expect(baobab.select(path3).get()).toEqual(222);
        let cursor = baobab.select(path3);
        state_before_a1 = TheHistory.log('change path3 to 555', path3);
        cursor.set(555);
        expect(baobab.select(path3).get()).toEqual(555);
        state_before_a2 = TheHistory.log('change path3 to 666', path3);
        cursor.set(666);
        expect(baobab.select(path3).get()).toEqual(666);
        state_before_a3 = TheHistory.log('change path3 to 777', path3);
        cursor.set(777);
        expect(baobab.select(path3).get()).toEqual(777);

        TheHistory.undo().then(() => {
            expect(baobab.select(path3).get()).toEqual(666);
            TheHistory.undo().then(() => {
                expect(baobab.select(path3).get()).toEqual(555);
                TheHistory.redo().then(() => {
                    expect(baobab.select(path3).get()).toEqual(666);
                    TheHistory.redo().then(() => {
                        expect(baobab.select(path3).get()).toEqual(777);
                        TheHistory.redo().catch(() => {
                            expect(baobab.select(path3).get()).toEqual(777);
                            TheHistory.undo().then(() => {
                                expect(baobab.select(path3).get()).toEqual(666);
                                done();
                            });
                        });
                    });
                });
            });
        });
    });

    it('block history', (done) => {

        spyOn(TheServer, 'undo').and.callThrough();

        expect(TheHistory.isHistoryBlocked()).toBeFalsy();     //start: history is unblocked, item value = 666
        expect(baobab.select(path3).get()).toEqual(666);
        expect(typeof(TheHistory.getLock().get())).toEqual('boolean');

        TheHistory.blockHistory();
        expect(TheHistory.isHistoryBlocked()).toBeTruthy();
        TheHistory.undo().then(() => {
            expect(baobab.select(path3).get()).toEqual(666);    // because history is blocked
            expect(TheHistory.isHistoryBlocked()).toBeTruthy(); // and we have a return at first
            expect(TheServer.undo).not.toHaveBeenCalled();
            TheHistory.unblockHistory();
            expect(TheHistory.isHistoryBlocked()).toBeFalsy();
            TheHistory.undo().then(() => {
                expect(baobab.select(path3).get()).toEqual(555);
                expect(TheServer.undo).toHaveBeenCalled();
                expect(TheHistory.isHistoryBlocked()).toBeFalsy();  //history is unblocked after undo
                done();
            });
        });
    });

    it('log', () => {
        let def1 = "qwerty1";
        let path1 = [];
        let path2 = ["0"];
        let path3 = ["0", "items", "0"];

        const record1 = TheHistory.log(def1, path1);
        const record2 = TheHistory.log(def1, path2);
        const record3 = TheHistory.log(def1, path3);
        const record4 = TheHistory.log(def1);

        expect(record1.definitions).toEqual('before qwerty1');
        expect(record1.state).toEqual(TheMatrixList["0"].get('data'));
        expect(record1).toEqual(record2);
        expect(record1).toEqual(record3);
        expect(record1).toEqual(record4);
    });
});
