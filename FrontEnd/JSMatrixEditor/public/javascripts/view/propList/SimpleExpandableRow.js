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
import React from 'react';
import BaseRow from 'view/base/BaseRow.js';
import Row from './Row.js';


// represents Expandable nested message/leg
class SimpleExpandableRow extends Row {
    static displayName = 'SimpleExpandableRow';

    constructor(props, context) {
        super(props, context);

        this.handleKeyClick = this.handleKeyClick.bind(this);
    }

    _getRowText(props) {
        let result;
        if (typeof props.value === 'object') {
            const values = props.value[c.VALUES_FIELD];
            if (values['#action']) {
                if (values['#action'].toLowerCase() === 'startblock') {
                    result = 'Block: ' + values[c.REFERENCE_FIELD];
                } else if (values['#action']) {
                    result = 'Action: ' + values['#action'];
                }
            } else if (values[c.REFERENCE_FIELD]) {
                result = 'Ref: ' + values[c.REFERENCE_FIELD];
                const msgType = values['#message_type'] || props.definition.name;
                if (msgType) {
                    result += '; Msg: ' + msgType;
                }
            }
        } else if (props.definition && props.definition.name) {
            result  = props.definition.name;
        } else {
            result = 'expandable';
        }
        return result;
    }

    render () {
        const props = this.props;
        const rowCls = this.props.cls;

        const key = this._getRowText(props);

        return (
            <BaseRow
                rowCls={rowCls + " " + this.getAdditionalClass()}

                propKey={key}

                onCellClick={this.handleCellClick}

                nestingLevel={props.nestingLevel}

                expandState={props.expandState}
            />
        );
    }

    handleKeyClick () {
        this.props.handleExpand(this);
    }
}

module.exports = SimpleExpandableRow;
