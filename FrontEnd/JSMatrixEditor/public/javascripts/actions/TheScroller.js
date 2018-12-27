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

const SCROLL_BUFFER_PX = 500;
const SCROLL_DELAY = 40;
const SCROLL_MAX_DELAY = -1;

import React from 'react';
import {DelayedTask} from '../utils.js';
import BaseAction from './Base.js';
import TheAppState, {getMainStoreId} from 'state/TheAppState.js';

class Scroller extends BaseAction {
    constructor(...args) {
        super(...args);
        this._scrollUpdate = {};
        this._scrollTask = new DelayedTask(() => {
            for (let panelId in this._scrollUpdate) {
                const ct = document.querySelector('.exa-main-view-half-' + panelId);
                const nodes = Array.from(document.querySelectorAll('.exa-main-view-half-' + panelId + ' .exa-node-ct'));
                const borders = ct.getBoundingClientRect();
                const top = borders.top - SCROLL_BUFFER_PX;
                const bottom = borders.bottom + SCROLL_BUFFER_PX;
                const result = (nodes
                    .filter(x => this._isRectVisible(top, bottom, x.getBoundingClientRect()))
                    .map(x => x.getAttribute('data-nodekey')));
                const cursor = TheAppState.select('node', panelId, 'scroll', 'visible');
                cursor.set(result);
            }
            this._scrollUpdate = {};
        }, SCROLL_MAX_DELAY);

        this._scrollTask.delay(SCROLL_DELAY);
    }

    /**
     *
     * @param {string} panelId
     *
     */
    handleScroll(panelId) {
        this._scrollUpdate =  this._scrollUpdate || {};
        this._scrollUpdate[panelId] = true;
        this._scrollTask.delay(SCROLL_DELAY);
    }

    _isRectVisible(top, bottom, targetRect) {
        return targetRect.top <= bottom && targetRect.bottom >= top;
    }

    updateByStoreId(storeId = getMainStoreId()) {
        Object.values(TheAppState.get('panels', 'items'))
            .filter(x => x.storeId===storeId)
            .map(x => x.panelId)
            .forEach(this.handleScroll.bind(this));

    }

    /**
     *
     * @param reactComponent
     */
    isIntoView(reactComponent) {
        const panelId = reactComponent.context.panelId;
        const nodeKey = reactComponent.props.nodeKey;
        const visibleNodes = TheAppState.select('node', panelId, 'scroll', 'visible').get();
        return (visibleNodes.indexOf(nodeKey) >= 0);
    }
}

export const TheScroller = new Scroller();
export default TheScroller;
