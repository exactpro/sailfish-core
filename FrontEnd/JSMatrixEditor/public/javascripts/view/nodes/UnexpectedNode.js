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
import BaseNode from 'view/base/BaseNode.js';
import ActionsList from './ActionsList.js';
import {baseContextTypes} from 'contextFactory';

var markedSetsSplitter = function(actions) {
    var result = [];
    var currentSet = [];
    var setId = actions.length > 0 ? actions[0].setId : undefined;

    var getTitle = (action) => {
        let messageName = action.values['#message_type'] || '';
        let dictionaryName = action.values['#dictionary'] || '';
        return `Message ${messageName}@${dictionaryName}   #${action.setId}`;
    };

    for (var i=0; i<actions.length; i++) {
        var action = actions[i];
        if (action.setId != setId) {
            currentSet.title = getTitle(action);
            result.push(currentSet);
            currentSet = [];
        }
        currentSet.push(action);
    }
    if (setId != undefined) {
        currentSet.title = getTitle(action);
        result.push(currentSet);
    }
    return result;
};

const UnexpectedNode = React.createClass({

    displayName: 'UnexpectedNode',

    propTypes: {
        path: React.PropTypes.string.isRequired,
        values: React.PropTypes.object.isRequired,
        items: React.PropTypes.array.isRequired,
        cls: React.PropTypes.string,
        nodeKey: React.PropTypes.string
    },

    contextTypes: baseContextTypes,

    getDefaultProps: function() {
        return {
            cls: ''
        };
    },

    _getHeaderText: function() {
        return 'Unexpected messages for TestCase: ' + this.props.values['#reference'];
    },

    renderBody: function(ownerBaseNode) {
        const props = this.props;
        var result = [
            <ActionsList
                items={props.items}
                path={props.path}
                ownerNodeKey={props.nodeKey}
                splitter={markedSetsSplitter}
                dndDraggableSets={true}
                dndDroppableSets={false}
                dndDraggableActions={true}
                dndDroppableActions={false}
            />
        ];

        return result;
    },

    render: function() {
        const props = this.props;
        const isDragging = props.isDragging;

        return (<BaseNode
                    path={props.path}
                    nodeKey={props.nodeKey}
                    nodeCls={`${props.cls} ${isDragging ? 'exa-dragging' : ''}`}
                    headerCls='node-header-test-case'
                    bodyCls='node-body-test-case'
                    headerText={this._getHeaderText()}
                    handleIcons={{}}

                    body={this.renderBody}
                    popover={null}

                    dndWrapper={x => x}
                    panelId={this.context.panelId}
                    storeId={this.context.storeId}

        />);
    }
});

export default UnexpectedNode;
