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
import {DelayedTask} from '../utils.js';
import Base from './Base.js';
import TheAppState from 'state/TheAppState.js';

const NODE_EXPANDABLE = 1;
const NODE_COLLAPSED = 2;
const NODE_EXPANDED = 4;
const NODE_LEAF = 8;
const UPDATE_CURRENT_NODE_DELAY = 50;

class NodeHelper extends Base {
    constructor(...args) {
        super(...args);
        this.__createMouseOverNodeTask();
    }

    /**
     *
     * @param {boolean} isExpanded
     * @param {boolean} isLeaf
     * @returns {number}
     */
    getNodeExpandState(isExpanded, isLeaf) {
        if (isLeaf) {
            return NODE_LEAF;
        } else {
            return NODE_EXPANDABLE ^ (isExpanded ? NODE_EXPANDED : NODE_COLLAPSED);
        }
    }

    /**
     *
     * @param {number} state
     * @returns {boolean}
     */
    isNodeLeaf(state) {
        return (state & NODE_LEAF) === NODE_LEAF;
    }

    /**
     *
     * @param {number} state
     * @returns {boolean}
     */
    isNodeExpandable(state) {
        return (state & NODE_EXPANDABLE) === NODE_EXPANDABLE;
    }

    /**
     *
     * @param {number} state
     * @returns {boolean}
     */
    isNodeExpanded(state) {
        return (state & NODE_EXPANDED) === NODE_EXPANDED;
    }

    /**
     *
     * @param {number} state
     * @returns {boolean}
     */
    isNodeCollapsed(state) {
        return (state & NODE_COLLAPSED) === NODE_COLLAPSED;
    }

    /**
     *
     * @param {string} path
     * @param {string} side
     */
    setMouseOverNode(path, side) {
        if (this.__mouseOverNodeTask.data.path !== path || this.__mouseOverNodeTask.data.side !== side) {
            this.__mouseOverNodeTask.data.path = path;
            this.__mouseOverNodeTask.data.side = side;
            this.__mouseOverNodeTask.delay(UPDATE_CURRENT_NODE_DELAY);
        }
    }

    unsetMouseOverNode(side) {
        this.__setMouseOverNode(null, side);
    }

    /**
     *
     * @param {string} path
     * @param {string} side
     */
    __setMouseOverNode(path, side) {
        const cursor = TheAppState.select('node', side, 'overPath');

        if (path !== cursor.get()) {
            cursor.set(path);
        }
    }

    __createMouseOverNodeTask() {
        this.__mouseOverNodeTask = new DelayedTask(() =>
            this.__setMouseOverNode(this.__mouseOverNodeTask.data.path, this.__mouseOverNodeTask.data.side)
        );
        this.__mouseOverNodeTask.data = {
            path: '',
            side: ''
        };
    }

    /*
     * @param {array} items
     */
    removeEnvironmentFromService(items) {
        if (!Array.isArray(items)) {
            throw 'Illegal argument type';
        }

        items.forEach( el => {
            if (el.values && el.values['#service_name']) {
                let value = el.values['#service_name'];
                let pos = value.indexOf('@');
                if (pos !== -1) {
                    el.values['#service_name'] = value.substr(pos + 1);
                }
            }
            if (el.items) {
                this.removeEnvironmentFromService(el.items);
            }
        });
        return items;
    }
}


export default NodeHelper;
