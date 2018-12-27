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
import PropertyRow from './PropertyRow.js';
import AddActionRow from './AddActionRow.js';
import AddPropertyRow from './AddPropertyRow.js';
import CollectionRow from './CollectionRow.js';
import RefCollectionRow from './RefCollectionRow.js';
import SimpleExpandableRow from './SimpleExpandableRow.js';
import HeaderRow from './HeaderRow.js';
import ExpandableRow from './ExpandableRow.js';
import Expander from '../mixins/Expander.js';

import TheDictManager from 'actions/TheDictManager.js';
import TheFinder from 'actions/TheFinder.js';
import TheHelper from 'actions/TheHelper.js';

import TheAppState from 'state/TheAppState.js';

import {MessageDefinition, FieldDefinition, DataItem} from 'actions/__models__.js';
import {baseContextTypes} from 'contextFactory';

export class RenderDataItem {
    constructor(path, value, nestingLevel, isEditing, expandState,
        definition, srStatus, hasErrors, parentPath, nodeKey) {

        this.path = path;
        this.value = value;
        this.nestingLevel = nestingLevel;
        this.isEditing = isEditing;
        this.expandState = expandState;
        this.definition = definition;
        this.srStatus = srStatus;
        this.hasErrors = hasErrors;
        this.parentPath = parentPath;
        this.nodeKey = nodeKey;
    }

    isEqual(other) {
        return (
            this.path === other.path
            && this.value === other.value
            && this.definition === other.definition
            && this.nestingLevel === other.nestingLevel
            && this.isEditing === other.isEditing
            && this.expandState === other.expandState
            && this.srStatus === other.srStatus
            && this.hasErrors === other.hasErrors
            && this.parentPath === other.parentPath
            && this.nodeKey === other.nodeKey
        );
    }
}

class PropertyList extends React.Component {
    static contextTypes = baseContextTypes;
    static displayName = 'PropertyList';
    static propTypes = {
        path: React.PropTypes.string.isRequired,
        values: React.PropTypes.object.isRequired,
        cls: React.PropTypes.string,
        fixedFields: React.PropTypes.bool
    };
    static defaultProps = {
        cls : ''
    };

    constructor(props, context) {
        super(props, context);

        this.handleExpand = this.handleExpand.bind(this);
        this._updateEditingCursor = this._updateEditingCursor.bind(this);
        this._editingCursor = TheAppState.select('propList', 'items', this.context.storeId);

        this.state = this._getInitialState();
    }

    render () {
        const nodeHandler = {
            handleExpand: this.handleExpand
        };
        const myPath = this.props.path;
        const list = this.state.renderData.items
        const isIfContainer = TheHelper.isIfContainer(this.props.values);
        return (
            <div className={`exa-prop-grid-ct ${this.props.cls}`}>
                {(isIfContainer) ? null : (<table className="exa-prop-grid table table-striped table-hover table-condensed">
                    <thead>
                        <HeaderRow/>
                    </thead>
                    <tbody>
                        { list.map((x, idx) => React.createElement(
                            this._getRowClass(x),
                            Object.assign({
                                key: x.nodeKey
                            }, nodeHandler, x)
                        )) }
                    </tbody>
                </table>)}
            </div>
        );
    }

    /**
     *
     * @param props
     * @param state
     * @returns {{items: Array, adding: boolean}}
     * @private
     */
    _getRenderData (props, state) {
        const editPath = state.editingPath;
        const definition = TheDictManager.findDefinition(props.values, this.context.storeId, props.path);
        return {
            items: this._getItemsRenderData(props.path, props.values, editPath, -1, state.expanded, definition, undefined, '0', false),
            adding: editPath === props.path
        };
    }

    componentDidMount() {
        this._editingCursor.on('update', this._updateEditingCursor);
    }


    componentWillUnmount() {
        this._editingCursor.off('update', this._updateEditingCursor);
    }

    _updateEditingCursor(event) {
        this.setState(this._getState(this.props));
    }

