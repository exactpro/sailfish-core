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
import AutoInput from './AutoInput.js';
import BooleanInput from './BooleanInput.js';
import ReadOnlyInput from'./ReadOnlyInput.js';
import TheHelper from 'actions/TheHelper.js';

import {baseContextTypes} from 'contextFactory';

class Input extends React.Component {
    static displayName = 'EpsInput';

    static contextTypes = baseContextTypes;

    static propTypes = {
        path: React.PropTypes.string.isRequired,
        value: React.PropTypes.string,
        definition: React.PropTypes.object, //.isRequired,
        isEditing: React.PropTypes.bool.isRequired,
        className: React.PropTypes.string,
        onValue: React.PropTypes.func
    }

    static defaultProps = {
            className: '',
            value: undefined
    };

    constructor (props, context) {
        super(props, context);
    }

    getValue () {
        return this.refs.input.getValue();
    }

    focus () {
        return this.refs.input.focus();
    }

    render  () {
        const def = this.props.definition;
        let InputClass;

        if (this.context.readonly) {
            InputClass = ReadOnlyInput;
        } else if (def && def.type === 'BOOLEAN' && TheHelper.getNodeTypeByPath(this.props.path) == 'testCaseField') {
            // Only testcase fields should be BooleanInput.
            // We can't use references/util-functions/static-values/etc in BooleanInput
            InputClass = BooleanInput;
        } else {
            InputClass = AutoInput;
        }

        // use input ref for read only
        return React.createElement(InputClass, Object.assign({}, this.props, {ref: 'input'}));
    }
}


module.exports = Input;
