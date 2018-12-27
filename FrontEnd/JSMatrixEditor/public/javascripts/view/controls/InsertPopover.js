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
import {Button, Glyphicon, ButtonGroup} from 'react-bootstrap';

import TheAppState from 'state/TheAppState.js';
import AddNodeButton from './AddNodeButton.js';
import TheHelper from 'actions/TheHelper.js';
import TheEditor from 'actions/TheEditor.js';
import {baseContextTypes} from 'contextFactory.js';
import * as c from 'consts';

// Node can have only one value from this list:
const IS_SOURCE = 1;
const CAN_BE_SOURCE = 2;
const CAN_BE_DESTINATION = 3;
const CAN_NOT_BE_DESTINATION = 4;
const CAN_NOT_BE_SOURCE = 5;

var InsertPopover = React.createClass({
    displayName: 'InsertPopover',

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

    getCopyCursor() {
        if (!this.__copyCursor) {
            this.__copyCursor = TheAppState.select('panels', 'dataSources', 'items', this.context.storeId, 'copyFields');
        }
        return this.__copyCursor;
    },

    componentDidMount() {
        this.getBookmarks().on('update', this.handleBookmarksUpdate);
        this.getCopyCursor().on('update', this.handleUpdateCopypaster);
    },

    componentWillUnmount() {
        this.getBookmarks().off('update', this.handleBookmarksUpdate);
        this.getCopyCursor().off('update', this.handleUpdateCopypaster);
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

    handleCopyResource() {
        if (this.state.copyStatus === IS_SOURCE) {
            this.getCopyCursor().unset('sourcePath');
        } else if (this.state.copyStatus === CAN_BE_DESTINATION) {
            this.getCopyCursor().select('destinationPath').set(this.props.path);
            TheEditor.copyFieldValues(this.context.storeId);
        } else if (this.state.copyStatus === CAN_BE_SOURCE) {
            this.getCopyCursor().select('sourcePath').set(this.props.path);
        }
    },

    __getCopyStatus() {
        const sourcePath = this.getCopyCursor().get('sourcePath');
        var destinationType = TheHelper.getNodeTypeByPath(this.props.path);
        const values = TheHelper.getValuesByPath(this.props.path, this.context.storeId).values;
        const action = values[c.ACTION_FIELD];
        const cantUseInCopy = destinationType === 'testCase' || TheHelper.isBlockAction(action) || TheHelper.isIfContainer(values);
        if (this.props.path === sourcePath) {
            return IS_SOURCE;       //source is set, and it's me
        } else if (!sourcePath) {
            return (cantUseInCopy) ? CAN_NOT_BE_SOURCE : CAN_BE_SOURCE;     //source not set
        } else {
            return (cantUseInCopy) ? CAN_NOT_BE_DESTINATION : CAN_BE_DESTINATION;
        }
    },

    __getState() {
        return {
            bm: this.getBookmarks().get().indexOf(this.props.path) >= 0,
            copyStatus: this.__getCopyStatus()
        };
    },

    render: function() {
        const type = TheHelper.getNodeTypeByPath(this.props.path);
        const isTestCase = type !== 'action' && type !== 'action+';
        const copyStatus = this.state.copyStatus;

        const isBookmark = this.state.bm;
        const isSetAsSource = copyStatus === IS_SOURCE;
        const maybeSetAsDestination = copyStatus === CAN_BE_DESTINATION;
        const hideCopyButton = this.props.hideCopyButton && (copyStatus === CAN_NOT_BE_DESTINATION || copyStatus === CAN_NOT_BE_SOURCE);

        return (<div className="exa-insert-popover" style={{
            float: 'right'
        }}>

            {hideCopyButton ? null : <Button bsSize='xsmall'
                onClick={this.handleCopyResource}
                bsStyle={maybeSetAsDestination || isSetAsSource ? 'info' : "default"}
                className="exa-insert-popover-item exa-btn-bookmark"
                title={maybeSetAsDestination ? "Copy here" : (isSetAsSource) ? "Cancel": "Copy similar fields"}>
                <Glyphicon glyph={maybeSetAsDestination ? 'ok-circle' : 'screenshot'}/>
            </Button>}

            <Button bsSize='xsmall'
                onClick={this.handleBookmarkButton}
                bsStyle={isBookmark ? 'warning' : 'default'}
                className="exa-insert-popover-item exa-btn-bookmark"
                title="Add bookmark">
                <Glyphicon glyph='bookmark'/>
            </Button>

            <ButtonGroup className="exa-insert-popover-item">
                <AddNodeButton bsSize='xsmall' pullRight
                    style={{'fontWeight': 'bold'}}
                    title='add'
                    isTestCase={isTestCase}
                    path={this.props.path}
                />
                <AddNodeButton bsSize='xsmall' pullRight
                    dropup
                    insertBefore={true}
                    title=''
                    isTestCase={isTestCase}
                    path={this.props.path}
                />
            </ButtonGroup>

            <ButtonGroup className="exa-insert-popover-item">
                <Button bsSize='xsmall'
                    onClick={this.handleCopyClick.bind(this, false)}
                >
                    <Glyphicon glyph='duplicate'/>
                    <span className='caret' style={{'marginLeft': '3px'}}/>
                </Button>
                <Button bsSize='xsmall' className='dropup'
                    onClick={this.handleCopyClick.bind(this, true)}
                >
                    <span className='caret'/>
                </Button>
            </ButtonGroup>

            <Button bsSize='xsmall' bsStyle="danger" onClick={this.handleDeleteClick} className="exa-insert-popover-item" title="Remove">
                <Glyphicon glyph='remove' />
            </Button>
        </div>);
    },

    handleCopyClick: function(before, action) {
        const path = this.props.path;
        const key = TheHelper.pathLastKey(path);
        const parentPath = TheHelper.parentPath(path);
        const data = Object.assign({}, TheHelper.getValuesByPath(path, this.context.storeId));
        delete data['key'];
        var position = (+key) + (before ? 0 : 1);

        TheEditor.insert(parentPath,  position, data);
    },

    handleDeleteClick: function() {
        TheEditor.remove(this.props.path);
    }
});

export default InsertPopover;
