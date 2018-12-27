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
import BaseAction from './Base.js';
import {PanelCfg, StoreCfg} from './__models__.js';
import TheMatrixList from 'state/TheMatrixList.js';
import {TheAppState, getMainStoreId} from 'state/TheAppState.js';
import {assignNodeKeys, EpsAppError} from 'utils.js';
import storeFactory from 'state/storeFactory.js';
import TheServer from 'actions/TheServer.js';
import TheDictManager from 'actions/TheDictManager';

class StoreManager extends BaseAction {
    constructor(...args) {
        super(...args);

        this.__propcessConfigs();

        this.__getAllStoresCursor().on('update', this.__handleCursorUpdate.bind(this));
        // this.loadStoreById(getMainStoreId());
    }

    createStore(storeId, data=undefined) {
        const store = TheMatrixList[storeId] = TheMatrixList[storeId] ||
            storeFactory({
                storeId: storeId,
                data: data
            });
        if (data) {
            store.select('data').set(data);
        }
        return store;
    }

    loadStoreById(storeId, cb = Function.prototype) {
        const baobab = this.createStore(storeId);
        if (baobab.isLoaded === true) {
            return cb();
        }
        if (baobab.isLoadingInProgress === false) {
            baobab.isLoadingInProgress = true;
            TheServer.load(storeId, (err, resp) => {
                if (err) {
                    throw new EpsAppError('Failed to load matrix', err);
                }

                const data = assignNodeKeys(resp.data);
                //baobab.isLoadingInProgress = false;
                baobab.set('data', data);

                this.__getStoreCursor(storeId).set('loaded', true);
                cb();
            });
        }
    }

    __updateAvailableStores() {
        return new Promise((ok, fail) => {
            TheServer.listOfMatrixes((err, data) => {
                if (err) {
                    return fail(err);
                }

                const stores = data['matrices'];
                const len = stores.length;
                let i, store;
                for (i = 0; i < len; i++) {
                    store = stores[i];
                    this.__addStore(new StoreCfg({
                        storeId: store.id,
                        amlVersion: store.amlVersion,
                        date: store.date,
                        title: store.name,
                        filename: store.name,
                        readonly: getMainStoreId() !== store.id
                    }));
                    this.__addPropItem({
                        storeId: store.id,
                        editingPath: undefined
                    });
                    //TODO add in propList
                }
                ok();
            });
        });
    }

    __propcessConfigs() {
        const allStores = this.__getAllStoresCursor().get();
        for (let storeId in allStores) {
            this.createStore(storeId);
        }
    }

    __handleCursorUpdate() {
        this.__propcessConfigs();
    }

    __getAllStoresCursor() {
        return TheAppState.select('panels', 'dataSources', 'items');
    }

    __getStoreCursor(storeId) {
        return TheAppState.select('panels', 'dataSources', 'items', storeId);
    }

    __getPanelCursor(panelId) {
        return TheAppState.select('panels', 'items', panelId);
    }

    __addStore(store) {
        return this.__getStoreCursor(store.storeId).set(store);
    }

    addStore(storeCfg, data) {
        this.__getStoreCursor(storeCfg.storeId).set(storeCfg);
        this.createStore(storeCfg.storeId, data);
    }

    __addPropItem({storeId, editingPath}) {
        const propCursor = TheAppState.select('propList', 'items');
        return propCursor.set(storeId, {"editingPath":editingPath});
    }

    loadMainStore(cb) {
        this.loadStoreById(getMainStoreId(), cb);
    }

    switchDataSource(panelId, storeId) {
        this.__getPanelCursor(panelId).set('storeId', storeId);
        this.loadStoreById(storeId, () => {
            TheDictManager.preloadDictionariesFromMatrix(storeId);
        });
    }

}

export const TheStoreManager = new StoreManager;
TheStoreManager.__updateAvailableStores().then(() => {
    TheStoreManager.loadMainStore(() => {
        TheDictManager.preloadDictionariesFromMatrix();
    });
});

export default TheStoreManager;
