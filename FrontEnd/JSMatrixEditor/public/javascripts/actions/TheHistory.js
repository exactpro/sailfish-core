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

 import Base from './Base.js';
 const MAX_HISTORY = 10;
 const CURSOR_PATH = ['data'];
 const IS_BLOCKED_PATH = ['isHistoryBlocked'];

 import TheServer from 'actions/TheServer.js';
 import {getMainStoreId} from 'state/TheAppState.js';
 import TheMatrixList from 'state/TheMatrixList.js';
 import {assignNodeKeys} from 'utils.js';

 class HistoryRecord {
     /**
      *
      * @param state - tree snapshot (plain JS objecy)
      * @param definition - (text description of the change)
      */
     constructor(state, definition) {
         this._state = state;
         this._definition = definition;
     }

     get state() {
         return this._state;
     }

     get definitions() {
         return this._definition;
     }
 }

 class Limited {
     /**
      *
      * @param {number} limit
      */
     constructor(limit) {
         this._array = [];
         this._limit = limit;
         this._cursorIdx = 0;
     }
     /**
      *
      * @param {HistoryRecord} value
      * @returns {boolean}  is history full
      */
     push(value) {
         if (this._cursorIdx < this._array.length) {
             this._array = this._array.slice(0, this._cursorIdx);
         }

         const full = this.isFull();
         this._array.push(value);
         if (full) {
             this._array.shift();
         } else {
             this._cursorIdx += 1;
         }
         return full;
     }

     /**
      *
      * @param {HistoryRecord} value
      * @returns {boolean} is history full
      */
     insertFirst(value) {
         const full = this.isFull();
         this._array.unshift(value);
         if (full) {
             this._array.pop();
         }
         this._cursorIdx = 0;
         return full;
     }
     isFull() {
         return this._limit === this._array.length;
     }
     asArray() {
         return this._array.slice();
     }
     getSize() {
         return this._array.length;
     }
     getLimit() {
         return this._limit;
     }
     /**
      *
      * @param {HistoryRecord} saved
      * @returns {HistoryRecord|undefined}
      */
     rollback(saved) {
         const idx = this._array.indexOf(saved);
         var result;
         if (idx >= 0) {
             result = saved;
             this._array = this._array.slice(0,idx + 1);
             this._cursorIdx = idx;
         }
         return result;
     }
     curr() {
         return this._array[this._cursorIdx];
     }
     prev() {
         if (this._cursorIdx > 0) {
             this._cursorIdx -= 1;
             return this._array[this._cursorIdx];
         }
     }
     next() {
         if (this._cursorIdx < this._array.length) {
             this._cursorIdx += 1;
             return this._array[this._cursorIdx];
         }
     }
     isAtEnd() {
         return this._cursorIdx === this._array.length - 1;
     }
 }

 class StateHistory extends Base {
     constructor(...args) {
         super(...args);
         this._history = new Limited(MAX_HISTORY);
         this._now = null;
     }

     redo(storeId = getMainStoreId()) {
         return new Promise((ok, fail) => {
             if (this.isHistoryBlocked(storeId)) {
                 return ok();
             }

             this.blockHistory(storeId);
             const failServer = (response) => {
                 this.unblockHistory(storeId);
                 return fail(response);
             };
             const baobab = TheMatrixList[storeId];
             const cursor = baobab.select(CURSOR_PATH);
             TheServer.redo(
                 failServer,
                 () => {
                     const next = this._history.next();
                     var state;

                     if (next) {
                         state = next.state;
                     } else {
                         if (this._now) {
                             state = this._now;
                             this._now = null;
                         }
                     }

                     if (state) {
                         cursor.set(state);
                         this.unblockHistory(storeId);
                         return ok();
                     } else {
                         TheServer.load(
                             storeId, (error, fromServer) => {
                                 if (error) {
                                     return failServer(error);
                                 } else {
                                     assignNodeKeys(fromServer.data);
                                     cursor.set(fromServer.data);
                                     let result = new HistoryRecord(fromServer.data, 'Load snapshot from server' , []);
                                     this._history.push(result);
                                     this.unblockHistory(storeId);
                                     return ok();
                                 }
                             }
                         );
                     }
                 }
             );
         });
     }

     undo(storeId = getMainStoreId()) {
         return new Promise((ok, fail) => {
             if (this.isHistoryBlocked(storeId)) {
                 return ok();
             }

             this.blockHistory(storeId);
             const failServer = (response) => {
                 this.unblockHistory(storeId);
                 return fail(response);
             };
             const baobab = TheMatrixList[storeId];
             const cursor = baobab.select(CURSOR_PATH);
             TheServer.undo(
                 failServer,
                 () => {
                     var record;
                     const current = this._history.curr();

                     if (!current) {
                         this._now = cursor.get();
                     }
                     record = this._history.prev();

                     if (record) {
                         const state = record.state;
                         cursor.set(state);
                         this.unblockHistory(storeId);
                         return ok();
                     } else {
                         TheServer.load(
                             storeId, (error, fromServer) => {
                                 if (error) {
                                     return failServer(error);
                                 } else {
                                     assignNodeKeys(fromServer.data);
                                     cursor.set(fromServer.data);
                                     let result = new HistoryRecord(fromServer.data, 'Load snapshot from server' , []);
                                     this._history.insertFirst(result);
                                     this.unblockHistory(storeId);
                                     return ok();
                                 }
                             }
                         );
                     }
                 }
             );
         });
     }

     /**
      * path - array related to CURSOR_PATH
      */
     log(definition, path = [], storeId = getMainStoreId()) {
         const cursor = TheMatrixList[storeId].select(CURSOR_PATH);
         const result = new HistoryRecord(cursor.get(), 'before ' + definition , path);
         this._history.push(result);
         this._now = null;
         return result;
     }

     /**
      *
      * @param {HistoryRecord} saved
      * @returns {HistoryRecord|undefined}
      */
     rollback(saved, storeId = getMainStoreId()) {
         const cursor = TheMatrixList[storeId].select(CURSOR_PATH);
         const result = this._history.rollback(saved);
         if (result) {
             cursor.set(result.state);
         }
         return result;
     }

     blockHistory(storeId = getMainStoreId()) {
         this.getLock(storeId).set(true);
     }

     unblockHistory(storeId = getMainStoreId()) {
         this.getLock(storeId).set(false);
     }

     isHistoryBlocked(storeId = getMainStoreId()) {
         return this.getLock(storeId).get();
     }

     getLock(storeId = getMainStoreId()) {
         return TheMatrixList[storeId].select(IS_BLOCKED_PATH);
     }
 }


export const TheHistory = new StateHistory();
export default TheHistory;
