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
import BaseRow from 'view/base/BaseRow.js';
import Row from './Row.js';
import Input from 'view/inputs/Input.js';

import TheEditor from 'actions/TheEditor.js';
import TheHelper from 'actions/TheHelper.js';
import TheRender from 'actions/TheRender.js';

class CollectionRow extends Row {
    static displayName = 'CollectionRow';

    constructor(props, context) {
        super(props, context);

        this.handleRemoveItem = this.handleRemoveItem.bind(this);
        this.handleAddItem = this.handleAddItem.bind(this);
        this.handleKeyPress = this.handleKeyPress.bind(this);
        this.handleActionClick = this.handleActionClick.bind(this);
        this.handleKeyClick = this.handleKeyClick.bind(this);
        this.renderItem = this.renderItem.bind(this);
    }

    handleRemoveItem (value) {
        const values = TheHelper.getValuesAsArr(this.props.value);
        const idx = values.indexOf(value);
        values.splice(idx, 1);

        TheEditor.edit(this.props.path, TheHelper.getValueAsString(values));
    }

    handleAddItem () {
        const value = this.refs.added.getValue().replace(/ /g, '_');
        if (value) {
            const values = TheHelper.getValuesAsArr(this.props.value);
            values.push(value);
            TheEditor.edit(this.props.path, TheHelper.getValueAsString(values));
            TheEditor.stop(this.props.path, undefined);
        }
    }

    renderContent () {
        const value = this.props.value;
        let renderedValues;

        if (value == null || value === '[]') {
            renderedValues =  undefined;
        } else {
            renderedValues = TheHelper.getValuesAsArr(value).map(this.renderItem);
        }

        return <div className="controls-row exa-prop-coll-ct">
            {renderedValues}
            {this.renderAddItem()}
        </div>;
    }

    handleKeyPress (event) {
        if (event.key === 'Enter') {
            this.handleAddItem();
        }
    }

    renderAddItem () {
        if (this.context.readonly) {
            return null;
        }

        return (<Input
            ref="added"
            path={this.props.path}
            isEditing={this.props.isEditing}
            definition={this.props.definition}
            value={''}
            className="exa-prop-coll-item exa-prop-coll-add"
            onValue={this.handleAddItem}
            valueOnBlur={true}
        />);
    }

    renderItem (value, idx) {
        return ( <div className="exa-prop-coll-item" key={idx}>
                <span className="exa-prop-input">{value}</span>
                {this.context.readonly ? null : <i className="glyphicon glyphicon-remove" onClick={this.handleRemoveItem.bind(this, value)}></i>}
        </div>);
    }

    handleActionClick (event) {
        if (this.props.definition.req) {
            TheEditor.edit(this.props.path, '');
        } else {
            TheEditor.remove(this.props.path);
        }
    }

    handleKeyClick () {
        this.props.handleExpand(this); // TODO
    }

    handleValueClick () {
        this.handleStartEdit();
    }

    render () {
        const props = this.props;

        const key = TheHelper.pathLastKey(props.path);

        const typ = this._getRenderedType(props.path, props.value, props.definition);
        const required = props.definition.req;

        return (
            <BaseRow
                rowCls = {`exa-coll-row-ct ${this._getRowCssCls(props)} ${this.getAdditionalClass(typ)}`}

                val={this.renderContent()}
                propKey={key}
                typ={typ}

                actGlyphIcon={required ? 'erase' : 'remove'}

                onCellClick={this.handleCellClick}

                nestingLevel={props.nestingLevel}

                expandState={props.expandState}
            />
        );
    }

    _getRenderedType(path, value, definition) {
        return TheRender.getRenderedType(path, value, definition);
    }
}

module.exports = CollectionRow;
