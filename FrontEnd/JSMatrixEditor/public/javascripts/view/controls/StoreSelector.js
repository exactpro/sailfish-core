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
import {DropdownButton, MenuItem} from 'react-bootstrap';

import TheAppState from 'state/TheAppState.js';
import TheStoreManager from 'actions/TheStoreManager.js';
import {baseContextTypes} from 'contextFactory';

export class StoreSelector extends Component {
    static displayName = 'StoreSelector';

    static contextTypes = baseContextTypes;

    static propTypes = {
        panelId: PropTypes.string.isRequired
    };

    constructor(props, context) {
        super(props, context);

        this.__handleSourceSelector = this.__handleSourceSelector.bind(this);
        this.__handleCursorUpdate = this.__handleCursorUpdate.bind(this);
        this.__cursor = this.__getCursor();
        this.state = this.__getState(props);
    }

    __getCursor() {
        return TheAppState.select('panels');
    }

    __getDataSourcesCfgSet() {
        return this.__cursor.get('dataSources', 'items');
    }

    __getPanelsCfgSet() {
        return this.__cursor.get('items');
    }

    __getState(props) {
        const loadedSources = [];
        const availableSources = [];
        const sources = this.__getDataSourcesCfgSet();
        const panelId = props.panelId;
        const panel = this.__getPanelsCfgSet()[panelId];
        if (!panel) {       //panel will unmount
            return;
        }
        const currentStoreId = panel.storeId;
        const currentStore = sources[currentStoreId];

        let storeId, store;
        for (storeId in sources) {
            //if (storeId !== currentStoreId) {
            store = sources[storeId];
            if (store.loaded) {
                loadedSources.push(store);
            } else {
                availableSources.push(store);
            }
            //}
        }

        return {
            sources: sources,
            storeId: currentStoreId,
            title: currentStore.title || '',
            loadedSources: loadedSources,
            availableSources: availableSources
        };
    }

    componentWillReceiveProps(newProps) {
        this.setState(this.__getState(newProps));
    }

    render() {
        return (<DropdownButton bsSize="xsmall" title={this.state.title}
            onSelect={this.__handleSourceSelector}
        >
            {this.state.loadedSources.map(source =>
                <MenuItem eventKey={source.storeId} key={'l-' + source.storeId}
                    disabled={this.state.storeId === source.storeId}
                >
                    {source.title}
                </MenuItem>
            )}
            {this.state.availableSources.length ?  <MenuItem divider/> : undefined}
            {this.state.availableSources.map(source =>
                <MenuItem eventKey={source.storeId} key={'a-' + source.storeId}>
                    {source.title}
                </MenuItem>
            )}
        </DropdownButton>);
    }

    __handleSourceSelector(storeId, event) {
        TheStoreManager.switchDataSource(this.props.panelId, storeId);
    }

    __handleCursorUpdate() {

        this.setState(this.__getState(this.props));
    }

    componentDidMount() {
        this.__cursor.on('update', this.__handleCursorUpdate);
    }

    componentWillUnmount() {
        this.__cursor.off('update', this.__handleCursorUpdate);
    }
}

export default StoreSelector;
