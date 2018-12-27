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

import React, {PropTypes, Component} from 'react';
import ReactDOM from 'react-dom';
import Draggable from 'react-draggable';
import {Button, Glyphicon, Modal, Form, Col, FormGroup, ControlLabel, FormControl} from 'react-bootstrap';

import TheEditor from 'actions/TheEditor.js';
import TheFinder from 'actions/TheFinder.js';
import TheHistory from 'actions/TheHistory.js';

const SearchReplaceDialog = React.createClass({
    displayName: 'SearchReplaceDialog',

    _searchTasks: {},

    propTypes: {
        activeSide: PropTypes.string.isRequired,
        onDialogClose: PropTypes.func.isRequired
    },

    render() {
        const locked = TheHistory.isHistoryBlocked();

        return (
            <Draggable handle=".modal-header">
                <Modal.Dialog dialogClassName='exa-modal-no-lock' >
                    <Modal.Header>
                        <button
                            type="button"
                            className="close"
                            onClick={this.handleCloseDialog}>
                            <span>&times;</span>
                        </button>
                        <Modal.Title>Find & Replace</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <ControlLabel>Search for</ControlLabel>
                        <FormControl ref='searchFor' type='text' placeholder='' className='exa-search-dialog-input' onInput={this.handleSearchInput} />
                        <ControlLabel>Search in</ControlLabel>
                        <FormControl ref='searchIn' type='text' placeholder='' className='exa-search-dialog-input' onInput={this.handleSearchInput} />
                        <ControlLabel>Replace with</ControlLabel>
                        <FormControl ref='replaceWith' type='text' placeholder='' className='exa-search-dialog-input' />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={this.handleSearchForNext} title='Find' disabled={locked} >
                            <Glyphicon glyph='search' /> Find
                        </Button>
                        <Button onClick={this.handleReplace} title='Replace' disabled={locked}>
                            <Glyphicon glyph='pencil' /> Replace
                        </Button>
                        <Button onClick={this.handleReplaceFind} title='Replace & find' disabled={locked}>
                            <Glyphicon glyph='pencil' />
                            {' '}
                            <Glyphicon glyph='search' /> Replace & Find
                        </Button>
                        <Button onClick={this.handleReplaceAll} title='Replace all' disabled={locked}>
                            { locked ? <div className='exa-spinner'></div> : null}
                            {' '}
                            <Glyphicon glyph='pencil' />
                            {' '}
                            <Glyphicon glyph='pencil' /> Replace all
                        </Button>
                    </Modal.Footer>
                </Modal.Dialog>
            </Draggable>
        );
    },

    handleCloseDialog: function() {
        TheFinder.cleanSearchResults(this.props.activeSide);
        this.props.onDialogClose();
    },

    handleSearchInput: function() {
        let side = this.props.activeSide;
        let searchFor = ReactDOM.findDOMNode(this.refs.searchFor).value;
        let searchIn = ReactDOM.findDOMNode(this.refs.searchIn).value.split(',').map(x => x.trim()).filter(x => x.length > 0);
        if (this._searchTasks[side] !== undefined) {
            clearTimeout(this._searchTasks[side]);
        }
        this._searchTasks[side] = setTimeout(
            () => {
                this._searchTasks[side] = undefined;
                TheFinder.find(this.props.activeSide, searchFor, searchIn);
            },
            100);
    },

    handleSearchForNext: function() {
        TheFinder.gotoNextResult(this.props.activeSide);
    },

    handleReplace: function() {
        var path = TheFinder.getCurrentSearchPath(this.props.activeSide);
        if (path == null) {
            return;
        }
        var replaceWith = ReactDOM.findDOMNode(this.refs.replaceWith).value;
        TheEditor.edit(path, replaceWith);
    },

    handleReplaceFind: function() {
        this.handleReplace();
        this.handleSearchForNext();
    },

    handleReplaceAll: function() {
        var searchFor = ReactDOM.findDOMNode(this.refs.searchFor).value;
        var searchIn = ReactDOM.findDOMNode(this.refs.searchIn).value.split(',').map(x => x.trim()).filter(x => x.length > 0);
        var replaceWith = ReactDOM.findDOMNode(this.refs.replaceWith).value;
        const me = this;
        TheHistory.blockHistory();

        TheEditor.replace(searchFor, searchIn, replaceWith, () => {
            TheHistory.unblockHistory();
            me.handleCloseDialog();
        });
    },

    componentDidMount: function() {
        TheHistory.getLock().on('update', this.__doForceUpdate);
    },

    componentWillUnmount: function() {
        TheHistory.getLock().off('update', this.__doForceUpdate);
    },

    __doForceUpdate: function() {
        this.forceUpdate();
    }

});

module.exports = SearchReplaceDialog;
