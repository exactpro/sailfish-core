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
import Property from 'view/inputs/Input.js';
import Row from './Row.js';

import TheEditor from 'actions/TheEditor.js';
import TheHelper from 'actions/TheHelper.js';
import TheRender from 'actions/TheRender.js';

class PropertyRow extends Row {
    static displayName = 'PropertyRow';
    static propTypes = {
        path: React.PropTypes.string.isRequired,
        nestingLevel: React.PropTypes.number.isRequired,
        srStatus: React.PropTypes.number.isRequired, // search result status
        definition: React.PropTypes.object
    }

    constructor(props, context) {
        super(props, context);

        this.handleStopEdit = this.handleStopEdit.bind(this);
        this.handleValueClick = this.handleValueClick.bind(this);
        this.handleActionClick = this.handleActionClick.bind(this);
    }

    handleStopEdit (val) {
        TheEditor.stop(this.props.path, val);
    }

    handleValueClick () {
        this.handleStartEdit();
    }

    handleTypeClick() {
        if (!this.props.definition || !this.props.definition.type) {
            return;
        }

        let path = this.props.path;
        let store = TheMatrixList[this.context.storeId];
        let value = store.select('data').select(TheHelper.pathAsArr(path)).get();

        if (value === undefined) {
            return;
        }

        value = TheRender.castValue(value, this.props.definition);

        TheEditor.edit(path, value);
    }

    handleActionClick (event) {
        if (this.props.definition.req) {
            TheEditor.edit(this.props.path, '');
        } else {
            TheEditor.remove(this.props.path);
        }
    }

    render () {
        const me = this;
        const props = this.props;

        const typ = TheRender.getRenderedType(this.props.path, this.props.value, props.definition);
        const required = props.definition && props.definition.req;

        const key = TheHelper.pathLastKey(props.path);

        const valCls = 'prop-input-ct';
        const val = (<Property
            path={this.props.path}
            isEditing={this.isEditing()}
            definition={this.props.definition}
            value={this.props.value}
            onValue={this.handleStopEdit}
        />);

        return (
            <BaseRow
                rowCls={this._getRowCssCls(props) + this.getAdditionalClass(typ)}
                valCls={valCls}

                val={val}
                propKey={key}
                typ={typ}

                actGlyphIcon={required ? 'erase' : 'remove'}

                onCellClick={me.handleCellClick}
                nestingLevel={props.nestingLevel}

                expandState={props.expandState}
            />
        );
    }
}

module.exports = PropertyRow;
