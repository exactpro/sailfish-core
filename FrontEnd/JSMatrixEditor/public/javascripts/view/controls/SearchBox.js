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
import _ from 'lodash';
import React from 'react';
import {ButtonGroup, Button, Glyphicon, FormGroup, FormControl, InputGroup} from 'react-bootstrap';

import SearchReplaceDialog from '../dialogs/SearchReplaceDialog.js';
import TheFinder from 'actions/TheFinder.js';
import TheAppState from 'state/TheAppState.js';

export const SearchBox = React.createClass({
    displayName: 'SearchBox',

    _searchTasks: {},

    getInitialState() {
        return {
            idx: -1,
            results: [],
            text: '',
            showDialog: false
        };
    },

    propTypes: {
        activePanelId: React.PropTypes.string.isRequired
    },

    render: function() {
        const state = this.state;
        const idx = state.idx;
        const len = state.results.length;

        let dialog = null;

        if (this.state.showDialog) {
            dialog = (<SearchReplaceDialog activeSide={this.props.activePanelId} onDialogClose={this.onCloseReplaceDialog} />);
        }

        return (
            <span>
            <FormGroup className="eps-search-container">
                <InputGroup>
                    <FormControl ref='search' type='text' label='' placeholder='Search' onInput={this.handleSearchInput}
                        className='exa-search-box form-inline exa-form-inline'
                    />
                    <InputGroup.Addon>{(idx >= 0) ? `${idx+1} of ${len}` : '0 of 0'}</InputGroup.Addon>
                    <InputGroup.Button>
                        <Button onClick={this.handleFindNext} title="Next">
                            <Glyphicon glyph='chevron-down' />
                        </Button>
                        <Button onClick={this.handleFindPrev} title="Previous">
                            <Glyphicon glyph='chevron-up' />
                        </Button>
                        <Button onClick={this.handleShowReplace} title="Find&Replace">
                            <Glyphicon glyph='pencil' />
                        </Button>
                    </InputGroup.Button>
                </InputGroup>
            </FormGroup>
            {dialog}
            </span>
        );
    },

    handleFindNext: function() {
        TheFinder.gotoNextResult(this.props.activePanelId);
    },

    handleFindPrev: function() {
        TheFinder.gotoNextResult(this.props.activePanelId, true);
    },

    handleSearchInput: function(event) {
        let side = this.props.activePanelId;
        let searchFor = event.target.value;
        if (this._searchTasks[side] !== undefined) {
            clearTimeout(this._searchTasks[side]);
        }
        if (searchFor != '') {
            this._searchTasks[side] = setTimeout(
                () => {
                    this._searchTasks[side] = undefined;
                    TheFinder.find(this.props.activePanelId, searchFor, []);
                },
                100);
        } else {
            TheFinder.cleanSearchResults(this.props.activePanelId);
        }
    },

    handleShowReplace: function() {
        this.setState({
            showDialog: true
        });
    },

    onCloseReplaceDialog: function() {
        this.setState({
            showDialog: false
        });
    },

    componentWillMount: function() {
        TheAppState.select('node').on('update', this.handleUpdateState);
    },

    componentWillUnmount: function() {
        TheAppState.select('node').off('update', this.handleUpdateState);
    },

    _updateState: function(cursorData) {
        const state = this.state;
        const curr = state.results[state.idx];

        if (curr !== cursorData.path
            || cursorData.text !== state.text
            || !_.isEqual(cursorData.results, state.results))
        {
            this.setState({
                idx: cursorData.results.indexOf(cursorData.path),
                results: cursorData.results,
                text: cursorData.text
            });
        }
    },

    handleUpdateState: function(event) {
        this._updateState(event.target.select(this.props.activePanelId, 'search').get());
    }
});


export default SearchBox;
