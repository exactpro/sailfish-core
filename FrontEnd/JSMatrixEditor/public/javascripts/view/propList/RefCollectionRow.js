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
import CollectionRow from './CollectionRow.js';

import TheEditor from 'actions/TheEditor.js';
import TheHelper from 'actions/TheHelper.js';

class RefCollectionRow extends CollectionRow {
    static displayName = 'RefColleсtionRow';

    constructor (props, context) {
        super(props, context);

        // this.xx = this.xx.bind(this);
    }

    handleAddItem (event) {
        const props = this.props;
        const value = this.refs.added.getValue().replace(/ /g, '_');
        if (value) {
            const values = TheHelper.getValuesAsArr(props.value);
            values.push(value);
            const newValue = TheHelper.getValueAsString(values);
            TheEditor.edit(props.path, newValue, this.context.storeId);
            TheEditor.stop(props.path, undefined);
        }
    }

    _getRenderedType(path, value, definition) {
        let result = super._getRenderedType(path, value, definition);
        return result; //TODO
    }
}

module.exports = RefCollectionRow;
