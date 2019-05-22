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
import Action from "../../models/Action";
import ParamsTable, { RecoverableParamsTable } from "./ParamsTable";
import { RecoverableExpandablePanel, ExpandablePanel } from "../ExpandablePanel";
import { StatusType } from "../../models/Status";
import "../../styles/action.scss";
import { getSecondsPeriod, formatTime } from "../../helpers/dateFormatter";
import { ExceptionChain } from "../ExceptionChain";
import { Chip } from "../Chip";
import { createSelector } from '../../helpers/styleCreators';

interface CardProps {
    action: Action;
    children?: JSX.Element[];
    isSelected?: boolean;
    isRoot?: boolean;
    isTransaparent?: boolean;
    isExpanded?: boolean;
    ref?: Function;
    onSelect?: (action: Action) => void;
    onExpand: () => void;
    onRootExpand: (isExpanded: boolean) => void;
}

export const ActionCard = ({ action, children, isSelected, onSelect, isRoot, isTransaparent, isExpanded, onExpand, onRootExpand }: CardProps) => {
    const {
        matrixId,
        serviceName,
        name,
        messageType,
        status,
        description,
        parameters,
        startTime,
        finishTime,
        outcome
    } = action;
    const rootClassName = createSelector(
        "action-card",
        status.status,
        isRoot && !isSelected ? "root" : null,
        isSelected ? "selected" : null
    ), headerClassName = createSelector(
        "ac-header",
        status.status,
        isTransaparent && !isSelected ? "transparent" : null
    ), inputParametersClassName = createSelector(
        "ac-body__input-params",
        isTransaparent && !isSelected ? "transparent" : null
    );


    const elapsedTime = getSecondsPeriod(startTime, finishTime);

    const clickHandler = (e: React.MouseEvent) => {
        if (!onSelect) return;
        onSelect(action);
        // here we cancel handling by parent divs
        e.stopPropagation();
    };

    return (
        <div className={rootClassName}
            onClick={clickHandler}
            key={action.id}>
            <ExpandablePanel
                isExpanded={isExpanded}
                onExpand={isExpanded => onRootExpand(isExpanded)}>
                <div className={headerClassName}>
                    <div className="ac-header__title">
                        <div className="ac-header__name">
                            <h3>{matrixId} {serviceName} {name} {messageType}</h3>
                        </div>
                        <div className="ac-header__description">
                            <h3>{description} {outcome}</h3>
                        </div>
                    </div>
                    {
                        action.startTime ? (
                            <div className="ac-header__start-time">
                                <span>Start</span>
                                <p>{formatTime(action.startTime)}</p>
                            </div>
                        ) : null
                    }
                    <div className="ac-header__elapsed-time">
                        <h3>{elapsedTime}</h3>
                    </div>
                    <div className="ac-header__controls">
                        <div className="ac-header__status">
                            <h3>{action.status.status.toUpperCase()}</h3>
                        </div>
                        {
                            action.relatedMessages.length > 0 ? (
                                <div className="ac-header__chips">
                                    <Chip
                                        count={action.relatedMessages.length}/>
                                </div>
                            ) : null
                        }
                    </div>
                </div>
                <div className="ac-body">
                    <div className={inputParametersClassName}>
                        <RecoverableExpandablePanel
                            stateKey={action.id + '-input-params'}
                            onExpand={onExpand}>
                            <div className="ac-body__item-title">Input parameters</div>
                            <RecoverableParamsTable
                                stateKey={action.id + '-input-params-nodes'}
                                params={parameters}
                                name={name} 
                                onExpand={onExpand} />
                        </RecoverableExpandablePanel>
                    </div>
                    {
                        // rendering inner nodes
                        children
                    }
                    {
                        action.status.status == 'FAILED' ? (
                            <div className="action-card-status">
                                <RecoverableExpandablePanel
                                    stateKey={action.id + '-status'}
                                    onExpand={onExpand}>
                                    <div className="ac-body__item-title">Status</div>
                                    <ExceptionChain exception={action.status.cause} />
                                </RecoverableExpandablePanel>
                            </div>
                        ) : null
                    }
                </div>
            </ExpandablePanel>
        </div>
    )
}
