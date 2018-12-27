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
import React, {Component , PropTypes} from 'react';
import {DropTarget, DragSource} from 'react-dnd';
import TheHelper from 'actions/TheHelper.js';
import ActionNode from './ActionNode.jsx';
import BaseNode from 'view/base/BaseNode.js';
import * as DnD from 'actions/dnd.js';
import {baseContextTypes} from 'contextFactory';

class _ActionsSet extends Component {
    static displayName = 'ActionsSet';

    static propTypes = {
        path: PropTypes.string.isRequired,
        items: PropTypes.array.isRequired,
        title: PropTypes.string.isRequired,
        offset: PropTypes.number.isRequired,
        // dnd
        connectDragSource: PropTypes.func,
        connectDragPreview: PropTypes.func,
        connectDropTarget: PropTypes.func,
        isDragging: PropTypes.bool,

        dndDraggableActions: PropTypes.bool.isRequired,
        dndDroppableActions: PropTypes.bool.isRequired,
        canDrag: PropTypes.bool.isRequired,
        canDrop: PropTypes.bool.isRequired
    };

    static contextTypes = baseContextTypes;

    static defaultProps = {
        connectDragSource: x => x,
        connectDragPreview: x => x,
        connectDropTarget: x => x,
        isDragging: false,
        dndDraggableActions: true,
        dndDroppableActions: true,
        // specify it explicitly in your render() method... getDefaultProps doesn't fill props in react-dnd... bug?
        canDrag: false,
        canDrop: true
    };

    constructor() {
        super();
        this.renderBody = this.renderBody.bind(this);
    }

    hasErrors(startIdx, endIdx) {
        const items = this.props.items;
        for (let i = startIdx; i <= endIdx; i++) {
            if (items[i][c.ERRORS_FIELD].length > 0) {
                return true;
            }
        }
        return false;
    }

    renderBody() {
        const props = this.props;
        const {items, path} = props;

        let nodes = [];
        let idx = 0;
        const len = items.length;

        for (; idx < len; idx++) {
            const action = items[idx];
            const values = action[c.VALUES_FIELD];
            const errors = action[c.ERRORS_FIELD];
            const actionItems = action[c.CHILDREN_FIELD];
            const itemPath = TheHelper.createPath(path, c.CHILDREN_FIELD, props.offset + idx);

            nodes.push(<ActionNode
                path={itemPath}
                values={values}
                errors={errors}
                items={actionItems}
                idx={idx}
                nodeKey={action.key}
                key={action.key}
                dndDraggableActions={props.dndDraggableActions}
                dndDroppableActions={props.dndDroppableActions}
                canDrag={props.dndDraggableActions}
                canDrop={props.dndDroppableActions}
            />);
        }

        return (<div>{nodes}</div>);
    }

    render() {
        const props = this.props;
        const connectDragSource = props.canDrag ? props.connectDragSource : x => x;
        const connectDragPreview = props.canDrag ? props.connectDragPreview : x => x;
        const connectDropTarget = props.canDrop ? props.connectDropTarget : x => x;
        const isDragging = props.isDragging;

        return  connectDragPreview(
            connectDropTarget(
            <div>
            <BaseNode
                nodeCls={`exa-action-node${this.hasErrors() ? ' exa-contain-error' : ''}${isDragging ? ' exa-dragging' : ''}`}
                nodeKey={props.nodeKey}
                headerText={props.title}
                headerIdx={props.idx}
                dndWrapper={connectDragSource}
                body={this.renderBody}
                path={props.path}
                bootstrapCls={this.hasErrors() ? 'danger' : 'default'}
                isServiceNode={true}
                popover={null}
            />
            </div>));

    }
}

export const ActionSet = (
DragSource(DnD.Types.ACTION, DnD.actionSource, DnD.collectSource) (
DropTarget(DnD.Types.ACTION, DnD.actionTarget, DnD.collectTarget) (
    _ActionsSet
)));

export default ActionSet;
