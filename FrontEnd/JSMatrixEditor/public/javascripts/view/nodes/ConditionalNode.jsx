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
import * as c from 'consts.js';
import {getColorClass, assignNodeKeys} from 'utils.js';
import React, {PropTypes, Component} from 'react';
import {DragSource, DropTarget} from 'react-dnd';
import {Alert} from 'react-bootstrap';

import * as DnD from 'actions/dnd.js';
import TheEditor from 'actions/TheEditor.js';
import TheHelper from 'actions/TheHelper.js';

import {ActionsList, defaultSetSplitter} from './ActionsList.js';

import PropertyList from 'view/propList/PropertyList.js';
import BaseNode from 'view/base/BaseNode.js';
import TestCaseNode from './TestCaseNode.js';
import AddNodeButton from 'view/controls/AddNodeButton.js';

import {baseContextTypes} from 'contextFactory';

class _ConditionalNode extends Component {
    static displayName = 'ConditionalNode';

    static propTypes = {
        path: PropTypes.string.isRequired,
        values: PropTypes.object.isRequired,
        idx: PropTypes.number.isRequired,
        errors: PropTypes.array,
        items: PropTypes.array,
        nodeKey: PropTypes.string.isRequired,
        // dnd
        isDragging: PropTypes.bool.isRequired,
        connectDragSource: PropTypes.func,
        connectDragPreview: PropTypes.func,
        connectDropTarget: PropTypes.func,
        canDrag: PropTypes.bool.isRequired,
        canDrop: PropTypes.bool.isRequired
    };

    static contextTypes = baseContextTypes;

    static defaultProps = {
        connectDragSource: x => x,
        connectDragPreview: x => x,
        connectDropTarget: x => x,
        // specify it explicitly in your render() method... getDefaultProps doesn't fill props in react-dnd... bug?
        canDrag: true,
        canDrop: true
    };

    constructor(props, context) {
        super();
        this.renderBody = this.renderBody.bind(this);
    }

    _getHeaderText(props) {
        const values = props.values;
        return (<span>Condition</span>);
    }

    renderBody() {
        const path = TheHelper.createPath(this.props.path, c.VALUES_FIELD);
        assignNodeKeys(this.props.errors);

        return (<div className="exa-action-body exa-if-block-container">
            {this.getInnerItems()}
        </div>);
    }

    getInnerItems() {
        if (!TheHelper.isIfContainer(this.props.values) && (!TheHelper.isBlockAction(this.props.values[c.ACTION_FIELD]) || (this.props.items && !Array.isArray(this.props.items)))) {
            return null;
        }
        return <ActionsList
            key="actions"
            items={this.props.items}
            path={this.props.path}
            ownerNodeKey={this.props.nodeKey}
            splitter={defaultSetSplitter}
            dndDraggableSets={false}
            dndDroppableSets={false}
            dndDraggableActions={this.props.canDrag}
            dndDroppableActions={this.props.canDrop}
            canDrag={false}
            canDrop={false}
        />
    }

    hasErrors() {
        const ERRORS_FIELD = c.ERRORS_FIELD;
        return this.props[ERRORS_FIELD].some(err => err.critical);
    }

    render() {
        const props = this.props;
        const cx = this.context;
        const connectDragSource = props.canDrag ? props.connectDragSource : x => x;
        const connectDragPreview = props.canDrag ? props.connectDragPreview : x => x;
        const isDragging = props.isDragging;

        const values = props.values;
        let addClass="exa-additional-action ";
        if (values["#action"]) {
            addClass += "exa-additional-type-action-" + values["#action"] + " ";
        }
        if (values["#message_type"]) {
            addClass += "exa-additional-msg-" + values["#message_type"];
        }
        return  connectDragPreview(
                <div className={addClass}>
                <BaseNode
                    nodeCls={`exa-action-node ${this.hasErrors() ? 'exa-contain-error' : ''}  ${isDragging ? 'exa-dragging' : ''}`}
                    nodeKey={props.nodeKey}
                    headerText={this._getHeaderText(props)}
                    headerIdx={'' + props.idx}
                    dndWrapper={connectDragSource}
                    handleIcons = {{}}
                    body={this.renderBody}
                    path={props.path}
                    bootstrapCls={this.hasErrors() ? 'danger' : 'default'}
                    alwaysExpanded={true}
                />
                </div>);
    }
}

export const ConditionalNode = (
DragSource(DnD.Types.ACTION, DnD.actionSource, DnD.collectSource)(
    _ConditionalNode
));

export default ConditionalNode;
