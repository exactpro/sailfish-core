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
import TheEditor from 'actions/TheEditor.js';
import {TheHelper} from 'actions/TheHelper.js';

export const Types = {
    ACTION: 'action',
    BLOCK: 'block'
};

var originalDragged = {};

var waitingForRender = false;


var onNextTick = function(cb) {
    setImmediate(cb);
}

export const actionSource = {
    canDrag(props, monitor) {
        return props.canDrag;
    },
    beginDrag(props, monitor, component) {
        var count = 1;
        if (typeof props.endIdx === 'number' && typeof props.startIdx === 'number') {
            count = props.endIdx - props.startIdx + 1;
        }
        const originalContext = (
            // we don't wrap Block>AddAction Button's class to DnD wrappers, so here we will get component itself
            component.getDecoratedComponentInstance ?
                component.getDecoratedComponentInstance().context :
                component.context
        );
        // save original context & path to use it in server-request
        // It will be 'from'
        originalDragged = {
            nodeKey : props.nodeKey,
            context: originalContext,
            path: props.path, // aka original path
            count: count
        };
        // copy this object. It will be edited in DnD handlers
        // It will be 'to'
        return Object.assign({}, originalDragged);
    },
    endDrag(props, monitor, component) {
        const didDrop = monitor.didDrop();

        if (didDrop) {
            // Element dropped. Send request to server:
            const from = originalDragged;
            const to = monitor.getItem();

            TheEditor.move(
                from.context.currentStore,
                from.path,
                from.count,
                to.context.currentStore,
                to.path,
                true, /* send request to server */
                false /* don't move node. it already in correct place */);
        } else {
            // Dropped out of target. Rollback
            const from = monitor.getItem();
            const to = originalDragged;
            // rollback
            TheEditor.move(
                from.context.currentStore,
                from.path,
                from.count,
                to.context.currentStore,
                to.path,
                false /* don't send to server */,
                true /* just move */);
        }
        originalDragged = {};
    },
    isDragging(props, monitor) {
        return props.nodeKey === monitor.getItem().nodeKey;
    }
};

export function collectSource(connect, monitor) {
    return {
        connectDragSource: connect.dragSource(),
        connectDragPreview: connect.dragPreview(),
        isDragging: monitor.isDragging()
    };
}

export const actionTarget = {
    drop(props, monitor, component) {
        let result = {
            nodeKey : props.nodeKey,
            context: component.context,
            path: props.path // aka original path
        };
        return result;
    },
    canDrop(props, monitor) {
        return props.canDrop;
    },
    hover(props, monitor, component) {
        if (!monitor.isOver({ shallow: true })) {
            return;
        }
        if (!monitor.canDrop()) {
            return;
        }

        if (waitingForRender) {
            return;
        }

        const draggedItem = monitor.getItem();
        const overItem = props;

        const overItemCtx = (
            // we don't wrap Block>AddAction Button's class to DnD wrappers, so here we will get component itself
            component.getDecoratedComponentInstance ?
                component.getDecoratedComponentInstance().context :
                component.context
        );

        var fromStore = draggedItem.context.currentStore;
        var toStore = overItemCtx.currentStore;

        if (draggedItem.nodeKey !== overItem.nodeKey) {
            // move node in store to show it on GUI
            var newPath = TheEditor.move(
                fromStore,
                draggedItem.path,
                draggedItem.count,
                toStore,
                overItem.path,
                false /* don't send to server */,
                true /* just move */);

            draggedItem.path = newPath;
            draggedItem.context = overItemCtx;

            // we need to wait for redrawing in order to get proper paths on next hover()
            waitingForRender = true;
            onNextTick(() => {
                waitingForRender = false;
            })
        }
    }
};

export function collectTarget(connect, monitor) {
    return {
        connectDropTarget: connect.dropTarget(),
        isOver: monitor.isOver()
    };
}

export const blockSource = actionSource;

export const blockTarget = actionTarget;
