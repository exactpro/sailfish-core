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

import React, {Component} from 'react';
import {Button, DropdownButton, MenuItem} from 'react-bootstrap';
import TheAppState from 'state/TheAppState.js';
import TheMatrixList from 'state/TheMatrixList.js';
import TheRender from 'actions/TheRender.js';
import TheHelper from 'actions/TheHelper.js';
import {baseContextTypes} from 'contextFactory';

export class Breadcrumbs extends Component {
    static displayName = 'Breadcrumbs';

    static contextTypes = baseContextTypes;

    propTypes: {
        panelId: React.PropTypes.string.isRequired,
        storeId: React.PropTypes.string.isRequired
    }

    constructor(props, cx) {
        super(props, cx);

        this.__overNodeCursor = null;
        this.__storeDataCursor = null;

        this.__handleUpdate = this.__handleUpdate.bind(this);
        this.__handleUpdate = this.__handleUpdate.bind(this);
        this.handleSelect = this.handleSelect.bind(this);

        this.state = this.__getState(props);
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.storeId !== this.props.storeId || nextProps.panelId !== this.props.panelId) {
            this.__subscribeOnCursors(nextProps.panelId, nextProps.storeId);
            this.__handleUpdate(nextProps);
        }
    }

    __subscribeOnCursors(panelId, storeId) {
        this.__unsubscribeFromCursors();
        this.__overNodeCursor = TheAppState.select('node', panelId, 'overPath');
        this.__storeDataCursor = TheMatrixList[storeId] && TheMatrixList[storeId].select('data');
        this.__overNodeCursor.on('update', this.__handleUpdate);
        this.__storeDataCursor && this.__storeDataCursor.on('update', this.__handleUpdate);
    }

    __unsubscribeFromCursors() {
        if (this.__overNodeCursor !== null) {
            this.__overNodeCursor.off('update', this.__handleUpdate);
            this.__overNodeCursor = null;
        }
        if (this.__storeDataCursor !== null) {
            this.__storeDataCursor.off('update', this.__handleUpdate);
            this.__storeDataCursor = null;
        }
    }

    __handleUpdate() {
        this.setState(this.__getState(this.props));
    }

    componentDidMount() {
        this.__subscribeOnCursors(this.props.panelId, this.props.storeId);
    }

    componentWillUnmount() {
        this.__unsubscribeFromCursors();
    }

    __getState(props) {
        const result = {
            breadcrumbs: [],
            testCases: []
        };

        const overPath = this.__overNodeCursor && this.__overNodeCursor.get();
        const testCases = this.__storeDataCursor && this.__storeDataCursor.get() || [];

        result.testCases = testCases.map(x => TheRender.testCaseName(x));

        if (overPath !== null) {
            const pathAsArray = TheHelper.pathAsArr(overPath);
            result.breadcrumbs.push(result.testCases[pathAsArray[0]]);
            const actionName = TheRender.actionName(testCases, pathAsArray);
            if (actionName) {
                result.breadcrumbs.push(actionName);
            }
        }

        return result;
    }

    handleSelect(testCaseIdx, event) {
        TheAppState.set(['node', this.props.panelId, 'scrollToPath'], testCaseIdx);
        // just update path in view (till user move mouse pointer over any node)
        // global app state will not affected
        this.setState({breadcrumbs: [this.state.testCases[testCaseIdx]]});
    }

    render() {
        //const sep = <b> </b>;
        const state = this.state;

        return (
            <div  className='exa-breadcrumbs'>
                <form className='form-inline exa-form-inline' action="javascript:void(0);">
                    <DropdownButton bsSize="xsmall" title={state.breadcrumbs[0] || 'Test cases'}
                        onSelect={this.handleSelect}
                    >
                         {state.testCases.map((x, idx) =>
                            <MenuItem
                                eventKey={'' + idx}
                                key={idx}
                            >
                                {x}
                            </MenuItem>)}
                    </DropdownButton>
                    { state.breadcrumbs[1] !== undefined ? <Button bsSize="xsmall" active>{state.breadcrumbs[1]}</Button> : undefined }
                </form>
            </div>
        );
    }
}

export default Breadcrumbs;
