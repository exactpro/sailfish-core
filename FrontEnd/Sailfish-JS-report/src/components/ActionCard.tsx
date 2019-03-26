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
import "../styles/action.scss";
import { getSecondsPeriod } from "../helpers/dateFormatter";
import { ExceptionChain } from "./ExceptionChain";

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
    const rootClassName = [
            "action-card",
            status.status,
            (isRoot && !isSelected ? "root" : null),
            (isSelected ? "selected" : null)
        ].join(' ').toLowerCase(),
        headerClassName = [
            "action-card-header",
            status.status,
            (isTransaparent && !isSelected ? "transparent" : null)
        ].join(' ').toLowerCase(),
        inputParametersClassName = [
            "action-card-body-params",
            (isTransaparent && !isSelected ? "transparent" : null)
        ].join(' ').toLowerCase();


    const time = getSecondsPeriod(startTime, finishTime);

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
                    <div class="action-card-header-name">
                        <h3>{name}</h3>
                        <p>{description}</p>
                    </div>
                    <div class="action-card-header-status">
                        <h3>{status.status.toUpperCase()}</h3>
                        <h3>{time}</h3>
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
                        // rendering inner actions
                        {children}
                    }
                    <div class="action-card-status">
                        <ExpandablePanel>
                            <h4>Status</h4>
                            <ExceptionChain exception = {action.status.cause}/>
                        </ExpandablePanel>
                    </div>
                </div>
            </ExpandablePanel>
        </div>)
}