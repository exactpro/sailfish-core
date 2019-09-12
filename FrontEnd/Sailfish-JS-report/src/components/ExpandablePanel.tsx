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
import "../styles/expandablePanel.scss"
import { createSelector } from '../helpers/styleCreators';
import StateSaver, { RecoverableElementProps } from "./util/StateSaver";
import { connect } from 'react-redux';
import AppState from '../state/models/AppState';
import { stopPropagationHandler } from '../helpers/react';

interface PanelProps {
    header?: React.ReactNode;
    body?: React.ReactNode;
    isExpanded?: boolean;
    onExpand?: (isExpanded: boolean) => any;
    children: React.ReactNode[];
}

export const ExpandablePanel = ({ header, body, children, isExpanded, onExpand }: PanelProps) => {
    const iconClass = createSelector(
        "expandable-panel__icon", 
        isExpanded ? "expanded" : "hidden"
    );

    return (
        <div className="expandable-panel">
            <div className="expandable-panel__header">
                <div className={iconClass} 
                    onClick={stopPropagationHandler(onExpand, !isExpanded)}/>
                { header || children[0] }
            </div>
            {
                isExpanded ? 
                    body || children.slice(1)
                    : null
            }
        </div>
    )
}

interface RecoverablePanelProps extends PanelProps, RecoverableElementProps {}

export const RecoverableExpandablePanel = ({ stateKey, ...props }: RecoverablePanelProps) => (
    <StateSaver
        stateKey={stateKey}>
        {
            (isExpanded: boolean, stateSaver) => (
                <ExpandablePanel
                    {...props}
                    isExpanded={props.isExpanded !== undefined ? props.isExpanded : isExpanded}
                    onExpand={isExpanded => {
                        stateSaver(isExpanded);
                        props.onExpand && props.onExpand(isExpanded)
                    }}/>
            )
        }
    </StateSaver>
)

interface SearchExpandablePanelOwnProps extends RecoverablePanelProps {
    searchKeyPrefix: string;
}

export const SearchExpandablePanel = connect(
    (state: AppState, ownProps: SearchExpandablePanelOwnProps) => {
        const [currentKey] = state.selected.searchResults.getByIndex(state.selected.searchIndex),
            isExpanded = currentKey && currentKey.startsWith(ownProps.searchKeyPrefix) ? true : undefined;        

        return { isExpanded };
    }
)(RecoverableExpandablePanel);
