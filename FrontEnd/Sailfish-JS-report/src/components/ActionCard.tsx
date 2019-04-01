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

import { h } from "preact";
import Action from "../models/Action";
import ParamsTable from "./ParamsTable";
import ExpandablePanel from "./ExpandablePanel";
import { StatusType } from "../models/Status";
import "../styles/action.scss";
import { getSecondsPeriod, formatTime } from "../helpers/dateFormatter";
import { ExceptionChain } from "./ExceptionChain";
import { Chip } from "./Chip";
import { createSelector } from '../helpers/styleCreators';

interface CardProps {
    action: Action;
    onSelect?: (action: Action) => void;
    children?: JSX.Element[];
    isSelected?: boolean;
    isRoot?: boolean;
    isTransaparent?: boolean;
    isExpanded?: boolean;
    ref?: Function;
}

export const ActionCard = ({ action, children, isSelected, onSelect, isRoot, isTransaparent, isExpanded }: CardProps) => {
    const {
        name,
        status,
        description,
        parameters,
        startTime,
        finishTime
    } = action;
    const rootClassName = createSelector(
        "action-card",
        status.status,
        isRoot && !isSelected ? "root" : null,
        isSelected ? "selected" : null
    ), headerClassName = createSelector(
        "action-card-header",
        status.status,
        isTransaparent && !isSelected ? "transparent" : null
    ), inputParametersClassName = createSelector(
        "action-card-body-params",
        isTransaparent && !isSelected ? "transparent" : null
    );


    const elapsedTime = getSecondsPeriod(startTime, finishTime);

    const clickHandler = e => {
        if (!onSelect) return;
        onSelect(action);
        // here we cancel handling by parent divs
        e.cancelBubble = true;
    };

    return (
        <div class={rootClassName}
            onClick={clickHandler}
            key={action.id}>
            <ExpandablePanel
                isExpanded={isExpanded}>
                <div class={headerClassName}>
                    <div class="action-card-header-title">
                        <div class="action-card-header-title-name">
                            <h3>{name}</h3>
                        </div>
                        <div class="action-card-header-title-description">
                            <h3>{description}</h3>
                        </div>
                    </div>
                    <div class="action-card-header-time-start">
                        <span>Start</span>
                        <p>{formatTime(action.startTime)}</p>
                    </div>
                    <div class="action-card-header-time-elapsed">
                        <h3>{elapsedTime}</h3>
                    </div>
                    <div class="action-card-header-controls">
                        <div class="action-card-header-controls-status">
                            <h3>{action.status.status.toUpperCase()}</h3>
                        </div>
                        {
                            action.relatedMessages.length > 0 ? (
                                <div class="action-card-header-controls-chips">
                                    <Chip
                                        count={action.relatedMessages.length}/>
                                </div>
                            ) : null
                        }
                    </div>
                </div>
                <div class="action-card-body">
                    <div class={inputParametersClassName}>
                        <ExpandablePanel>
                            <h4>Input parameters</h4>
                            <ParamsTable
                                params={parameters}
                                name={name} />
                        </ExpandablePanel>
                    </div>
                    {
                        // rendering inner nodes
                        children
                    }
                    {
                        action.status.status == 'FAILED' ? (
                            <div class="action-card-status">
                                <ExpandablePanel>
                                    <h4>Status</h4>
                                    <ExceptionChain exception={action.status.cause} />
                                </ExpandablePanel>
                            </div>
                        ) : null
                    }
                </div>
            </ExpandablePanel>
        </div>)
}