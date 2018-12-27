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
import {CURRENT_SEARCH_RESULT} from 'actions/TheFinder.js';
import {baseContextTypes} from 'contextFactory';

import TheEditor from 'actions/TheEditor.js';
import TheHelper from 'actions/TheHelper.js';

class Row extends React.Component {
    static contextTypes = baseContextTypes;
    static propTypes = {
        path: React.PropTypes.string,
        parentPath: React.PropTypes.string,
        isEditing: React.PropTypes.bool,
        value: React.PropTypes.string,
        cls: React.PropTypes.string,
        handleExpand: React.PropTypes.func,
        expandState: React.PropTypes.number,
        definition: React.PropTypes.object
    };
    static defaultProps = {
        cls:  '',
        definition: {}
    };

    constructor(props, context) {
        super(props, context);

        this.handleCellClick = this.handleCellClick.bind(this);
        this.handleStartEdit = this.handleStartEdit.bind(this);
    }

    isEditing () {
        return this.props.isEditing;
    }

    handleCellClick (cellName, event) {
        var handler = ({
            key: this.handleKeyClick,
            val: this.handleValueClick,
            typ: this.handleTypeClick,
            act: this.handleActionClick
        })[cellName];

        handler && handler.call(this, event);
    }

    handleStartEdit () {
        console.log('set edit path=' + this.props.path);
        TheEditor.start(this.props.path, this.context.storeId);
    }

    _getRowCssCls (props) {
        let rowCls = this.props.cls;
        const isEditing = this.isEditing();
        if (isEditing) {
            rowCls += ' prop-row-editing';
        } else if (props.srStatus) {
            rowCls += props.srStatus === CURRENT_SEARCH_RESULT ? ' prop-row-search-result-curr' : ' prop-row-search-result';
        } else if (props.hasErrors) {
            if (Array.isArray(props.hasErrors) && props.hasErrors.length !== 0) {
                if (props.hasErrors.some((err) => err.critical)) {
                    rowCls += ' exa-contain-error';
                } else {
                    rowCls += ' exa-contain-warning';
                }
            } else {
                rowCls += ' exa-contain-error';
            }
        }
        return rowCls;
    }


    getAdditionalClass(type) {
        var cls = " exa-additional-field ";
        let propType = TheHelper.getNodeTypeByPath(this.props.path);
        if (propType === "testCaseField") {
            cls += "exa-additional-field-block ";
        } else if (propType === "actionField") {
            cls += "exa-additional-field-action ";
        }
        let name = TheHelper.pathLastKey(this.props.path);
        if (name && name.length && name[0] == "#") {
            cls += "exa-additional-field-system ";
        }
        if (type) {
            if (!this.props.definition.isCollection)
                cls += "exa-additional-type-field-" + ((type.indexOf("msg:") == 0)? type.substring(4) : type) + " ";
            else
                cls +=  "exa-additional-type-field-" + type.substring(5,type.length-1) + " ";
        }
        if (this.props.value) {
            cls += "exa-additional-field-hasValue ";
        }
        return cls;
    }

    // handleKeyClick(event) {
    //
    // }
    //
    // handleValueClick(event) {
    //
    // }
    //
    // handleTypeClick(event) {
    //
    // }
    //
    // handleActionClick(event) {
    //
    // }
}

module.exports = Row;
