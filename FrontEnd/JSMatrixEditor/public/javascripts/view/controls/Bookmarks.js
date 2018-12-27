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

import React, {Component} from 'react';
import {DropdownButton, MenuItem} from 'react-bootstrap';

import TheAppState from 'state/TheAppState.js';
import TheMatrixList from 'state/TheMatrixList.js';
import TheRender from 'actions/TheRender.js';
import {baseContextTypes} from 'contextFactory';

function cursorData(cursor) {
    return cursor && cursor.get();
}

export class Bookmarks extends Component{
    static displayName = 'Bookmarks';

    static contextTypes = baseContextTypes;

    static propTypes = {
        panelId: React.PropTypes.string.isRequired,
        storeId: React.PropTypes.string.isRequired
    };

    constructor(props, context) {
        super(props, context);

        this.__bookmarkCursor = null;
        this.__storeDataCursor = null;

        this.__handleUpdate = this.__handleUpdate.bind(this);
        this.handleSelect = this.handleSelect.bind(this);

        this.state = this.__getState(props);
    }


    __subscribeOnCursors(panelId, storeId) {
        this.__unsubscribeFromCursors();
        this.__bookmarkCursor = TheAppState.select('panels', 'dataSources', 'items', storeId, 'bookmarks');
        this.__storeDataCursor = TheMatrixList[storeId] &&  TheMatrixList[storeId].select('data');
        this.__bookmarkCursor.on('update', this.__handleUpdate);
        this.__storeDataCursor && this.__storeDataCursor.on('update', this.__handleUpdate);

    }

    __unsubscribeFromCursors() {
        if (this.__bookmarkCursor !== null) {
            this.__bookmarkCursor.off('update', this.__handleUpdate);
            this.__bookmarkCursor = null;
        }
        if (this.__storeDataCursor !== null) {
            this.__storeDataCursor.off('update', this.__handleUpdate);
            this.__storeDataCursor = null;
        }
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.storeId !== this.props.storeId || nextProps.panelId !== this.props.panelId) {
            this.__subscribeOnCursors(nextProps.panelId, nextProps.storeId);
            this.__handleUpdate(nextProps);
        }
    }

    componentDidMount() {
        this.__subscribeOnCursors(this.props.panelId, this.props.storeId);
    }

    componentWillUnmount() {
        this.__unsubscribeFromCursors();
    }

    __handleUpdate() {
        this.setState(this.__getState(this.props));
    }

    __getState(props) {
        const result = {
            items: []
        };
        const tcs = cursorData(this.__storeDataCursor);
        if (tcs != null) {
            const bms = cursorData(this.__bookmarkCursor);
            result.items = bms.reduce((acc, x) => {
                acc[x] = TheRender.prettyPrintPath(tcs, x);
                return acc;
            }, {});
        }
        return result;
    }

    handleSelect(path, event) {
        //TODO remove direct writing to store
        TheAppState.set(['node', this.props.panelId, 'scrollToPath'], path);
    }

    render() {
        const items = this.state.items;
        const keys = Object.keys(items);

        return (
            <div className='exa-bookmarks'>
                <DropdownButton pullRight bsSize="xsmall" title='Bookmarks' disabled={keys.length === 0}
                    onSelect={this.handleSelect}
                >
                {keys.map(x =>
                    <MenuItem eventKey={x}>
                        {items[x]}
                    </MenuItem>)}
                </DropdownButton>
            </div>
        );
    }
}

export default Bookmarks;
