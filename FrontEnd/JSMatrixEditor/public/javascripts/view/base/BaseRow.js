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
import TheHelper from 'actions/TheHelper';
import {TheDictManager} from 'actions/TheDictManager';
import {baseContextTypes} from 'contextFactory';

const BaseRow = React.createClass({
    displayName: 'BaseRow',

    propTypes: {
        propKey: React.PropTypes.any,
        val: React.PropTypes.any,
        typ: React.PropTypes.any,
        act: React.PropTypes.any,
        actGlyphIcon : React.PropTypes.string,
        propKeyCls: React.PropTypes.string,
        valCls: React.PropTypes.string,
        typCls: React.PropTypes.string,
        actCls: React.PropTypes.string,
        rowCls: React.PropTypes.string,
        onCellClick: React.PropTypes.func,
        onRowClick: React.PropTypes.func,
        nestingLevel: React.PropTypes.number,
        expandState: React.PropTypes.number.isRequired
    },

    contextTypes: baseContextTypes,

    getDefaultProps: function() {
        return {
            propKeyCls: '',
            valCls: '',
            typCls: '',
            actCls: '',
            rowCls: '',
            propKey: '',
            val: '',
            typ: '',
            act: '',
            actGlyphIcon: ''
        };
    },

    render: function() {
        const props = this.props;

        const propKey = props.propKey;
        const val = props.val;
        const typ = props.typ;

        const act = props.act;
        const title = TheDictManager.getHelpString(propKey);

        const isNodeExpandable = TheHelper.isNodeExpandable(props.expandState);
        const rowExpandCls = isNodeExpandable ? (
            'exa-prop-row-expandable' +
            (TheHelper.isNodeExpanded(props.expandState) ?
                    ' exa-node-expanded' :
                    ' exa-node-collapsed')
        ) : (
            TheHelper.isNodeLeaf(props.expandState) ?
                ' exa-prop-row-leaf' :
                ''
        );

        const propNestingCls = props.nestingLevel ? 'exa-nested-prop-' + props.nestingLevel : '';

        const actIcon = `glyphicon glyphicon-${props.actGlyphIcon}`;

        return (
            <tr className={`exa-prop-row ${props.rowCls} ${rowExpandCls}`} onClick={props.onRowClick}>
                <td className={`exa-prop-key ${props.propKeyCls} ${propNestingCls}`} onClick={props.onCellClick.bind(null, 'key')}>
                    <span className="exa-prop-key-wrap" title={title} >
                        {propKey}
                    </span>
                </td>
                <td className={`exa-prop-val ${props.valCls}`} onClick={props.onCellClick.bind(null, 'val')}>{val}</td>
                <td className={`exa-prop-type ${props.typCls}`} onClick={props.onCellClick.bind(null, 'typ')}>{typ}</td>
                <td className="exa-prop-act-ct">
                    {(props.act || props.actGlyphIcon) && !this.context.readonly ? (
                        <i className={`exa-prop-act ${props.actCls} ${actIcon}`}
                            onClick={props.onCellClick.bind(null, 'act')} href="javascript:;">{act}</i>
                    ) : (
                        ''
                    )}
                </td>
            </tr>
        );
    }
});

module.exports = BaseRow;
