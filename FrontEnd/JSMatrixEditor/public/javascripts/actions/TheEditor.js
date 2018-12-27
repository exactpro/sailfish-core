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
import _ from 'lodash';

const EDITING_PATH = ['editor', 'editing'];

import TheStoreList from 'state/TheMatrixList.js';
import TheAppState, {getMainStoreId} from 'state/TheAppState.js';
import {EpsAppError, assignNodeKeys} from 'utils.js';
import {TheHelper} from 'actions/TheHelper.js';
import TheServer from 'actions/TheServer.js';
import {PanelCfg} from 'actions/__models__.js';
import TheHistory from 'actions/TheHistory.js';
import TheDictManager from 'actions/TheDictManager.js';

import * as c from 'consts.js';

function getStoreName(storeId) {
    return storeId && TheAppState.get('panels', 'dataSources', storeId, 'title');
}

class Editor extends Base {

    constructor(...args) {
        super(...args);
        // errors cache. stores 'short' paths (example: [1,3])
        this._errorsCollection = [];
    }

    /**
     *
     * @param {string} path
     * @param value
     */
     edit(path, value, storeId = getMainStoreId()) {
         const pathAsArr = TheHelper.pathAsArr(path);
         const key = pathAsArr.pop();
         const cursor = TheStoreList[storeId].select('data').select(pathAsArr);
         const action = Object.assign({}, cursor.get());

         if (!value && (key === '#action' && !action['#reference'] || key === '#reference' &&  !action['#action'])) {
             TheHelper.error('Failed to edit ' + storeId &&  + ': ', '#action or #reference fields should be filled');
             return;
         }

         if (key === '#action') {
             // block -> action
             if (!TheHelper.isBlockAction(value) && TheHelper.isBlockAction(action['#action'])) {
                 TheHelper.error('It\'s not allowed to change IF-block to simple action.');
                 return;
             }
             // action -> block
             if (TheHelper.isBlockAction(value) && !TheHelper.isBlockAction(action['#action'])) {

                 var old_data = Object.assign({}, cursor.up().get());
                 old_data[c.VALUES_FIELD][key] = value;
                 old_data.items = [];

                 var result = {
                     values: { "#execute" : "y"},
                     items: [ old_data ],
                     errors: [],
                     metadata: {}
                 };

                 pathAsArr.pop();

                 const transaction = {
                     actions: [{
                         data: [ result ],
                         start: TheHelper.pathLastKey(pathAsArr),
                         deleteCount: 1,
                         path: TheHelper.pathShort(TheHelper.pathParent(pathAsArr)),
                         shallowReplace: false
                     }]
                 };

                 TheServer.splice(
                     transaction,
                     () => {throw new EpsAppError('Failed to edit ' + getStoreName(storeId) );},
                     (data) => {
                         // remove errors before inserting record (elsewhere errors cache will point to incorrect actions)
                         this._cleanErrorsWithoutRefresh();

                         const savedState = TheHistory.log(`${pathAsArr.join('/')} changed with ${value}`, path);
                         // pessimistic update:
                         assignNodeKeys(result);
                         cursor.up().set(result);
                         this._updateErrors(data);
                     }
                 );
                 return;
             }
         }

         const savedState = TheHistory.log(`${pathAsArr.join('/')} changed with ${value}`, path);
         cursor.set(key, value); // optimistic edit

         if (key === '#action') {
             // action -> block
             if (TheHelper.isBlockAction(value) && !TheHelper.isBlockAction(action['#action'])) {
                 cursor.up().set('items', []);
             }
             if (!TheHelper.isBlockAction(value) && TheHelper.isBlockAction(action['#action'])) {
                 cursor.up().set('items', null);
             }

         }

         action[key] = value;
         TheServer.update(
             pathAsArr,
             {values: action},
             () => {
                 TheHistory.rollback(savedState);
                 throw new EpsAppError('Failed to edit ' + getStoreName(storeId) );
             },
             (resp) => {
                 this._updateErrors(resp, storeId);
             }
         );
     }

