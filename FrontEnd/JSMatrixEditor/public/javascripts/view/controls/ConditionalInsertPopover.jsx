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
import {Button, Glyphicon, ButtonGroup, DropdownButton, MenuItem} from 'react-bootstrap';

import TheAppState from 'state/TheAppState.js';
import {getMainStoreId} from 'state/initialAppState.js';

import TheHelper from 'actions/TheHelper.js';
import TheEditor from 'actions/TheEditor.js';
import {baseContextTypes} from 'contextFactory.js';
import * as c from 'consts';

var ConditionalInsertPopover = React.createClass({
    displayName: 'ConditionalInsertPopover',

    contextTypes: baseContextTypes,

    propTypes: {
        path: React.PropTypes.string.isRequired
    },

    getInitialState() {
        return this.__getState();
    },

    getBookmarks() {
        if (!this.__bookmarks) {
            this.__bookmarks = TheAppState.select('panels', 'dataSources', 'items', this.context.storeId, 'bookmarks');
        }
        return this.__bookmarks;
    },

    componentDidMount() {
        this.getBookmarks().on('update', this.handleBookmarksUpdate);
    },

    componentWillUnmount() {
        this.getBookmarks().off('update', this.handleBookmarksUpdate);
    },

    handleBookmarksUpdate() {
        this.setState(this.__getState());
    },

    handleUpdateCopypaster() {
        this.setState(this.__getState());
    },

    handleBookmarkButton() {
        if (this.state.bm) {
            const bms = this.getBookmarks();
            const idx = bms.get().indexOf(this.props.path);
            bms.splice([idx, 1]);
        } else {
            this.getBookmarks().push(this.props.path);
        }
    },

    __getState() {
        return {
            bm: this.getBookmarks().get().indexOf(this.props.path) >= 0,
        };
    },

    getBeforeButton(path, storeId) {
        const action = TheHelper.getValuesByPath(path, storeId);
        const actionName = action.values['#action'].toLowerCase();

        const disabled = (actionName == 'if');

        return (<DropdownButton
                onSelect={this.handleInsertClick.bind(this, true)}
                bsSize='xsmall'
                pullRight dropup
                style={{'fontWeight': 'bold'}}
                title=''
                disabled={disabled}
            >
            <MenuItem key='ELIF' eventKey='ELIF'>ELIF</MenuItem>
        </DropdownButton>);
    },

    getAfterButton(path, storeId) {
        const action = TheHelper.getValuesByPath(path, storeId);
        const actionName = action.values['#action'].toLowerCase();

        const items = TheHelper.getValuesByPath(TheHelper.parentPath(path), storeId);
        const isLastRow =  TheHelper.pathLastKey(path) == items.length-1;
        const hasElse = _.find(items, (item) => { return item.values['#action'].toLowerCase() == 'else'; });
        const showElse = isLastRow && ! hasElse;


        const disabled = (actionName == 'else');
        return (<DropdownButton
                onSelect={this.handleInsertClick.bind(this, false)}
                bsSize='xsmall'
                pullRight
                title='add'
                disabled={disabled}
            >
            <MenuItem key='ELIF' eventKey='ELIF'>ELIF</MenuItem>
            {showElse ? <MenuItem key='ELSE' eventKey='ELSE'>ELSE</MenuItem> : null}
        </DropdownButton>);
    },

    render: function() {
        const type = TheHelper.getNodeTypeByPath(this.props.path);
        const isTestCase = type !== 'action' && type !== 'action+';
        const copyStatus = this.state.copyStatus;

        const isBookmark = this.state.bm;

        return (<div className="exa-insert-popover" style={{ float: 'right' }}>
            <Button bsSize='xsmall'
                onClick={this.handleBookmarkButton}
                bsStyle={isBookmark ? 'warning' : 'default'}
                className="exa-insert-popover-item exa-btn-bookmark"
                title="Add bookmark">
                <Glyphicon glyph='bookmark'/>
            </Button>

            <ButtonGroup className="exa-insert-popover-item">
                {this.getAfterButton(this.props.path, getMainStoreId())}
                {this.getBeforeButton(this.props.path, getMainStoreId())}
            </ButtonGroup>

            <Button bsSize='xsmall' bsStyle="danger" onClick={this.handleDeleteClick} className="exa-insert-popover-item" title="Remove">
                <Glyphicon glyph='remove' />
            </Button>
        </div>);
    },

    handleInsertClick(before, eventKey, event) {
        const path = this.props.path;
        const index = + TheHelper.pathLastKey(path);
        const parentPath = TheHelper.parentPath(path);
        const insertIndex = index + (before ? 0 : 1);
        const data = TheHelper.getConditionalTemplate(eventKey);

        TheEditor.insert(parentPath, insertIndex, data);
    },

    handleDeleteClick: function() {
        TheEditor.remove(this.props.path);
    }
});

export default ConditionalInsertPopover;
