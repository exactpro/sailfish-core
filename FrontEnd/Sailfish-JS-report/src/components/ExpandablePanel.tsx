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

import { h, Component } from "preact";
import "../styles/expandablePanel.scss"
import { createSelector } from '../helpers/styleCreators';

interface PanelProps {
    header?: JSX.Element;
    // shows, when panel is expanded
    expandedHeader?: JSX.Element;
    body?: JSX.Element;
    isExpanded?: boolean;
    onExpand?: (isExpanded: boolean) => any;
    children: JSX.Element[];
}

interface PanelState {
    isExpanded: boolean;
}

export default class ExpandablePanel extends Component<PanelProps, PanelState> {
    constructor(props: PanelProps) {
        super(props);
        this.state = {
            isExpanded: props.isExpanded != null ? props.isExpanded : false
        };
    }

    expandPanel() {
        this.setState({isExpanded: !this.state.isExpanded});
        this.props.onExpand && this.props.onExpand(this.state.isExpanded);
    }

    componentWillReceiveProps(nextProps: PanelProps) {
        if (nextProps.isExpanded != null) {
            this.setState({isExpanded: nextProps.isExpanded});
        }
    }

    render({ header, body, children, expandedHeader }: PanelProps, { isExpanded }: PanelState) {
        const iconClass = createSelector(
            "expandable-panel__icon", 
            isExpanded ? "expanded" : "hidden"
        );

        return (
            <div class="expandable-panel">
                <div class="expandable-panel__header">
                    <div class={iconClass} 
                        onClick={e => this.expandPanel()}/>
                    { (expandedHeader && isExpanded) || header || children[0] }
                </div>
                {
                    isExpanded ? 
                        body || children.slice(1)
                        : null
                }
            </div>
        )
    }
}
