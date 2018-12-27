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
import {Alert} from 'react-bootstrap';
import TheAppState from 'state/TheAppState.js';

class  ErrorPopup  extends React.Component {
    static displayName = 'ErrorPopup';

    constructor(props, cx) {
        super(props, cx);
        this._handleCursorUpdate = this._handleCursorUpdate.bind(this);
        this.__cursor = TheAppState.select('errors');
        this.state = this.__getState();
    }

    componentWillMount() {
        this.__cursor.on('update', this._handleCursorUpdate);
    }

    componentWillUnmount() {
        this.__cursor.off('update', this._handleCursorUpdate);
    }

    _handleCursorUpdate() {
        this.setState(this.__getState());
    }

    handleAlertDismiss(idx) {
        // TODO - view should not directly change state
        this.__cursor.select('list', idx).set('visible', false);
    }

    handleTimeoutAdding(idx, timeout) {
        // TODO - view should not directly change state
        const handler = this.handleAlertDismiss.bind(this, idx);
        this.__cursor.select('list', idx).set('hasTimeout', true);
        setTimeout(handler, timeout);
    }

    __getState() {
        return {
            list: this.__cursor.select('list').get()
        };
    }

    render() {
        return (
            <div className="exa-errors-container">
                {this.state.list.reduce((alerts, error, idx) => {
                    if (error.visible) {
                        const handler = this.handleAlertDismiss.bind(this, idx);
                        alerts.push(
                            <Alert key={idx} bsStyle={error.severity} onDismiss={handler} onDoubleClick={handler}>
                                <strong>{error.message}</strong> {error.rootCause}
                            </Alert>,
                        );
                        if (error.visible && !error.hasTimeout) {
                            if (error.severity == 'success') {
                                this.handleTimeoutAdding(idx, 5000);
                            } else if (error.severity != 'danger') {
                                this.handleTimeoutAdding(idx, 15000);
                            } else {
                                //FIXME need to add another method to view errors, because they can be important
                                this.handleTimeoutAdding(idx, 40000);
                            }
                        }
                    }
                    return alerts;
                }, [])}
            </div>
        );
    }
}

module.exports = ErrorPopup;
export default ErrorPopup;
