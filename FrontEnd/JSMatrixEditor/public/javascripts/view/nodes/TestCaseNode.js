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
import {Alert} from 'react-bootstrap';
import {DragSource, DropTarget} from 'react-dnd';
import BaseNode from 'view/base/BaseNode.js';
import PropertyList from '../propList/PropertyList.js';
import {ActionsList, defaultSetSplitter} from './ActionsList.js';
import * as DnD from 'actions/dnd.js';
import TheHelper from 'actions/TheHelper.js';
import TheEditor from 'actions/TheEditor.js';

import InsertPopover from 'view/controls/InsertPopover.js';

import {baseContextTypes} from 'contextFactory';


class _TestCaseNode extends Component {
    static displayName = 'TestCaseNode';

    static propTypes = {
        path: PropTypes.string.isRequired,
        values: PropTypes.object.isRequired,
        items: PropTypes.array.isRequired,
        cls: PropTypes.string,
        isServiceNode: PropTypes.bool,
        // dnd
        connectDragSource: PropTypes.func.isRequired,
        connectDragPreview: PropTypes.func.isRequired,
        connectDropTarget: PropTypes.func.isRequired,
        isDragging: PropTypes.bool.isRequired,
        canDrag: PropTypes.bool.isRequired,
        canDrop: PropTypes.bool.isRequired
    };

    static contextTypes = baseContextTypes;

    static defaultProps = {
        cls: '',
        // specify it explicitly in your render() method... getDefaultProps doesn't fill props in react-dnd... bug?
        canDrag: true,
        canDrop: true
    };

    constructor(props, context) {
        super();
        this.handleExecuteIcon = this.handleExecuteIcon.bind(this);
        this.handleFailUnexpectedIcon = this.handleFailUnexpectedIcon.bind(this);
        this.renderBody = this.renderBody.bind(this);
        this.state = {
            hasTcErrors: this.hasTestCaseErrors(props),
            hasNestedErrors: this.hasNestedErrors(props)
        };
    }

    _getHeaderText() {
        const values = this.props.values;
        let id = values['#id'] ? ("#" + this.props.values['#id']) : null;
        let reference = values['#reference'] ? (<span title="#reference">
            <span>{(values['#id'] ? " " : "")}</span>
            <span className="exa-tc-header-reference">{"[" + this.props.values['#reference'] + "]"}</span>
        </span>) : null;
        let action = (<span title="#action">
            <span>{((id || reference) ? " ": "")}</span>
            <span className="exa-tc-header-action">{(values["#action"] || "Block")}</span>
        </span>);
        let description = values['#description'] ? (<span title="#description">
            <span>{((id || reference || action || msg_type) ? " : " : "#desc : ")}</span>
            <span className="exa-tc-header-description">{values["#description"]}</span>
        </span>) : null;

        return (<span title="#<id> [<reference>] <action> : <description>">
            <span className="exa-tc-header-id" title="#id">{id}</span>
            {reference}
            {action}
            {description}
        </span>);
    }

    renderBody(ownerBaseNode) {
        const props = this.props;

        var result = [
            <PropertyList
                key="properties"
                values={props.values}
                path={TheHelper.createPath(props.path, c.VALUES_FIELD)}
                fixedFields={true}
            />,
            <ActionsList
                key="actions"
                items={props.items}
                path={props.path}
                ownerNodeKey={props.nodeKey}
                splitter={defaultSetSplitter}
                dndDraggableSets={this.props.canDrag}
                dndDroppableSets={this.props.canDrop}
                dndDraggableActions={this.props.canDrag}
                dndDroppableActions={this.props.canDrop}
            />
        ];

        if (this.state.hasTcErrors) {
            result.push(
                <div className="exa-errors" key="errors">
                    {this.props.errors.map((error) => <Alert bsStyle='danger' bsSize='xsmall'>{error.message}</Alert>)}
                </div>
            );
        }

        return result;
    }

    renderPopover(path) {
        return <InsertPopover path={path} />;
    }

    render() {
        const state = this.state;
        const props = this.props;
        const connectDragSource = props.connectDragSource;
        const connectDragPreview = props.connectDragPreview;
        const connectDropTarget = props.connectDropTarget;
        const isDragging = props.isDragging;

        const values = props.values;
        let addClass="exa-additional-block ";
        if (values["#action"]) {
            addClass += "exa-additional-type-block-" + values["#action"] + " ";
        }

        return  connectDragPreview(
                connectDropTarget(
                    <div className={addClass}>
                    <BaseNode
                        path={props.path}
                        nodeKey={props.nodeKey}
                        nodeCls={`${props.cls} ${isDragging ? 'exa-dragging' : ''}`}
                        headerCls='node-header-test-case'
                        bodyCls='node-body-test-case'
                        bootstrapCls={state.hasTcErrors || state.hasNestedErrors ? 'danger' : 'default'}
                        headerText={this._getHeaderText()}
                        handleIcons={{
                            'execute': { handler: this.handleExecuteIcon, hint: 'Execute' },
                            'filterstop': { handler: this.handleFailUnexpectedIcon, hint: 'Fail on unexpected message' }
                        }}

                        body={this.renderBody}
                        popover={this.renderPopover(props.path)}

                        isServiceNode={props.isServiceNode}

                        dndWrapper={connectDragSource}
                    />
                    </div>));
    }

    _isExecuted() {
        return (this.props.values['#execute'] || 'y').toLowerCase() !== 'n';
    }

    handleExecuteIcon(event) {
        if (arguments.length === 0) {
            return this._isExecuted();
        }

        const path = TheHelper.createPath(this.props.path, 'values', '#execute');
        const value = event.target.checked ? 'y' : 'n';
        TheEditor.edit(path, value);
    }

    handleFailUnexpectedIcon(event) {
        if (arguments.length === 0) {
            return (this.props.values['#fail_on_unexpected_message'] || 'n').toLowerCase() === 'y';
        }

        const path = TheHelper.createPath(this.props.path, 'values', '#fail_on_unexpected_message');
        const value = event.target.checked ? 'y' : 'n';
        TheEditor.edit(path, value);
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            hasTcErrors: this.hasTestCaseErrors(nextProps),
            hasNestedErrors: this.hasNestedErrors(nextProps)
        });
    }

    hasNestedErrors(props) {
        return props.items.some( x => {
            return x.errors.some(err => err.critical);
        });
    }

    hasTestCaseErrors(props) {
        return props.errors.length > 0;
    }
}

export const TestCaseNode = (
DragSource(DnD.Types.BLOCK, DnD.blockSource, DnD.collectSource)(
DropTarget(DnD.Types.BLOCK, DnD.blockTarget, DnD.collectTarget)(
    _TestCaseNode
)));

export default TestCaseNode;