    /**
     * @param {MessageDefinition} rowDefinition
     * @param rowPath
     * @param rowValue
     * @param refsToAdd
     * @param storeId
     */
    editAndAddRefsIf(rowDefinition, rowPath, rowValue, refsToAdd = TheHelper.getValuesAsArr(rowValue), storeId = getMainStoreId()) {
        const rowPathAsArray = TheHelper.pathAsArr(rowPath);
        const actionPath = TheHelper.getPathToLastAction(rowPathAsArray);
        const msgType = rowDefinition && rowDefinition.name;
        const oldValue = TheHelper.getValuesByPath(rowPathAsArray, storeId);
        const toAdd = this._createNewReferences(TheStoreList[storeId].select('data'), msgType, actionPath, ...refsToAdd);
        const updateEditedAction = oldValue === undefined || oldValue.trim().length === 0 || (rowValue !== oldValue && rowValue !== undefined);

        //
        // send to server
        //
        const toSend = toAdd.slice();

        if (updateEditedAction) {
            rowValue = '[' + toAdd.map((operation) => operation.values['#reference']).join(',') + ']';

            const action = Object.assign({}, TheHelper.getValuesByPath(actionPath, storeId));
            const actionValues = action[c.VALUES_FIELD] = Object.assign({}, action[c.VALUES_FIELD]);
            actionValues[TheHelper.pathLastKey(rowPath)] = rowValue;

            toSend.push(action);
        }

        const transaction = {
            actions: [{
                data: toSend,
                start: TheHelper.pathLastKey(actionPath),
                deleteCount: updateEditedAction ? 1 /* update existing action */ : 0,
                path: TheHelper.pathShort(TheHelper.pathParent(actionPath))
            }]
        };

        TheServer.splice(
            transaction,
            () => {throw new EpsAppError('Failed to edit ' + getStoreName(storeId) );},
            (data) => {
                // remove errors before inserting record (elsewhere errors cache will point to incorrect actions)
                this._cleanErrorsWithoutRefresh();
                var savedState;

                // pessimistic update:
                if (updateEditedAction) {
                    const path = TheHelper.pathAsArr(rowPath);
                    const pessimisticCursor = TheStoreList[storeId].select('data').select(path);
                    savedState = TheHistory.log(`${path.join('/')} changed with ${rowValue}`, path);        //TODO check this!
                    pessimisticCursor.set(rowValue);
                }

                const cursor = TheStoreList[storeId].select('data').select(actionPath).up();
                const idx = +actionPath[actionPath.length-1];
                toAdd.forEach(record => {
                    cursor.splice([idx, 0, record]);
                });

                this._updateErrors(data);
            }
        );
    }

    _createNewReferences(storeCursor, msgType, insertPath, ...references) {
        const newRefs = new Set(references);
        let refIdx;
        TheHelper.eachActionAbove(
            storeCursor,
            insertPath,
            (action) => {
                const actionRef = action[c.VALUES_FIELD][c.REFERENCE_FIELD];
                refIdx = references.indexOf(actionRef);
                if (refIdx !== -1) {
                    newRefs.delete(actionRef);
                }
            }
        );
        let result = [];
        if (newRefs.size) {
            newRefs.forEach(refName => {
                const action = TheHelper.getActionTemplate(null, refName);
                action.values[c.MESSAGE_TYPE_FIELD] = msgType;
                result.push(action);
            });
        }
        assignNodeKeys(result);
        return result;
    }

