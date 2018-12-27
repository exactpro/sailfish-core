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
import {Button} from 'react-bootstrap';

import StoreSelector from './StoreSelector.js';
import Breadcrumbs from './Breadcrumbs.js';
import Bookmarks from './Bookmarks.js';
import TheAppState from 'state/TheAppState.js';
import {baseContextTypes} from 'contextFactory';


export class PanelToolbar extends Component {
    static displayName = 'PanelToolbar(HalfToolbar)';

    static contextTypes = baseContextTypes;

    static propTypes = {
        panelId: React.PropTypes.string.isRequired,
        storeId: React.PropTypes.string.isRequired,
        showBreadcrumbs: React.PropTypes.bool.isRequired,
        showBookmarks: React.PropTypes.bool.isRequired,
        onPanelClose: React.PropTypes.func,
        onPanelAdd: React.PropTypes.func
    };

    render() {
        const props = this.props;
        const activePanelId = TheAppState.get('panels', 'active');
        const isActivePanel = activePanelId===props.panelId;
        return (
            <div className='exa-over-ct'>
                <StoreSelector panelId={props.panelId}/>

                {props.showBreadcrumbs &&
                    <Breadcrumbs panelId={props.panelId} storeId={props.storeId}/>
                }

                {props.onPanelClose &&
                    <Button bsStyle='danger' bsSize="xsmall" onClick={props.onPanelClose} className='exa-btn-close-panel' disabled={isActivePanel} >Close</Button>
                }

                {props.onPanelAdd &&
                    <Button bsStyle='primary' bsSize="xsmall" onClick={props.onPanelAdd} className='exa-btn-close-panel'>Add</Button>
                }

                {props.showBookmarks  &&
                    <Bookmarks panelId={props.panelId} storeId={props.storeId}/>
                }

            </div>
        );
    }
}

export default PanelToolbar;
