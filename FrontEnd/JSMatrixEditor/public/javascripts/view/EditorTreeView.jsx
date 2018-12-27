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

import * as cx from 'consts';
import React, {PropTypes, Component} from 'react';
import {Button} from 'react-bootstrap';
import TestCaseNode from './nodes/TestCaseNode.js';
import UnexpectedNode from './nodes/UnexpectedNode.js';
import {baseContextTypes} from 'contextFactory.js';

export class EditorTreeView extends Component {
    static displayName = 'EditorTreeView';

    static contextTypes = baseContextTypes;

    static propTypes = {
        storeData: PropTypes.arrayOf(PropTypes.object),
        storeId: PropTypes.string.isRequired,
        readonly: PropTypes.bool.isRequired,
        onAddTcClick: PropTypes.func.isRequired
    };

    _renderLoading() {
        return <div className="exa-spinner"/>;
    }

    render() {
        const testCases = this.props.storeData;

        let content;
        if (testCases == null) {
            content = this._renderLoading();
        } else if (testCases.length === 0 && this.props.readonly !== true) {
            content = <Button onClick={this.props.onAddTcClick}> Add test case </Button>;
        } else if (testCases.length > 0) {
            content = testCases.map((testCase, idx) => {
                if (this.props.storeId !== cx.SCRIPT_RUN_STORE_NAME) {
                    return (<TestCaseNode
                        path = {'' + idx}
                        items = {testCase[cx.CHILDREN_FIELD] || []}
                        values = {testCase[cx.VALUES_FIELD] || {}}
                        errors = {testCase[cx.ERRORS_FIELD] || []}
                        nodeKey = {testCase.key}
                        key = {testCase.key}
                        canDrag = {true}
                        canDrop = {!this.props.readonly}
                    />);
                } else {
                    return (<UnexpectedNode
                        path = {'' + idx}
                        items = {testCase[cx.CHILDREN_FIELD] || []}
                        values = {testCase[cx.VALUES_FIELD] || {}}
                        nodeKey = {testCase.key}
                        key = {testCase.key}
                    />);
                }
            });
        }

        return (<div className='exa-editor-ct' ref='ct'>
            {content}
        </div>);
    }
}

export default EditorTreeView;
