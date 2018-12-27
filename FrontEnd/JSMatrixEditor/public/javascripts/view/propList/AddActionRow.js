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
import {Button} from 'react-bootstrap';
import Row from './Row.js';
import BaseRow from 'view/base/BaseRow.js';

import TheEditor from 'actions/TheEditor.js';

class AddActionRow extends Row {
    static displayName = 'AddActionRow';

    constructor(props, context) {
        super(props, context);

        this.handleActionClick = this.handleActionClick.bind(this);
        this.onCreate = this.onCreate.bind(this);
    }

    handleStopEdit (val) {
    }

    handleValueClick () {
    }

    handleActionClick(event) {
        // FIXME: remove repeating group from list
        // TheEitor.remove(this.props.path);
        // or remove 'action' button
    }

    onCreate() {
        const props = this.props;
        TheEditor.editAndAddRefsIf(props.definition, props.parentPath, undefined, [props.value]);
    }

    render () {
        const props = this.props;

        const typ = '';
        const key = 'Ref: ' + props.value + '; Msg: ' + props.definition.name;

        const valCls = 'prop-input-ct';
        const val = (<Button onClick={this.onCreate} bsSize="xsmall" disabled={this.context.readonly}>Create</Button>);

        return (
            <BaseRow
                rowCls={this._getRowCssCls(props)}
                valCls={valCls}

                val={val}
                propKey={key}
                typ={typ}

                actGlyphIcon={'remove'}

                onCellClick={this.handleCellClick}
                nestingLevel={props.nestingLevel}
                expandState={0}
            />
        );
    }
}

module.exports = AddActionRow;
