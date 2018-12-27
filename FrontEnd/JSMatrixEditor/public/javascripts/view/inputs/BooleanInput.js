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
import {baseContextTypes} from 'contextFactory';

function emptyFn() {}

var BooleanInput = React.createClass({
    displayName: 'BooleanInput',

    contextTypes: baseContextTypes,

    propTypes: {
        path: React.PropTypes.string.isRequired,
        value: React.PropTypes.string.isRequired,
        definition: React.PropTypes.object.isRequired,
        isEditing: React.PropTypes.bool.isRequired,
        className: React.PropTypes.string,
        onValue: React.PropTypes.func
    },

    _onChange: function(key, event) {
        this.props.onValue(key ? 'y' : 'n');
    },

    getDefaultProps: function() {
        return {
            className: '',
            onValue: emptyFn
        };
    },

    getNullableBoolean: function(boolStr) {
        if (boolStr == null) {
            return null;
        }
        return boolStr.toLowerCase() !== 'n';
    },

    getValue: function() {
        throw 'Not Implemented';
    },

    focus: function() {
        throw 'Not Implemented';
    },

    render: function () {
        const value =  this.getNullableBoolean(this.props.value);

        return (
            <div className={`exa-prop-input exa-prop-input-bool ${this.props.className}`}>
                <input ref="yes" type="radio" name={this.props.path} value="yes" checked={value === true} onChange={this._onChange.bind(this, true)}/>True
                <input ref="no" type="radio" name={this.props.path} value="no" checked={value === false} onChange={this._onChange.bind(this, false)}/>False
            </div>
        );
    }
});


module.exports = BooleanInput;
