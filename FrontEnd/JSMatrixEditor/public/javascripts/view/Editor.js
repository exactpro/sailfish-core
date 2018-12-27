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
import {DragDropContext} from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import {EditorView, PanelViewCfg} from './EditorView.jsx';
import TheScroller from 'actions/TheScroller.js';
import TheEditor from 'actions/TheEditor.js';
import TheAppState from 'state/TheAppState.js';

class Editor extends Component {
    static displayName = 'Editor';

    constructor(props, ctx) {
        super(props, ctx);
        this.__cursor = TheAppState.select('panels');
        this.__handleCursorUpdate = this.__handleCursorUpdate.bind(this);
        this.state = this.__getState(props);
    }

    componentWillMount() {
        this.__cursor.on('update', this.__handleCursorUpdate);
    }

    componentWillUnmount() {
        this.__cursor.off('update', this.__handleCursorUpdate);
    }

    __handleCursorUpdate() {
        this.setState(this.__getState(this.props));
    }

    __getData() {
        return this.__cursor.get();
    }

    __getPanelsViewConfigs() {
        const result = [];
        const data = this.__getData();
        const order = data.order;
        const panels = data.items;
        const active = data.active;
        const orderLen = order.length;
        let panelId, panel, i;
        for (i = 0; i < orderLen; i++) {
            panelId = order[i];
            panel = panels[panelId];
            result.push(new PanelViewCfg(
                panelId,
                panel.storeId,
                data.dataSources.items[panel.storeId].readonly,
                panel.canClose,
                panel.showBreadcrumbs,
                panel.showBookmarks,
                panelId === active
            ));
        }
        return result;
    }

    __getActivePanelId() {
        return this.__cursor.get('active');
    }

    __getState(props) {
        return {
            panels: this.__getPanelsViewConfigs(),
            activePanelId: this.__getActivePanelId()
        };
    }

    handleClose(panelId) {
        TheEditor.closePanel(panelId);
    }

    handleAdd(panelId) {
        TheEditor.addPanel(panelId);
    }

    handleScroll(panelId) {
        TheScroller.handleScroll(panelId);
    }

    handleSelect(panelId) {
        TheEditor.checkoutPanel(panelId);
    }

    render() {
        return (<EditorView ref='view'
            {...this.state}
            handlerScope = {this}
            onClose = {this.handleClose}
            onScroll = {this.handleScroll}
            onSelect = {this.handleSelect}
            onAdd = {this.handleAdd}
        />);
    }
}

const dndWrapped = DragDropContext(HTML5Backend)(Editor);

export default dndWrapped;
export {dndWrapped as Editor};
