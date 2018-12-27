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

import * as c from 'consts';
import React, {PropTypes, Component} from 'react';
import TheEditor from 'actions/TheEditor.js';
import EditorTreeView from './EditorTreeView.jsx';

import TheMatrixList, {getMainStoreId} from 'state/TheMatrixList.js';
import {baseContextTypes} from 'contextFactory';

export class EditorTreeWrapper extends Component {
    static displayName = 'EditorTreeWrapper';

    static contextTypes = baseContextTypes;

    static propTypes = {
        storeId: PropTypes.string
    };

    constructor(props, cx) {
        super(props, cx);

        this.__cursor = null;

        this.__handleCursorUpdate = this.__handleCursorUpdate.bind(this);
        this.handleAddTestCase = this.handleAddTestCase.bind(this);
    }

    __getState(props) {
        return {
            storeData: this.__cursor && this.__cursor.get(),
            readonly: props.storeId !== getMainStoreId()
        };
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.storeId !== this.props.storeId) {
            this.__subscribeOnCursors(nextProps.storeId);
            this.setState(this.__getState(nextProps));
        }
    }

    __handleCursorUpdate() {
        this.setState(this.__getState(this.props));
    }

    __getStoreCursor(storeId) {
        const baobab = TheMatrixList[storeId];
        return baobab && baobab.select('data');
    }

    __subscribeOnCursors(storeId) {
        this.__unsubscribeFromCursors();
        this.__cursor = this.__getStoreCursor(storeId);
        this.__cursor.on('update', this.__handleCursorUpdate);
    }

    __unsubscribeFromCursors() {
        if (this.__cursor !== null) {
            this.__cursor.off('update', this.__handleCursorUpdate);
            this.__cursor = null;
        }
    }

    componentWillMount() {
        this.__subscribeOnCursors(this.props.storeId);
        this.setState(this.__getState(this.props));
    }

    componentWillUnmount() {
        this.__unsubscribeFromCursors();
    }

    handleAddTestCase() {
        var testCase = {
            [c.VALUES_FIELD]: {},
            [c.CHILDREN_FIELD]: []
        };
        TheEditor.insert('', 0, testCase, this.props.storeId);
    }

    render() {
        return <EditorTreeView
            storeData={this.state.storeData}
            readonly={this.state.readonly}
            onAddTcClick={this.handleAddTestCase}
            storeId={this.props.storeId}
        />;
    }
}

export default EditorTreeWrapper;
