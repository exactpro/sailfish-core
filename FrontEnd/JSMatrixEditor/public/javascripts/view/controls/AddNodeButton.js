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
import React, {Component, PropTypes} from 'react';
import {DropdownButton, MenuItem} from 'react-bootstrap';
import TheHelper from 'actions/TheHelper.js';
import TheEditor from 'actions/TheEditor.js';

const REFERENCE_ACTION = '0*^^%$#';

export class AddNodeButton extends Component {
    static propTypes = {
        ...DropdownButton.propTypes,
        insertBefore: PropTypes.bool,
        isTestCases: PropTypes.bool,
        path: React.PropTypes.string.isRequired
    };

    static defaultProps = {
        insertBefore: false,
        isTestCase: false
    };

    constructor(props) {
        super(props);
    }

    static __menuItems = [[
        <MenuItem key="Sleep" eventKey='Sleep'>sleep</MenuItem>,
        <MenuItem key={REFERENCE_ACTION} eventKey={REFERENCE_ACTION}>reference</MenuItem>,
        <MenuItem key='send' eventKey='send'>send</MenuItem>,
        <MenuItem key='sendDirty' eventKey='sendDirty'>send dirty</MenuItem>,
        <MenuItem key='receive' eventKey='receive'>receive</MenuItem>,
        <MenuItem key='count' eventKey='count'>count</MenuItem>,
        <MenuItem key='SetStatic' eventKey='SetStatic'>set static</MenuItem>,
        <MenuItem key='GetCheckPoint' eventKey='GetCheckPoint'>checkpoint</MenuItem>,
        <MenuItem key='GetAdminCheckPoint' eventKey='GetAdminCheckPoint'>admin checkpoint</MenuItem>
    ],[
        <MenuItem key='TestCase' eventKey='Test case start'>TestCase</MenuItem>,
        <MenuItem key='Global Block' eventKey='Global Block start'>Global Block</MenuItem>,
        <MenuItem key='BeforeTCBlock' eventKey='Before Test Case Block start'>BeforeTCBlock</MenuItem>,
        <MenuItem key='AfterTCBlock' eventKey='After Test Case Block start'>AfterTCBlock</MenuItem>,
        <MenuItem key='Block' eventKey='Block start'>Block</MenuItem>,
        <MenuItem key='FirstBlock' eventKey='First Block start'>FirstBlock</MenuItem>,
        <MenuItem key='LastBlock' eventKey='Last Block start'>LastBlock</MenuItem>
    ]];

    getMenuItems(isTestCase) {
        return this.constructor.__menuItems[+ isTestCase];
    }

    render() {
        const {isTestCase, insertBefore, path, ...other} = this.props;
        return (<DropdownButton {...other} onSelect={this.handleInsertClick.bind(this, insertBefore)}>
            {this.getMenuItems(isTestCase)}
        </DropdownButton>);
    }

    handleInsertClick(before, eventKey, event) {
        const path = this.props.path;
        const index = + TheHelper.pathLastKey(path);
        const parentPath = TheHelper.parentPath(path);
        const insertIndex = index + (before ? 0 : 1);
        const data = eventKey === REFERENCE_ACTION ?  TheHelper.getActionTemplate():  TheHelper.getActionTemplate(eventKey);

        TheEditor.insert(parentPath, insertIndex, data);
    }
}

export default AddNodeButton;
