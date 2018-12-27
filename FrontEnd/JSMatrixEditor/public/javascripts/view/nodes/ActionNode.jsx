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
import InsertPopover from 'view/controls/InsertPopover.js';
import ConditionalInsertPopover from 'view/controls/ConditionalInsertPopover.jsx';


import {baseContextTypes} from 'contextFactory';

class _ActionNode extends Component {
    static displayName = 'ActionNode';

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
        this.handleExecuteIcon = this.handleExecuteIcon.bind(this);
        this.handleContinueOnFailedIcon = this.handleContinueOnFailedIcon.bind(this);
        this.handleFailUnexpectedIcon = this.handleFailUnexpectedIcon.bind(this);
        this.renderBody = this.renderBody.bind(this);
    }

    _getHeaderText(props) {
        const values = props.values;
        let id = values['#id'] ? ("#" + this.props.values['#id']) : null;
        let reference = values['#reference'] ? (<span title="#reference">
            <span>{(values['#id'] ? " " : "")}</span>
            <span className="exa-action-header-reference">{"[" + this.props.values['#reference'] + "]"}</span>
        </span>) : null;
        let action = values['#action'] ? (<span title="#action">
                <span>{((id || reference) ? " ": "")}</span>
                <span className="exa-action-header-action">{values["#action"]}</span>
        </span>) : null;
        let condition = values['#condition'] ? (<span title="#condition">
                <span>{((id || reference || action) ? " ": "")}</span>
                <span className="exa-action-header-condition">{"(" +values["#condition"] + ")"}</span>
        </span>) : null;
        let msg_type = values['#message_type'] ? (<span title="#message_type">
                <span>{((id || reference || action || condition) ? " -> " : "")}</span>
                <span className={"exa-action-header-msgtype " + getColorClass(values["#message_type"])}>{values["#message_type"]}</span>
        </span>) : null;
        let description = values['#description'] ? (<span title="#description">
            <span>{((id || reference || action || condition || msg_type) ? " : " : "#desc : ")}</span>
            <span className="exa-action-header-description">{values["#description"]}</span>
        </span>) : null;

        return (<span title="#<id> [<reference>] <action> -> <message_type> : <description>">
            <span className="exa-action-header-id" title="#id">{id}</span>
            {reference}
            {action}
            {condition}
            {msg_type}
            {description}
        </span>);
    }

    renderBody() {
        const path = TheHelper.createPath(this.props.path, c.VALUES_FIELD);

        assignNodeKeys(this.props.errors);

        return (<div className="exa-action-body">
            <PropertyList
                values = {this.props.values}
                path = {path}
            />

            {this.getInlineBlock()}
            {this.getInnerItems()}

            <div className="exa-errors">
                    {this.props.errors.map((error) => <Alert key={error.key} bsStyle={error.critical ? 'danger' : 'warning'} bsSize='xsmall'>{error.message}</Alert>)}
            </div>
        </div>);
    }

    getInnerItems() {
        if ((!TheHelper.isBlockAction(this.props.values[c.ACTION_FIELD]) || (this.props.items && !Array.isArray(this.props.items)))) {
            return null;
        }
        return <ActionsList
            key="actions"
            items={this.props.items}
            path={this.props.path}
            ownerNodeKey={this.props.nodeKey}
            splitter={defaultSetSplitter}
            dndDraggableSets={this.props.canDrag}
            dndDroppableSets={this.props.canDrop}
            dndDraggableActions={this.props.canDrag}
            dndDroppableActions={this.props.canDrop}
            canDrag={this.props.canDrag}
            canDrop={this.props.canDrop}
        />
    }

    getInlineBlock() {
        const path = TheHelper.findIncludeBlockPath(this.props.values, this.context.storeId);
        if (typeof path !== 'string') {
            return null;
        }
        //FIXME move to helper
        const testCase = this.context.currentStore.select('data').select(path).get();
        //..
        return <TestCaseNode
            path={path}
            items={testCase[c.CHILDREN_FIELD] || []}
            values={testCase[c.VALUES_FIELD] || {}}
            errors={testCase[c.ERRORS_FIELD] || []}
            nodeKey={testCase.key}
            key={testCase.key}
            canDrag={false}
            canDrop={true}
            isServiceNode={true}
        />;
    }

    renderPopover() {
        const isConditional = TheHelper.isConditionalAction(this.props.values);
        if (isConditional) {
            return <ConditionalInsertPopover path={this.props.path} />;
        } else {
            return <InsertPopover path={this.props.path} />;
        }
    }

    hasErrors() {
        const ERRORS_FIELD = c.ERRORS_FIELD;
        return this.props[ERRORS_FIELD].some(err => err.critical);
    }

    render() {
        const props = this.props;
        const cx = this.context;
        const values = props.values;

        const isConditional = TheHelper.isConditionalAction(values);

        const connectDragSource = (props.canDrag && !isConditional) ? props.connectDragSource : x => x;
        const connectDragPreview = (props.canDrag && !isConditional) ? props.connectDragPreview : x => x;
        const connectDropTarget = (props.canDrop && !isConditional) ? props.connectDropTarget : x => x;
        const isDragging = props.isDragging;


        let addClass="exa-additional-action ";
        if (values["#action"]) {
            addClass += "exa-additional-type-action-" + values["#action"] + " ";
        }
        if (values["#message_type"]) {
            addClass += "exa-additional-msg-" + values["#message_type"];
        }

        return  connectDragPreview(
                connectDropTarget(
                <div className={addClass}>
                <BaseNode
                    nodeCls={`exa-action-node ${this.hasErrors() ? 'exa-contain-error' : ''}  ${isDragging ? 'exa-dragging' : ''}`}

                    nodeKey={props.nodeKey}
                    headerText={this._getHeaderText(props)}
                    headerIdx={'' + props.idx}
                    dndWrapper={connectDragSource}
                    handleIcons = {{
                        'execute': { handler: this.handleExecuteIcon, hint: 'Execute' },
                        'unstoppable': { handler : this.handleContinueOnFailedIcon, hint: 'Continue on failed' },
                        'filterstop': { handler: this.handleFailUnexpectedIcon, hint: 'Fail unexpected' }
                    }}
                    body={this.renderBody}
                    path={props.path}
                    bootstrapCls={this.hasErrors() ? 'danger' : 'default'}
                    popover={this.renderPopover()}
                />
                </div>));
    }

    handleExecuteIcon(event) {
        if (arguments.length === 0) {
            return (this.props.values['#execute'] || 'y').toLowerCase() !== 'n';
        }

        const path = TheHelper.createPath(this.props.path, 'values', '#execute');
        const value = event.target.checked ? 'y' : 'n';
        TheEditor.edit(path, value);
    }

    handleContinueOnFailedIcon(event) {
        if (arguments.length === 0) {
            return (this.props.values['#continue_on_failed'] || 'n').toLowerCase() === 'y';
        }

        const path = TheHelper.createPath(this.props.path, 'values', '#continue_on_failed');
        const value = event.target.checked ? 'y' : 'n';
        TheEditor.edit(path, value);
    }

    handleFailUnexpectedIcon(event) {
        if (arguments.length === 0) {
            return (this.props.values['#fail_unexpected'] || 'n').toLowerCase() === 'y';
        }

        const path = TheHelper.createPath(this.props.path, 'values', '#fail_unexpected');
        const value = event.target.checked ? 'y' : 'n';
        TheEditor.edit(path, value);
    }
}

export const ActionNode = (
DragSource(DnD.Types.ACTION, DnD.actionSource, DnD.collectSource)(
DropTarget(DnD.Types.ACTION, DnD.actionTarget, DnD.collectTarget)(
    _ActionNode
)));

export default ActionNode;
