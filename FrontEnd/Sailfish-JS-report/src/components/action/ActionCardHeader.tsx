/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
import PanelArea from "../../util/PanelArea";
import SearchableContent from "../search/SearchableContent";
import { keyForAction } from "../../helpers/keys";
import { formatTime, getSecondsPeriod } from "../../helpers/date";
import { Chip } from "../Chip";
import { ActionMlUploadButton } from "../ActionMlUploadButton";
import { createBemBlock, createBemElement } from "../../helpers/styleCreators";
import { getMinifiedStatus } from "../../helpers/action";

interface Props {
    action: Action;
    toggleExpand: () => void;
    panelArea: PanelArea;
    isTransparent: boolean;
    isSelected: boolean;
    isExpanded: boolean;
}

export default function ActionCardHeader({ action, toggleExpand, panelArea, isSelected, isTransparent, isExpanded }: Props) {
    const {
        id,
        matrixId,
        serviceName,
        name,
        messageType,
        status,
        description,
        startTime,
        finishTime,
        outcome
    } = action;

    const isMinified = panelArea == PanelArea.P25 && !isExpanded;

    const headerClassName = createBemBlock(
        "ac-header",
        panelArea,
        status.status,
        isTransparent && !isSelected ? "transparent" : null,
        isExpanded ? "expanded" : null
    );

    const headerTitleElemClassName = createBemElement(
        'ac-header',
        'name-element',
        isSelected ? 'selected' : null
    );

    const elapsedTime = getSecondsPeriod(startTime, finishTime);
    const nameTitle = isMinified ?
        [matrixId, serviceName, name, messageType].filter(Boolean).join(' | ') :
        'Action name';

    return (
        <div className={headerClassName}>
            <div className="ac-header__title">
                <div className="ac-header__name"
                     onClick={toggleExpand}>
                    {
                        matrixId && !isMinified &&
                        <div className={headerTitleElemClassName} title="Matrix ID">
                            <SearchableContent
                                content={matrixId}
                                contentKey={keyForAction(id, 'matrixId')}/>
                        </div>
                    }
                    {
                        serviceName && !isMinified &&
                        <div className={headerTitleElemClassName} title="Service name">
                            <SearchableContent
                                content={serviceName}
                                contentKey={keyForAction(id, 'serviceName')}/>
                        </div>
                    }
                    {
                        name &&
                        <div className={headerTitleElemClassName}
                             title={nameTitle}>
                            <SearchableContent
                                content={name}
                                contentKey={keyForAction(id, 'name')}/>
                        </div>
                    }
                    {
                        messageType && !isMinified &&
                        <div className={headerTitleElemClassName} title="Message type">
                            <SearchableContent
                                content={messageType}
                                contentKey={keyForAction(id, 'messageType')}/>
                        </div>
                    }
                </div>
                {
                    !isMinified ? (
                        <div className="ac-header__description">
                            <SearchableContent
                                content={description}
                                contentKey={keyForAction(id, 'description')}/>
                            {outcome}
                        </div>
                    ) : null
                }
            </div>
            {
                action.startTime && !isMinified ? (
                    <div className="ac-header__start-time">
                        <div className="ac-header__time-label">Start</div>
                        <div className="ac-header__time-value">
                            {formatTime(action.startTime)}
                        </div>
                    </div>
                ) : null
            }
            <div className="ac-header__elapsed-time">
                {elapsedTime}
            </div>
            <div className="ac-header__controls">
                <div className="ac-header__status">
                    {
                        panelArea == PanelArea.P25 || panelArea == PanelArea.P100 ?
                            getMinifiedStatus(action.status.status) :
                            action.status.status.toUpperCase()
                    }
                </div>
                {
                    action.relatedMessages.length > 0 ? (
                        <div className="ac-header__chips">
                            <Chip
                                text={action.relatedMessages.length.toString()}/>
                        </div>
                    ) : null
                }
                <ActionMlUploadButton actionId={action.id}/>
            </div>
        </div>
    )
}
