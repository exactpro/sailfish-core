/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import * as React from 'react';
import Exception from '../models/Exception';

interface Props {
    exception: Exception;
    drawDivider: boolean;
}

interface State {
    isExpanded: boolean;
}

export default class ExceptionCard extends React.Component<Props, State> {

    constructor(props) {
        super(props);

        this.state = {
            isExpanded: false
        }
    }

    render() {
        const { exception, drawDivider } = this.props,
            { isExpanded } = this.state;

        const divider = (
            <div className="status-panel-exception-divider">
                <div className="status-panel-exception-divider-icon"/>
            </div>
        )

        return (
            <div>
                {drawDivider? divider : null}
                <div className="status-panel failed">
                    <div className="status-panel-exception-wrapper">
                        <div className="status-panel-exception-header">
                            <div>{isExpanded? exception.class + ": " : null}</div>
                            <div>{exception.message}</div>
                        </div>
                        <div className="status-panel-exception-expand" onClick={this.onTogglerClick}>
                            <div className="status-panel-exception-expand-title">More</div>
                            <div className={"status-panel-exception-expand-icon " + (isExpanded ? "expanded" : "hidden")}/>
                        </div>
                    </div>
                    <div style={{ display: isExpanded ? null : "none"}} className="status-panel-exception-stacktrace">
                        <pre>{exception.stacktrace}</pre>
                    </div>
                </div>
            </div>
        )
    }

    private onTogglerClick = (e: React.MouseEvent) => {
        this.setState({isExpanded: !this.state.isExpanded});

        e.stopPropagation();
    }
}

