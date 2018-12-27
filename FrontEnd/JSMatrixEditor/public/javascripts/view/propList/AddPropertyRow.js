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

import * as c from 'consts.js';

class AddPropertyRow extends Row {
    static displayName = 'AddPropertyRow';

    _getInitialState () {
        return {
            key: '',
            value: ''
        };
    }

     constructor(props, context) {
         super(props, context);

         this.onKeyEditStart = this.onKeyEditStart.bind(this);
         this.onKeySelect = this.onKeySelect.bind(this);
         this.onValueEditStart = this.onValueEditStart.bind(this);
         this.onValueSelect = this.onValueSelect.bind(this);
         this.handleActionClick = this.handleActionClick.bind(this);
         this.handleStartEdit = this.handleStartEdit.bind(this);

         this.state = this._getInitialState();
     }

    handleStartEdit () {
        TheEditor.start(this.props.path);
    }

    render () {
        var me = this;

        var rowCls = 'prop-add-row';

        var isEditing = this.isEditing();
        if (isEditing) {
            rowCls += ' prop-row-editing';
        }

        const path = this.props.path;
        const valuePath = TheHelper.createPath(path, this.state.key);

        var valDef = (this.state.key && this.props.definition.fields) ? this.props.definition.fields[this.state.key] : undefined;

        var key = (<Property
            ref="key"
            path={path}
            isEditing={isEditing}
            definition={this.props.definition}
            value={this.state.key}
            className="exa-prop-add-input"
            onValue={this.onKeySelect}
            valueOnBlur={true}
        />);

        var val = <Property
            ref="value"
            path={valuePath}
            isEditing={isEditing}
            definition={valDef}
            value={this.state.value}
            onValue={this.onValueSelect}
            valueOnBlur={true}
        />;

        return (
            <BaseRow
                rowCls={rowCls}
                keyCls="exa-prop-add-key"
                valCls="exa-prop-add-val"

                val={val}
                propKey={key}
                actGlyphIcon={isEditing ? 'plus' : ''}

                onCellClick={me.handleCellClick}
                onRowClick={isEditing ? undefined : me.handleStartEdit}
                nestingLevel={this.props.nestingLevel || 0}

                expandState={0}
            />
        );
    }

    onKeyEditStart () {
        TheEditor.start(this.props.path);
    }

    onKeySelect (keyValue) {
        this.setState({
            key: keyValue
        });
        this.refs.value.focus();
    }

    onValueEditStart () {
        TheEditor.start(this.props.path);
    }

    onValueSelect (newValue) {
        this.setState({
            value: newValue
        }, this.handleActionClick.bind(this));
    }

    handleActionClick () {
        var key = this.state.key.trim();
        var value = this.state.value.trim();
        if (value === '' || key === '') {
            return;
        }
        TheEditor.add(this.props.path, key, value);
        // TheEditor.edit(this.props.path + c.PATH_SEP + key, value);

        // set path to edit to next AddPropertyRow
        TheEditor.start(this.props.path);
        this.setState(this._getInitialState());
        // automaticly focus AddPropertyRow after previous row submition
        this.refs.key.focus();
    }

    componentWillReceiveProps (nextProps) {
        if (nextProps.isEditing === false) {
            this.setState(this._getInitialState());
        }
    }
}


module.exports = AddPropertyRow;