    add(path, key, value, storeId = getMainStoreId()) {
        const pathAsArr = TheHelper.pathAsArr(path);
        const cursor = TheStoreList[storeId].select('data').select(pathAsArr);
        const action = Object.assign({}, cursor.get());
        if (!action.hasOwnProperty(key)) {
            action[key] = value;

            const savedState = TheHistory.log(`${pathAsArr.join('/')} added with value ${value}`, path);
            cursor.set(key, value); // optimistic set

            TheServer.update(
                pathAsArr,
                { values: action },
                () => {
                    TheHistory.rollback(savedState);
                    throw new EpsAppError('Failed to edit ' + getStoreName(storeId) );
                },
                (resp) => {
                    this._updateErrors(resp, storeId);
                });
        }
    }


    /**
     *
     * @param {string} path
     */
    start(path, storeId = getMainStoreId()) {
        console.log(`set editing path=${path} for store ${storeId}`);
        TheAppState.select('propList', 'items', storeId).set('editingPath', path);
    }

    /**
     *
     * @param {string} path
     * @param value (or undefined to cancel editing)
     */
    stop(path, value, storeId = getMainStoreId()) {
        console.log(`unset editing path=${path} for store ${storeId}`);
        var cursor = TheAppState.select('propList', 'items', storeId);
        if (cursor.get('editingPath') === null) {
            throw new EpsAppError('edit.stop() reached without edit.start() call. Path=' + path + ' value=' + value);
        }
        cursor.set('editingPath', null);

        if (value !== undefined) {
            this.edit(path, value);
        }
    }

    /**
     *
     * @param {string} path
     */
     remove(path, storeId = getMainStoreId()) {
         const pathAsArray = TheHelper.pathAsArr(path);
         let type = TheHelper.getNodeTypeByPath(pathAsArray);

         if (type == 'actionField' || type == 'testCaseField') {
             let key = pathAsArray.pop();
             let cursor = TheStoreList[storeId].select('data').select(pathAsArray);
             let action = Object.assign({}, cursor.get());
             let oldValue = action[key];

             if ( (key === '#action' && !action['#reference']) || (key === '#reference' && !action['#action']) ) {
                 TheHelper.error('Failed to edit ' + getStoreName(storeId) + ':',
                 '#action or #reference fields should be filled');
                 return;
             }

             const savedState = TheHistory.log(`removed ${pathAsArray.join('/')}`, path);
             cursor.unset(key); // optimistic remove
             delete action[key];

             TheServer.update(
                 pathAsArray,
                 { values: action },
                 (error) => {
                     TheHistory.rollback(savedState);
                     cursor.set(key, oldValue);
                     TheHelper.error('Failed to remove ' + type + ':', error.rootCase);
                 },
                 (resp) => {
                     this._updateErrors(resp, storeId);
                 });
         } else if (type == 'action' || type == 'testCase' || type == 'action+') {
             let index = + pathAsArray[pathAsArray.length - 1];
             let cursor = TheStoreList[storeId].select('data').select(pathAsArray.slice(0, -1));
             const savedState = TheHistory.log(`removed ${pathAsArray.join('/')}`, path);
             cursor.splice([index, 1]);

             TheServer.remove(pathAsArray,
                 (error) => {
                     TheHistory.rollback(savedState);
                     TheHelper.error('Failed to remove ' + type + ':', error.rootCase);
                 },
                 (resp) => {
                     this._updateErrors(resp, storeId);
                 });
         }
     }