    /**
     * @param {string} path
     * @param {*} value
     * @param {string} editPath
     * @param {number} nestingLevel
     * @param {string} expandedCfg
     * @param {object} definition
     * @param {string} parentPath
     * @param {string} nodeKey
     * @param {boolean} hasErrors
     * @returns {Array}
     */
    _getItemsRenderData (path, value, editPath, nestingLevel = 0, expandedCfg, definition, parentPath, nodeKey, hasErrors) {

        const cx = this.context;
        let result;
        let expanded;

        const topLevelNode = (typeof value === 'object' && nestingLevel === -1);
        const nestedData = TheHelper.getNestedData(path, value, definition, cx.storeId);

        if (topLevelNode) {
            expanded = true;
            result = [];
        } else {
            expanded = nestedData && Expander.isRowExpanded(nodeKey, expandedCfg);
            result = [new RenderDataItem(
                path,
                value,
                nestingLevel,
                editPath === path,
                TheHelper.getNodeExpandState(expanded, expanded === undefined),
                definition,
                TheFinder.__getSearchStatus(this.context.panelId, path),
                hasErrors,
                parentPath,
                nodeKey
            )];
        }

        if (nestedData && expanded) {
            if (!topLevelNode) {
                result[result.length - 1].expandable = true; // mark last node as expandable if it has nested data
            }

            const innerNestingLevel = nestingLevel + 1;
            for (var i = 0, len = nestedData.length; i < len; i++) {
                const dataItem = nestedData[i];
                var renderItems = this._getItemsRenderData(
                    dataItem.path,
                    dataItem.data,
                    editPath,
                    innerNestingLevel,
                    expandedCfg,
                    dataItem.definition || TheDictManager.getBaseFieldDefinition(TheHelper.pathLastKey(dataItem.path)) || undefined,
                    path,
                    nodeKey + '>' + i,
                    dataItem.hasErrors
                );

                result.push(...renderItems);
            }
        }

        let type = TheHelper.getNodeTypeByPath(path);
        if (!this.context.readonly && expanded && (type === 'actionValues' || type === 'action' || type === 'action+')) {
            const addingRowPath = (type === 'action' || type === 'action+') ? TheHelper.createPath(path, 'values') : path;
            result.push(new RenderDataItem(
                addingRowPath,
                undefined,
                nestingLevel,
                editPath === addingRowPath,
                false,
                definition,
                false,
                false, // hasErrors
                parentPath,
                'add>' + nodeKey
            ));
        }

        return result;
    }

    /**
     *
     * @param {RenderDataItem} item
     * @returns {Constructor}
     * @private
     */
    _getRowClass (item) {
        let result;
        const definition = item.definition || {};

        if (item.value !== null && typeof item.value === 'object') {
            result = SimpleExpandableRow;
        } else if (definition) {
            if (item.path === undefined) { // not exists
                result = AddActionRow;
            } else if (definition instanceof MessageDefinition) {
                result = AddPropertyRow;
            } else if (definition.isCollection) {
                if (definition.type === 'SUBMESSAGE') {
                    result = RefCollectionRow;
                } else {
                    result = CollectionRow;
                }
            } else {
                if (definition.type === 'SUBMESSAGE') {
                    result = ExpandableRow;
                } else {
                    result = PropertyRow;
                }
            }
        } else {
            result = PropertyRow;
        }

        return result;
    }

    componentWillReceiveProps (nextProps) {
        this.setState(this._getState(nextProps, this.state.expanded));
    }

    shouldComponentUpdate (nextProps, nextState) {
        const nextItems = nextState.renderData.items;
        const currRenderData = this.state.renderData;
        const currItems = currRenderData.items;

        return !!(this.state.expanded !== nextState.expanded
        || currRenderData.adding !== nextState.renderData.adding
        || currItems.length !== nextItems.length
        || currItems.some((x, idx) => !x.isEqual(nextItems[idx])
        ));
    }

    /**
     *
     * @param {object} props
     * @param {string} expandedCfg
     * @returns {{expanded: *}}
     * @private
     */
    _getState (props, expandedCfg) {
        const state = {
            editingPath: this._editingCursor.get('editingPath')
        };

        state.expanded = arguments.length === 1 ? this.state.expanded : expandedCfg;

        state.renderData =  this._getRenderData(props, state);
        return state;
    }

    _getInitialState () {
        return this._getState(this.props, Expander.getEmptyExpanded());
    }

    handleExpand (row) {
        const nodeKey = row.props.nodeKey;
        const expandedCfg = this.state.expanded;
        if (Expander.isRowExpanded(nodeKey, expandedCfg)) {
            this.setState(this._getState(this.props, Expander.collapseRow(nodeKey, expandedCfg)));
        } else {
            this.setState(this._getState(this.props, Expander.expandRow(nodeKey, expandedCfg)));
        }
    }
}

module.exports = PropertyList;
