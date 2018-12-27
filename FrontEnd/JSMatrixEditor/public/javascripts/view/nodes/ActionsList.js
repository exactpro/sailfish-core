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
import React, {Component, PropTypes} from 'react';
import {DropTarget} from 'react-dnd';

import ActionsSet from './ActionsSet.jsx';
import ActionNode from './ActionNode.jsx';
import ConditionalNode from './ConditionalNode.jsx';
import AddNodeButton from 'view/controls/AddNodeButton.js';
import TheHelper from 'actions/TheHelper.js';
import * as DnD from 'actions/dnd.js';

import {baseContextTypes} from 'contextFactory';

export const defaultSetSplitter = function(actions) {
    const MAX_LIST_LENGTH = 100;
    const setsCount = Math.ceil((actions.length - 1) / MAX_LIST_LENGTH);
    const result = [];

    if (setsCount > 1) {
        let startIdx, endIdx, i;
        for (i = 0; i < setsCount; i++ ) {
            startIdx = i * MAX_LIST_LENGTH;
            endIdx = startIdx + MAX_LIST_LENGTH;
            const list = actions.slice(startIdx, endIdx);
            //list.title = `Actions ${startIdx} ${endIdx}`
            result.push(list);
        }
    }
    return result;
};

class _ActionsList extends Component {
    static displayName = 'ActionsList';

    static propTypes = {
        items: PropTypes.array.isRequired,
        path: PropTypes.string.isRequired,
        ownerNodeKey: PropTypes.string.isRequired,
        splitter: PropTypes.func.isRequired,

        dndDraggableSets: PropTypes.bool.isRequired,
        dndDroppableSets: PropTypes.bool.isRequired,
        dndDraggableActions: PropTypes.bool.isRequired,
        dndDroppableActions: PropTypes.bool.isRequired
    };

    static contextTypes = baseContextTypes;

    static defaultProps = {
        startIdx: 0,
        itemsPaths: [],
        dndDraggableSets: false,
        dndDroppableSets: true,
        dndDraggableActions: true,
        dndDroppableActions: true
    };

    render() {
        const props = this.props;
        const {items, path, ownerNodeKey} = props;
        let content;

        if (items.length === 0 && !this.context.readonly) {
            content = <AddNodeButton
                path={TheHelper.pathCreate(props.path, c.CHILDREN_FIELD, 0)}
                insertBefore={true}
                title="Add action"
            />;
            if (props.dndDroppableActions) {
                content = this.props.connectDropTarget(<div>{content}</div>);
            }
        } else {
            const actionSets = props.splitter(items);
            if (actionSets.length === 0) {
                content = items.map((action, idx) => {
                    const itemPath = TheHelper.createPath(path, c.CHILDREN_FIELD, idx);
                    const values = action[c.VALUES_FIELD];
                    const errors = action[c.ERRORS_FIELD];
                    const items = action[c.CHILDREN_FIELD];

                    if (TheHelper.isIfContainer(values)) {
                        return (<ConditionalNode
                            path={itemPath}
                            values={values}
                            items={items}
                            errors={errors}
                            idx={idx}
                            nodeKey={action.key}
                            key={action.key}
                            dndDraggableActions={props.dndDraggableActions}
                            dndDroppableActions={props.dndDroppableActions}
                            canDrag={props.dndDraggableActions}
                            canDrop={props.dndDroppableActions}
                        />);
                    } else {
                        return (<ActionNode
                            path={itemPath}
                            values={values}
                            items={items}
                            errors={errors}
                            idx={idx}
                            nodeKey={action.key}
                            key={action.key}
                            dndDraggableActions={props.dndDraggableActions}
                            dndDroppableActions={props.dndDroppableActions}
                            canDrag={props.dndDraggableActions}
                            canDrop={props.dndDroppableActions}
                        />);
                    }
                });
            } else {
                let currIdx = 0;
                content = actionSets.map(aSet => {
                    const startIdx = currIdx;
                    const endIdx = startIdx + aSet.length;
                    const key = `${ownerNodeKey}_${startIdx}-${endIdx}`;
                    const result = (<ActionsSet
                        items={aSet}
                        path={path}
                        title={this.getSetName(aSet, startIdx, endIdx)}
                        offset={currIdx}
                        key={key}
                        nodeKey={key}
                        dndDraggableActions={props.dndDraggableActions}
                        dndDroppableActions={props.dndDroppableActions}
                        canDrag={props.dndDraggableSets}
                        canDrop={props.dndDroppableSets}
                    />);
                    currIdx += aSet.length;
                    return result;
                });
            }
        }
        return (<div className="exa-actions-list-ct">
            {content}
        </div>);
    }

    getSetName(actionSet, startIdx, endIdx) {
        return actionSet.title || `Actions ${startIdx + 1}-${endIdx}`;
    }
}

export const ActionsList = (
DropTarget(DnD.Types.ACTION, DnD.actionTarget, DnD.collectTarget)(
    _ActionsList
));
export default ActionsList;