    /**
     *
     * @param {string} parentPath - pathAsArray to 'items' element
     * @param {number} index - position in 'items' where we should insert element
     * @param {object} value - { values: {}, items: [] } (itmes are optional)
     */
     insert(parentPath, index, value, storeId = getMainStoreId()) {
         if (value === undefined || value[c.VALUES_FIELD] === undefined) {
             throw new EpsAppError('Incorrect \'value\' parameter');
         }

         const path = TheHelper.pathAsArr(parentPath);
         path.push(index);

         const cursor = TheStoreList[storeId].select('data').select(path);
         const type = TheHelper.getNodeTypeByPath(path);

         if (type === 'action') {
             if (Object.keys(value[c.VALUES_FIELD]).length === 0) {
                // AML skips empty lines. We should add non-empty '#action' or '#reference':
                value[c.VALUES_FIELD]['#action'] = 'Sleep';
                value[c.VALUES_FIELD]['#timeout'] = '1000';
             }
         } else if (type === 'testCase') {
             if (Object.keys(value[c.VALUES_FIELD]).length === 0) {
                 value[c.VALUES_FIELD]['#action'] = 'Test Case Start'; // AMLBlockBrace
             }
         }

         TheServer.create(path, value,
             (error) => TheHelper.error(error.message, error.rootCase),
             (resp) => {
                 var errorsContainer = {
                     errors :resp.errors
                 };
                 // don't push errors to tree here. pass it to _updateErrors()
                 resp.errors = [];
                 // set 'key' field
                 assignNodeKeys(resp);

                 const savedState = TheHistory.log(`${path.join('/')} added`, path);
                //  debugger();
                 if (!cursor.up().get()) {
                     cursor.up().set([resp]);
                 } else {
                    cursor.up().splice([ + index, 0, resp]);
                 }

                 this._updateErrors(errorsContainer);
             }
         );
     }

    /**
     * move
     * @param  {} fromStorer
     * @param  {} fromPath
     * @param  {} count
     * @param  {} toStore
     * @param  {} toPath
     * @param  {} sendToServer =             true
     * @param  {} doMove       =             true
     * @return new path for dragged element
     */
    move(fromStore, fromPath, count, toStore, toPath, sendToServer = true, doMove = true) {
        //TODO add TheHistory.log
        const sameStore = (fromStore === toStore);
        if (sameStore && toPath == fromPath) {
            return fromPath;
        }

        const fromCursor = fromStore.select('data');
        const toCursor = toStore.select('data');

        // we can Drag 2 types of thins:
        // 1. (TestCases) Blocks
        // 2. Actions and IF-Blocks
        //
        // we don't support mixing of this types...
        // normilize paths:
        let p1 = TheHelper.pathAsArr(fromPath);
        let p2 = TheHelper.pathAsArr(toPath);

        var toAction = toCursor.select(p2).select(c.VALUES_FIELD).select(c.ACTION_FIELD).get();
        var isToBlock = TheHelper.isBlockAction(toAction);

        if (isToBlock) {
            toPath = TheHelper.createPath(toPath, c.CHILDREN_FIELD, 0);
            p2 = TheHelper.pathAsArr(toPath);
        }

        const fromParentPath = TheHelper.parentPath(fromPath);
        const fromIdx = + TheHelper.pathLastKey(fromPath);

        let toParentPath = TheHelper.parentPath(toPath);
        const toIdx = + TheHelper.pathLastKey(toPath);
        //
        // Copy data to internal buffer
        //
        // if we don't do 'doMove' - data already in toStore
        const data = [];
        const moveParent = doMove ? fromCursor.select(TheHelper.pathAsArr(fromParentPath)).get()
                                  : toCursor.select(TheHelper.pathAsArr(toParentPath)).get();
        const moveIdx = doMove ? fromIdx : toIdx;
        for (let i = 0; i < count; i++) {
            data.push(moveParent[moveIdx]);
        }
        //
        // remove old position (fromPath)
        //
        if (doMove) {
            fromCursor.select(p1).up().splice([fromIdx, count]);
        }

        // correct to_index if needed:
        //  * removed item was before insert place
        //  * they in same store
        //  * it's not process of building server transaction
        if (!sendToServer && sameStore && p2.length > p1.length) {
            var differentSubTrees = false,
                toPathMoved = false;
            for (var i=0; i<p1.length ; i+=2) { // with step = '2' !!
                if (i == p1.length-1) {
                    if (+p2[i] > +p1[i]) {
                        toPathMoved = true;
                    }
                }
                if (+p2[i] != +p1[i]) {
                    differentSubTrees = true;
                    break;
                }
            }

            toPathMoved &= differentSubTrees;

            if (toPathMoved) {
                p2[p1.length -1] = +p2[p1.length -1]; // cast to number
                p2[p1.length -1] -= count;
                p2[p1.length -1] = '' + p2[p1.length -1]; // cast to string
                toParentPath = TheHelper.parentPath(TheHelper.createPath(...p2));
            }
        }

        //
        // insert new position
        //
        if (doMove) {
            const request = [toIdx, 0];
            request.push(...data);
            try {
                var cursor = toCursor.select(p2).up();
                cursor.splice(request);
            } catch (err) {
                console.error(err);
                console.log(JSON.stringify(toCursor.get()));
                return TheHelper.createPath(...p2);
            }
        }

        console.debug(`fromIdx=${fromStore.options.storeId}>${fromParentPath}>${fromIdx} count=${count} toIdx=${toStore.options.storeId}>${toParentPath}>${toIdx} MOVE=${doMove} SERVER=${sendToServer} isToBlock=${isToBlock}`);

        if (!sendToServer) {
            return TheHelper.createPath(...p2);
        }

        const mainStoreId = getMainStoreId();

        // send to server
        const serverActions = [];

        if (fromStore.options.storeId === mainStoreId) {
            serverActions.push({
                path: TheHelper.toShortPath(fromParentPath),
                start: fromIdx,
                deleteCount: count,
                data: []
            });
        }
        if (toStore.options.storeId === mainStoreId) {
            serverActions.push({
                path: TheHelper.toShortPath(toParentPath),
                start: toIdx,
                deleteCount: 0,
                data: data
            });
        }
        TheServer.splice(
            { actions: serverActions },
            (error) => TheHelper.error(error.message, error.rootCase),
            (resp) => {
                const errorsContainer = {
                    errors: resp.errors
                };
                this._updateErrors(errorsContainer, getMainStoreId());
            }
        );

        return TheHelper.createPath(...p2);
    }

