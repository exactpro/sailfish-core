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
import EditorHalfView from './EditorHalfView.jsx';
import ErrorPopup from 'view/controls/ErrorPopup.js';
import EditorToolbar from 'view/controls/EditorToolbar.js';
import {polymorphCfg} from 'utils';
import {getMainStoreId} from 'state/TheMatrixList';

export class PanelViewCfg {
    static ctrParams = [
        'panelId',
        'storeId',
        'readonly',
        'canClose',
        'showBreadcrumbs',
        'showBookmarks',
        'isActive'
    ];
    constructor() {
        polymorphCfg(this, arguments, true);
        this.storeId = '' + this.storeId;
        //Object.freeze(this);
    }
}

export class EditorView extends Component {
    static displayName = 'EditorView';

    static propTypes = {
        panels: PropTypes.arrayOf(PropTypes.instanceOf(PanelViewCfg)),
        activePanelId: PropTypes.string,

        onClose: PropTypes.func.isRequired,
        onScroll: PropTypes.func.isRequired,
        onSelect: PropTypes.func.isRequired
    };

    render() {
        const handlerScope = this.props.handlerScope;
        return (
            <div className="exa-sf-ed">
                <ErrorPopup/>
                <EditorToolbar activePanelId={this.props.activePanelId} />
                <div className="exa-main-view-area">
                    {this.props.panels.map(x =>
                        <EditorHalfView {...x}
                            // {/*readonly = {x.storeId === getMainStoreId()}*/}

                            onClose = {this.props.onClose.bind(handlerScope, x.panelId)}
                            onAdd = {this.props.onAdd.bind(handlerScope, x.panelId)}
                            onScroll = {this.props.onScroll.bind(handlerScope, x.panelId)}
                            onSelect = {this.props.onSelect.bind(handlerScope, x.panelId)}
                            key = {x.panelId}
                        />
                    )}
                </div>
            </div>
        );
    }
}

export default EditorView;
