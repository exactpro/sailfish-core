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
import TheStoreManager from "actions/TheStoreManager.js";
import TheMatrixList from 'state/TheMatrixList.js';
import TheAppState from 'state/TheAppState.js';
import TheServer from "actions/TheServer.js";
import {StoreCfg} from 'actions/__models__.js';
import TheDictManager from "actions/TheDictManager.js";

describe('TheStoreManager tests', function() {

    /*
    * loadStoreById
    * loadMainStore
    */

   it('createStore', () => {
     const storeID_1 = "1";
     const storeID_2 = "2";

     const initialData = [{
         "items": [],
         "values": {
             "#id": "123"
         },
         "errors": []
     }, {
         "items": [],
         "values": {
             "#id": "125"
         },
         "errors": []
     }];

     expect(TheMatrixList[storeID_1]).toBeUndefined();                      //store #1 is not exists
     expect(TheMatrixList[storeID_2]).toBeUndefined();                      //store #2 is not exists

     const store1 = TheStoreManager.createStore(storeID_1);
     expect(TheMatrixList[storeID_1]).toBe(store1);                         //store exists in MatrixList
     expect(store1.get('data')).toBeUndefined();                            //new store with undefined data

     const store1_1 = TheStoreManager.createStore(storeID_1);
     expect(TheMatrixList[storeID_1]).toBe(store1_1);                       //store exists in MatrixList
     expect(store1).toBe(store1_1);                                         //it's same store
     expect(store1_1.get('data')).toBeUndefined();                          //new store with undefined data

     const store1_2 = TheStoreManager.createStore(storeID_1, initialData);
     expect(TheMatrixList[storeID_1]).toBe(store1_2);                       //store exists in MatrixList
     expect(store1).toBe(store1_2);                                         //it's same store
     expect(store1_2.get('data')).toEqual(initialData);                     //store contains defined data

     const store2 = TheStoreManager.createStore(storeID_2, initialData);
     expect(TheMatrixList[storeID_2]).toBe(store2);                         //store exists in MatrixList
     expect(store2.get('data')).toEqual(initialData);                       //new store with defined data
   });

   it('loadStoreById', (done) => {

       spyOn(TheServer, 'load').and.callThrough();

       TheStoreManager.loadStoreById("1", () => {
           expect(TheServer.load).not.toHaveBeenCalled();
           done();
           //TODO load dictionary from server
       });
   });

   it('addStore', () => {
       let cfg = new StoreCfg({
           storeId: "200",
           filename: 'abc.csv',
           title: 'abc.csv',
           readonly: true,
           date: undefined,
           amlVersion: 3,
           loaded: true,
           bookmarks: []
       });

       TheStoreManager.addStore(cfg, []);
       expect(TheAppState.get('panels', 'dataSources', 'items', '200')).toEqual(cfg);
       expect(TheMatrixList['200']).toBeDefined();
       expect(TheMatrixList['200'].get('data')).toEqual([]);
   });

   xit('switchDataSource', (done) => {

       //TODO; __getStoreCursor must be set

       const fakeData = {
           "id": '10',
           "name": "AML3_14_test_newGenerator.csv",
           "date": 1429177344000,
           "amlVersion": 3,
           "data": []
       }

       spyOn(TheServer, 'load').and.callFake((matrixId, callback) => {setTimeout(() => {callback(null, fakeData) } ,1)});

       expect(TheAppState.get('panels', 'items', 'right', 'storeId')).toBe('0');
       TheStoreManager.switchDataSource('right', '1');
       setTimeout(() => {
           expect(TheAppState.get('panels', 'items', 'right', 'storeId')).toBe('1');        //loaded matrix
           expect(TheServer.load).not.toHaveBeenCalled();
           TheStoreManager.switchDataSource('right', '10');
           setTimeout(() => {
               expect(TheServer.load).toHaveBeenCalled();                                   //not loaded matrix
               done();
           }, 10);
       }, 10);
   });
});