    __expandErrorPath(path) {
        const CHILDREN_FIELD = c.CHILDREN_FIELD;
        const ERRORS_FIELD = c.ERRORS_FIELD;
        // don't modify original array!
        if (path.length === 0) {
            return [ERRORS_FIELD]; // it is matrix-level error
        } else if (path.length > 0) {
            let result = ['data'];
            _.each(path, (p) => result.push(p, CHILDREN_FIELD));
            result[result.length - 1] = ERRORS_FIELD;
            return result;
        }
        throw 'unsupported path ' + path;
    }

    _cleanErrorsWithoutRefresh(storeId = getMainStoreId()) {
        const store = TheStoreList[storeId];

        const CHILDREN_FIELD = c.CHILDREN_FIELD;
        const ERRORS_FIELD = c.ERRORS_FIELD;

        for (let i = 0; i < store.get('data').length; i++) {
            let tcCursor = store.select(['data', i.toString()]);
            if (tcCursor.get(ERRORS_FIELD)) {
                tcCursor.select(ERRORS_FIELD).set([]);
            }
            for (let j = 0; j < tcCursor.get(CHILDREN_FIELD).length; j++) {
                const epath = this.__expandErrorPath([i,j]);
                if (store.select(epath).get()) {
                    store.select(epath).set([]);
                }
            }
        }

        // element's paths can change after DnD, and following code doesn't work correctly
        // this._errorsCollection.forEach((path) => {
        //     const epath = this.__expandErrorPath(path);
        //     if (store.select(epath).get()) {
        //         store.select(epath).set([]);
        //     }
        // });
        this._errorsCollection = [];

        return true;
    }

    _updateErrors(errors, storeId = getMainStoreId()) {
        if (this._updateErrorsWithoutRefresh(errors, storeId)) {
             // schedule redraw (we use other baobab tree (there is no recording in that tree))
             // FIXME - it could break in next baobab version
            TheAppState.update({});
        }
    }

