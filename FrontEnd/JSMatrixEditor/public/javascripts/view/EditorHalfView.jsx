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

export const CHECK_RESIZE_INTERVAL = 100;

import React, {PropTypes, Component} from 'react';
import {findDOMNode} from 'react-dom';

import EditorTree from './EditorTree.js';
import PanelToolbar from 'view/controls/PanelToolbar.jsx';

import {DelayedTask} from 'utils';
import {baseContextTypes, contextFactory} from 'contextFactory.js';

export class EditorHalfView extends Component {
    static displayName = 'EditorHalfView';

    static childContextTypes = baseContextTypes;

    getChildContext() {
        const props = this.props;
        return contextFactory(props.storeId, props.panelId);
    }

    static propTypes = {
        panelId: PropTypes.string.isRequired,
        storeId: PropTypes.string.isRequired,
        readonly: PropTypes.bool.isRequired,
        canClose: PropTypes.bool.isRequired,
        showBreadcrumbs: PropTypes.bool.isRequired,
        showBookmarks: PropTypes.bool.isRequired,
        isActive: PropTypes.bool.isRequired,

        onClose: PropTypes.func.isRequired,
        onScroll: PropTypes.func.isRequired,
        onSelect: PropTypes.func.isRequired
    };

    constructor(props, context) {
        super(props, context);

        this._handleScroll = this._handleScroll.bind(this);
    }

    render() {
        const props = this.props;

        return (
            <div className="exa-main-view-half-ct">
                <PanelToolbar
                    panelId = {props.panelId}
                    storeId = {props.storeId}
                    showBreadcrumbs = {props.showBreadcrumbs}
                    showBookmarks = {props.showBookmarks}
                    onPanelClose = {props.canClose ? props.onClose : undefined}
                    onPanelAdd = {props.onAdd}
                />
                <div className = {`exa-main-view-half exa-main-view-half-${props.panelId} ${props.isActive ? 'exa-blue-glow' : ''}`}
                    onScroll = {this._handleScroll}
                    onClick = {props.onSelect}>
                    <EditorTree
                        ref = "tree"
                        storeId = {props.storeId}
                        readonly = {props.readonly}
                    />
                </div>
            </div>
        );
    }

    _handleScroll() {
        this.props.onScroll();
    }

    _startResizingMonitor() {
        if (this.__heightMonitor !== undefined) {
            this._stopResizingMonitor();
        }

        let savedHeight = 0;
        const checker = () => {
            const actualHeight = findDOMNode(this.refs.tree).getBoundingClientRect().height;
            if (actualHeight !== savedHeight) {
                this._handleScroll();
                savedHeight = actualHeight;
            }
        };

        this.__heightMonitor = setInterval(checker, CHECK_RESIZE_INTERVAL);
    }

    _stopResizingMonitor() {
        clearInterval(this.__heightMonitor);
        this.__heightMonitor = undefined;
    }

    componentDidMount() {
        this._startResizingMonitor();
    }

    componentWillUnmount() {
        this._stopResizingMonitor();
    }
}

export default EditorHalfView;
