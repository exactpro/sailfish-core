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

import {PanelCfg, StoreCfg} from 'actions/__models__.js';

export function getMainStoreId() {
    return '' + window.EPS.matrixId;
}

export const initialState = {
    editor: {
        dict: {},

        actions: {},
        services: [],

        environment: 'default',
        encoding: 'UTF-8'
    },

    panels: {
        active: 'left',
        order: ['left', 'right'],
        items: {
            'left': new PanelCfg ({
                panelId: 'left',
                storeId: getMainStoreId(),
                showBreadcrumbs: true,
                showBookmarks: true,
                canClose: false,
                searchText: ''
            }),
            'right': new PanelCfg ({
                panelId: 'right',
                storeId: getMainStoreId(),
                showBreadcrumbs: true,
                showBookmarks: true,
                canClose: true,
                searchText: ''
            })
        },
        dataSources: {
            items: {
                [getMainStoreId()]: new StoreCfg({
                    storeId: getMainStoreId(),
                    filename: '',
                    title: undefined,
                    readonly: false,
                    date: undefined,
                    amlVersion: undefined,
                    loaded: undefined,
                    bookmarks: [],
                    copyFields: {
                        sourcePath: undefined,
                        destinationPath: undefined
                    }
                })
            }
        }
    },

    // setting for nodes;
    // each BaseNode component subscribe on this cursor ( 'node/<panelId>' )
    node: {
        left: { // panelId
            editingPath: undefined,
            scroll: {
                toPath: null,
                visible: []
            },
            overPath: null,
            search: {       //TODO to separate cursor
                path: '',
                pathPointer: 0, // next path item pointer.
                // Example: path = '1/items/22/values/2' pathPointer = 8 (points to 8-th character) : 1/items/*22/values/2
                results: [] // paths
            }
        },
        right: {
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
            },
        }
    },

    propList: {
        items:{
          [getMainStoreId()]: {
              editingPath: undefined
          }
        }
    },

    errors: {
        list: [
        // {
        //     severity: "success", // possible: success, info, warning and danger
        //     message: "Press 'Load' to load matrix",
        //     rootCause: "",
        //     visible: true,
        //     hasTimeout: true
        // }
        ]
    },

    history: {
        blocked: false
    },

    execution: {
        id: -1
    }
};

export default initialState;