    _updateErrorsWithoutRefresh(data, storeId = getMainStoreId()) {
        const store = TheStoreList[storeId];
        let result = false;

        if (data !== undefined && data.errors !== undefined) {
            this._cleanErrorsWithoutRefresh();

            data.errors.forEach((error) => {
                error.paths.forEach((path) => {
                    this._errorsCollection.push(path); // 'shot path'

                    var epath = this.__expandErrorPath(path);

                    var cursor = store.select(epath);
                    if (!cursor.get()) {
                        cursor.set([]);
                    }
                    cursor.push(error);

                    if (epath.length === 1) {
                        TheHelper.error(error.message, error.rootCase);
                    }
                });
            });

            result = true;
        }

        return result;
    }

    replace(searchFor, searchIn, replaceWith,
        callback = Function.prototype,
        storeId = getMainStoreId()
    ) {
        if (!Array.isArray(searchIn)) {
            throw new Error('IllegalArgument: searchIn must be array');
        }

        let transaction = {
            actions: []
        };

        const savedState = TheHistory.log(`replace in ${searchIn.join('/')} for ${searchFor} to ${replaceWith}`);
        const blocks = TheStoreList[storeId].select('data').get();

        const blocksCount = blocks.length;
        for (let i=0; i<blocksCount; i++) {
            let block = blocks[i];
            this.__replaceInBlock(block, [i], transaction.actions, searchIn, searchFor, replaceWith);
        }

        if (transaction.actions.length === 0) {
            return callback(undefined);
        }

        //
        // send to server
        //
        TheServer.splice(
            transaction,
            () => {
                TheHistory.rollback(savedState);
                throw new EpsAppError('Failed to replace in ' + getStoreName(storeId));
            },
            (data) => {
                // remove errors before inserting record (elsewhere errors cache will point to incorrect actions)
                this._cleanErrorsWithoutRefresh();

                // pessimistic update:
                transaction.actions.forEach((action) => {
                    action.path.push(action.start);
                    const pathAsArr = TheHelper.pathAsArr(TheHelper.expandShortPath(action.path));
                    TheStoreList[storeId].select('data').select(pathAsArr).select(c.VALUES_FIELD).set(action.data[0][c.VALUES_FIELD]);
                });

                this._updateErrors(data, storeId);
                callback(undefined);
            }
        );
    }

    __replaceInBlock(block, path, result, searchIn, searchFor, replaceWith) {
        // search in Block fields:
        let editedBlock = null;
        let values = block[c.VALUES_FIELD];
        for (let key in values) {
            if (searchIn.length > 0 && searchIn.indexOf(key) === -1) {
                continue;
            }
            let value = values[key];
            if (value != null && value.indexOf(searchFor) !== -1) {
                // we don't want to send items,errors,metadata to server
                editedBlock = editedBlock || Object.assign({}, { [c.VALUES_FIELD] : values });
                editedBlock[c.VALUES_FIELD][key] = value.replace(searchFor, replaceWith);
            }
        }

        if (editedBlock !== null) {
            result.push({
                data: [editedBlock],
                start: path[path.length-1],
                deleteCount: 0,
                path: path.slice(0, -1),
                shallowReplace: true
            });
        }

        // search in nested actions:
        let actions = block[c.CHILDREN_FIELD];
        if (!actions) {
            return;
        }

        let actionsCount = actions.length;
        for (let j=0; j<actionsCount; j++) {
            let action = actions[j];
            this.__replaceInBlock(action, [...path, j], result, searchIn, searchFor, replaceWith);
        }
    }

    closePanel(panelId) {
        const activePanel = TheAppState.get('panels', 'active');
        if (activePanel!=panelId) {
            const nodeCursor = TheAppState.select('node');
            nodeCursor.unset(panelId);
            const orderCursor = TheAppState.select('panels', 'order');
            orderCursor.splice([orderCursor.get().indexOf(panelId), 1]);
            const itemsCursor = TheAppState.select('panels', 'items');
            itemsCursor.unset(panelId);
        }
    }

