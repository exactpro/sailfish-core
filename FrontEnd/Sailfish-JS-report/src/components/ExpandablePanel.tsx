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

import { h, Component, PreactDOMAttributes } from "preact";
import "../styles/panel.scss"

interface IPanelProps {
    header?: JSX.Element;
    body?: JSX.Element;
    isExpanded?: boolean;
    children: JSX.Element[];
}

interface IPanelState {
    isExpanded: boolean;
}

export default class ExpandablePanel extends Component<IPanelProps, IPanelState> {
    constructor(props: IPanelProps) {
        super(props);
        this.state = {
            isExpanded: props.isExpanded !== undefined ? props.isExpanded : false
        };
    }

    expandPanel() {
        this.setState({isExpanded: !this.state.isExpanded})

    }

    render({ header, body, children }: IPanelProps, { isExpanded }: IPanelState) {
        const iconClass = ["expandable-panel-header-icon", (isExpanded ? "expanded" : "hidden")].join(' ');
        return (<div class="expandable-panel-root">
            <div class="expandable-panel-header">
                <div class={iconClass} 
                    onClick={e => this.expandPanel()}/>
                {header || children[0]}
            </div>
            {isExpanded ?
                <div className="expandable-panel-body">
                        {body || children.slice(1)}
                </div>
                : null}
        </div>)
    }
}