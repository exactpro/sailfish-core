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

import {isEqual} from 'lodash';
import React, {PropTypes, Component} from 'react';
import {findDOMNode} from 'react-dom';

export const NODE_CT_CLS = 'exa-node-ct';

import {CURRENT_SEARCH_RESULT,
    CONTAINS_CURRENT_SEARCH_RESULT,
    SEARCH_RESULT,
    CONTAINS_SEARCH_RESULT} from 'consts';

export class BaseNodeView extends Component {

    static propTypes = {
        expandHandler: PropTypes.func,
        expanded: PropTypes.bool,       //number?
        alwaysExpanded: PropTypes.bool,
        alwaysVisible: PropTypes.bool,
        handleIcons: PropTypes.object, // icon cls to { handler: function, hint: string }
        headerText: PropTypes.object.isRequired,
        headerIdx: PropTypes.string,
        headerCls: PropTypes.string,
        body: PropTypes.any.isRequired,
        bodyCls: PropTypes.string,
        nodeCls: PropTypes.string,
        isServiceNode: PropTypes.bool,
        bootstrapCls: PropTypes.string,
        nodeKey: PropTypes.string.isRequired, // any unique string. used to mark node in HTML attribute 'data-nodekey'
        dndWrapper: PropTypes.func,
        popover: PropTypes.object
    };

    static defaultProps = {
        nodeCls: '',
        headerCls: '',
        bodyCls: '',
        isServiceNode: false,
        bootstrapCls: 'default',
        handleIcons: {}, // map icon cls to handler
        headerIdx: '',
        dndWrapper: x => x,
        alwaysVisible: false,
        expanded: false
    };

    constructor(props, context) {
        super(props, context);

        this._savedHeight = 0;

        this.state = this.__getState(props);
        this.state.searchStatus = 0;
        this.state.isMouseOver = false;
        this.state.isCopyPasteSource = false;

        this.handleExpandIconClick = this.handleExpandIconClick.bind(this);
    }

    __getState(props, state = this.state) {
        // state can be undefined (at first call)
        return {
            expanded: props.alwaysExpanded ?
                true :
                (state ?
                    state.expanded :
                    props.expanded),
            iconsValues: this._getIconsValues(props)  // call without arguments - return value
        };
    }

    _getIconsValues(props) {
        return Object.keys(props.handleIcons).reduce((result, x) => {
            result[x] = props.handleIcons[x].handler();
            return result;
        }, {});
    }

    handleExpandIconClick() {
        const currentStatus = !this.state.expanded;
        this.setState({expanded: currentStatus});
        if (typeof this.props.expandHandler === 'function') {
            this.props.expandHandler(currentStatus);
        }
    }

    handleMouseEnter() {

    }

    _getMainCssClass(cls = '') {
        let result;
        (cls !== '') && (result += ' ' + cls);
        result += (' ' + NODE_CT_CLS);
        (this.props.nodeCls && this.props.nodeCls !== '') && (result += ' ' + this.props.nodeCls);
        (this.state.isMouseOver !== false) && (result += ' exa-node-over');
        (this.state.searchStatus !== 0) && (result += {
            [CURRENT_SEARCH_RESULT]: ' exa-search-result ',
            [CONTAINS_CURRENT_SEARCH_RESULT]: ' exa-search-result ',
            [SEARCH_RESULT]: ' exa-search-result ',
            [CONTAINS_SEARCH_RESULT]: ' exa-search-result '
        }[this.state.searchStatus]);
        (this.state.isCopyPasteSource) && (result+= ' exa-sopy-source ');        //TODO set isCopyPasteSource

        return result.trim();
    }

    render() {
        const state = this.state;
        const props = this.props;
        const context = this.context;
        return (
            <div className = {'panel panel-' + props.bootstrapCls + ' ' + this._getMainCssClass()}
                data-nodekey = {props.nodeKey}
                style = {this._getInlineStyle(state, props)}
                onMouseMove = {props.isServiceNode ? undefined : this.handleMouseEnter}
            >
                {this._renderContent(state, props, context)}
            </div>
        );
    }

    _renderContent(state, props, context) {
        const result = [];
        if (state.visible) {
            const expandCls = state.expanded ? 'triangle-bottom' : 'triangle-right';
            result.push(props.dndWrapper(
                <div className={`panel-heading exa-node-header-ct ${expandCls} ${props.headerCls}`} key='h'>

                    <span className={`exa-node-expand-icon glyphicon glyphicon-${expandCls}`}
                        onClick={!props.alwaysExpanded && this.handleExpandIconClick}/>

                    <div className="exa-node-header">
                        {Object.keys(props.handleIcons).map((x, idx) =>
                            <label className="exa-checkbox-label" key={idx}>
                                <input className={`exa-checkbox exa-checkbox-${x}`}
                                    type="checkbox"
                                    checked={state.iconsValues[x]}
                                    onChange={props.handleIcons[x].handler}
                                />
                                <i title={props.handleIcons[x].hint} />
                            </label>
                        )}

                        <span className="exa-node-header-text">{props.headerText}</span>
                    </div>

                    {(state.isMouseOver && !context.readonly && props.popover) ? (typeof props.popover === 'function' ? props.popover(this) : props.popover) : <span className="exa-action-number">{props.headerIdx}</span>}
                </div>
            ));
            if (state.expanded) {
                result.push(
                    <div className={`panel-body exa-node-body-ct ${expandCls} ${props.bodyCls}`} key='b'>
                        <div className="exa-node-body">
                            {typeof props.body === 'function' ? props.body(this) : props.body}
                        </div>
                    </div>
                );
            }
        }
        return result;
    }

    _getInlineStyle(state, props) {
        return (!state.visible && state.expanded) ? {height: this._savedHeight} : {};
    }

    shouldComponentUpdate(nextProps, nextState) {
        const state = this.state;
        const props = this.props;
        let result = true;

        if (nextState.visible !== state.visible) {
            if (nextState.visible) {
                result = true;
            } else {
                result = nextState.expanded;
            }
        } else if (nextState.visible === false) {
            result = false;
        } else if (nextState.expanded !== state.expanded) {
            result = true;
        } else {
            if (nextState.expanded === true) {
                result = true;
            } else {
                result = (
                nextProps.headerText !== props.headerText
                || nextProps.headerIdx !== props.headerIdx
                || nextState.searchStatus !== state.searchStatus
                || nextState.isCopyPasteSource !== state.isCopyPasteSource
                || nextProps.nodeCls !== props.nodeCls
                || nextProps.headerCls !== props.headerCls
                || nextProps.bodyCls !== props.bodyCls
                || nextProps.bootstrapCls !== props.bootstrapCls
                || nextState.isMouseOver !== state.isMouseOver
                || !isEqual(nextState.iconsValues, state.iconsValues)
                );
            }
        }

        return result;
    }

    _saveOwnHeight() {
        this._savedHeight = findDOMNode(this).offsetHeight;
    }

    componentDidMount() {
        this._saveOwnHeight();
    }

    componentDidUpdate() {
        this._saveOwnHeight();
    }

    componentWillReceiveProps(nextProps) {
        this.setState(this.__getState(nextProps));
    }
}

export default BaseNodeView;
