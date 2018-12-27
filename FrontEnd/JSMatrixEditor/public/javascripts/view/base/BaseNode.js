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

import React from 'react';
import {findDOMNode} from 'react-dom';
import {isEqual} from 'lodash';

import {NODE_CT_CLS, BaseNodeView} from './BaseNodeView.jsx';
import {TheFinder, CONTAINS_CURRENT_SEARCH_RESULT, CURRENT_SEARCH_RESULT} from 'actions/TheFinder.js';
import TheHelper from 'actions/TheHelper.js';
import TheScroller from 'actions/TheScroller.js';
import TheAppState from 'state/TheAppState.js';
import {baseContextTypes} from 'contextFactory';

export class BaseNode extends BaseNodeView {
    static displayName = 'BaseNode';

    static contextTypes = baseContextTypes;

    constructor(props, context) {
        super(props, context);

        this.handleMouseEnter = this.handleMouseEnter.bind(this);
        this._handleUpdateNodeCursor = this._handleUpdateNodeCursor.bind(this);
        this._handleUpdateCopyCursor = this._handleUpdateCopyCursor.bind(this);
    }

    _isMouseOver(props, nodeCursor) {
        return (!props.isServiceNode && props.path === nodeCursor.get('overPath'));
    }

    _getClosesNodeCt(el) {
        while (el && el !== document.body) {
            if (el.classList.contains(NODE_CT_CLS)) {
                return el;
            } else {
                el = el.parentNode;
            }
        }
        return null;
    }

    handleMouseEnter(event) {
        if (findDOMNode(this) === this._getClosesNodeCt(event.target)) {
            TheHelper.setMouseOverNode(this.props.path, this.context.panelId);
        }
    }

    _getNodeCursor() {
        if (this.__nodeCursor === undefined) {
            this.__nodeCursor = TheAppState.select('node', this.context.panelId);
        }
        return this.__nodeCursor;
    }

    _getCopyCursor() {
        if (this.__copyCursor === undefined) {
            this.__copyCursor = TheAppState.select('panels', 'dataSources', 'items', this.context.storeId, 'copyFields');
        }
        return this.__copyCursor;
    }

    _handleUpdateCopyCursor(event) {
        const cursor = event.target;
        const state = this.state;
        const stateUpdate = {};

        const copyValues = cursor.get();
        const isCopyPasteSource = (this.props.path==copyValues.sourcePath);
        const isCopyPasteSourceChanged = isCopyPasteSource !== state.isCopyPasteSource;

        if (isCopyPasteSourceChanged) {
            stateUpdate.isCopyPasteSource = isCopyPasteSource;
        }

        const newState = Object.assign({}, state, stateUpdate);

        const needsUpdateEditorView = (
            newState.expanded
            && newState.visible
            && newState.visible !== state.visible
        );

        const callback = this.__afterStateUpdate.bind(this, false, needsUpdateEditorView);

        if (!isEqual(state, newState)) {
            this.setState(stateUpdate, callback);
        } else {
            callback();
        }
    }

    _handleUpdateNodeCursor(event) {
        const cursor = event.target;
        const state = this.state;
        const stateUpdate = {};

        const searchStatus = TheFinder.getSearchStatus(this, this.context.panelId, cursor);
        const isSearchStatusChanged = searchStatus !== state.searchStatus;

        let needsExpanding = false,
            needsScrollIntoView = false,
            scrollToPath;

        if (isSearchStatusChanged) {
            stateUpdate.searchStatus = searchStatus;
            if (searchStatus !== 0) {
                needsScrollIntoView = needsExpanding = (searchStatus >= CURRENT_SEARCH_RESULT);
            }
        }

        let isVisible = searchStatus > 0;

        if (!needsExpanding) {
            scrollToPath = cursor.get('scrollToPath');
            const myPath = this.props.path;
            if (scrollToPath === myPath) {
                needsScrollIntoView = needsExpanding = true;
//FIXME move following code into actions.
//VIEW should not write to state directly (only throw actions)
//in perfect case - we should send event 'scroll-target-reached' from here;
                cursor.set('scrollToPath', null);
                cursor.tree.commit();
            } else {
                needsScrollIntoView = needsExpanding = TheFinder.isOnPath(this, scrollToPath);
            }
        }

        //FIXME isIntoView give not actual info; ????
        if (!isVisible) {
            isVisible = TheScroller.isIntoView(this);
        }

        stateUpdate.isMouseOver = this._isMouseOver(this.props, cursor);
        stateUpdate.visible = isVisible;
        stateUpdate.expanded = state.expanded || needsExpanding;

        const newState = Object.assign({}, state, stateUpdate);

        const needsUpdateEditorView = (
            newState.expanded
            && newState.visible
            && newState.visible !== state.visible
        );

        const callback = this.__afterStateUpdate.bind(this, needsScrollIntoView, needsUpdateEditorView);

        if (!isEqual(state, newState)) {
            this.setState(stateUpdate, callback);
        } else if (searchStatus === CONTAINS_CURRENT_SEARCH_RESULT || searchStatus === CURRENT_SEARCH_RESULT) {
            // in scenario, where we have several search results in nested node we shold update nested nodes...
            // so, update state -> shouldComponentUpdate -> rerender
            this.setState(stateUpdate, callback);
        } else {
            callback();
        }
    }

    __afterStateUpdate(needsScrollIntoView, needsUpdateEditorView) {
        if (needsScrollIntoView) {
            this.__scrollIntoView();
        }
        if (needsUpdateEditorView) {
            TheScroller.handleScroll(this.context.panelId);
        }
    }

    __scrollIntoView() {
        const myEl = findDOMNode(this);
        myEl.scrollIntoView();
    }

    componentDidMount() {
        this._getNodeCursor().on('update', this._handleUpdateNodeCursor);
        this._getCopyCursor().on('update', this._handleUpdateCopyCursor);
    }

    componentWillUnmount() {
        this._getNodeCursor().off('update', this._handleUpdateNodeCursor);
        this._getCopyCursor().off('update', this._handleUpdateCopyCursor);
    }
}

export default BaseNode;