    addPanel(panelId) {
        const newPanelId = panelId + "_copy";
        const copyStoreId = TheAppState.get('panels', 'items', panelId).storeId;
        const nodeCursor = TheAppState.select('node');
        // const visibleNodes = nodeCursor.get()
        nodeCursor.set(newPanelId, {
            editingPath: undefined,
            scroll: {
                toPath: null,
                visible: []
            },
            overPath: null,
            search: {
                path: '',
                pathPointer: 0,
                results: []
            }})
        const itemsCursor = TheAppState.select('panels', 'items');
        itemsCursor.set(newPanelId, new PanelCfg ({
            panelId: newPanelId,
            storeId: copyStoreId,
            showBreadcrumbs: true,
            showBookmarks: true,
            canClose: true,
            searchText: ''
        }));
        const orderCursor = TheAppState.select('panels', 'order');
        orderCursor.splice([(orderCursor.get().indexOf(panelId)+1), 0, newPanelId]);
    }

    checkoutPanel(panelId) {
        const panelCursor = TheAppState.select('panels');
        panelCursor.set('active', panelId);
    }

    copyFieldValues(storeId = getMainStoreId()) {
        const copyCursor = TheAppState.select('panels', 'dataSources', 'items', storeId, 'copyFields');
        const sourcePath = copyCursor.get('sourcePath');
        const destinationPath = copyCursor.get('destinationPath');

        const pathAsArr = TheHelper.pathAsArr(destinationPath);
        const itemCursor = TheStoreList[storeId].select('data').select(pathAsArr).select('values');

        let sourceValues = TheHelper.getValuesByPath(sourcePath, storeId).values;
        let dstValues = TheHelper.getValuesByPath(destinationPath, storeId).values;

        const destinationDefinition = TheDictManager.findDefinition(TheHelper.getValuesByPath(destinationPath, storeId), storeId, pathAsArr);
        let allFields = TheHelper.getNestedData(pathAsArr, destinationValues, destinationDefinition, storeId);

        const destinationValuesArray = allFields.map((item) => {
            return {
                "key": TheHelper.pathLastKey(item.path),
                "value": item.data,
                "definition": item.definition
            }
        });

        let destinationValues = {};
        let definitionValues = {};

        for (let i in destinationValuesArray) {
            let item = destinationValuesArray[i];
            destinationValues[item.key] = item.value;
            definitionValues[item.key] = item.definition;
        }

        let sourceReference = sourceValues['#reference'];
        Object.keys(sourceValues).forEach((key) => {
            if (key[0]!=="#" && sourceValues[key]!==null && sourceValues[key]!==undefined && destinationValues.hasOwnProperty(key)
            && (!(definitionValues[key] && definitionValues[key].type==="SUBMESSAGE")) && (destinationValues[key]!=="*")) {
                if (sourceReference) {
                    destinationValues[key] = "${"+sourceReference + "." + key + "}";
                } else {
                    destinationValues[key] = sourceValues[key];     //TODO copy without reference - is it necessary?
                }
            }
        });

        const savedState = TheHistory.log(`${pathAsArr.join('/')} filled from ${TheHelper.pathAsArr(sourcePath).join('/')}`, pathAsArr, storeId);
        itemCursor.set(destinationValues); // optimistic edit

        copyCursor.unset('sourcePath');
        copyCursor.unset('destinationPath');

        TheServer.update(
            pathAsArr,
            {values: destinationValues},
            () => {
                TheHistory.rollback(savedState);
                throw new EpsAppError('Failed to edit ' + getStoreName(storeId) );
            },
            (resp) => {
                this._updateErrors(resp, storeId);
            }
        );
    }
}

export const TheEditor = new Editor();
export default TheEditor;
