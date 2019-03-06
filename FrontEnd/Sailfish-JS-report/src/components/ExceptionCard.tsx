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

import {h, Component} from 'preact';
import Exception from '../models/Exception';

interface Props {
    exception: Exception;
    drawDivider: boolean;
}

interface State {
    isExpanded: boolean;
}

export default class ExceptionCard extends Component<Props, State> {
    toggle() {
        this.setState({isExpanded: !this.state.isExpanded})
    }

    render({ exception, drawDivider }: Props, { isExpanded }: State) {

        const divider = (
                <div class="status-panel-exception-divider">
                    <div class="status-panel-exception-divider-icon"/>
                </div>
        )

        return (
            <div>
                {drawDivider? divider : null}
                <div class="status-panel failed">
                        <div class="status-panel-exception-wrapper">
                            <div class="status-panel-exception-header">
                                <div>{isExpanded? exception.class + ": " : null}</div>
                                <div>{exception.message}</div>
                            </div>
                            <div class="status-panel-exception-expand" onClick={e => this.toggle()}>
                                <div class="status-panel-exception-expand-title">More</div>
                                <div class={"status-panel-exception-expand-icon " + (isExpanded ? "expanded" : "hidden")}/>
                            </div>
                        </div>
                        <div style={isExpanded ? null : "display: none"} class="status-panel-exception-stacktrace">
                            <pre>{exception.stacktrace}</pre>
                        </div>
                </div>
            </div>
        )
    }
}

