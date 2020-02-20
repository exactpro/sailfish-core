/******************************************************************************
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
import ParamsTable from './ParamsTable';
import { ExpandablePanel, RecoverableExpandablePanel, SearchExpandablePanel } from "../ExpandablePanel";
import "../../styles/action.scss";
import {formatTime, getSecondsPeriod} from "../../helpers/date";
import {RecoverableExceptionChain} from "../ExceptionChain";
import {Chip} from "../Chip";
import {createStyleSelector} from '../../helpers/styleCreators';
import SearchableContent from '../search/SearchableContent';
import { keyForAction } from '../../helpers/keys';
import { ActionMlUploadButton } from "../ActionMlUploadButton";
import { StatusType } from '../../models/Status';
import { stopPropagationHandler } from '../../helpers/react';
import { isVerification } from "../../models/Verification";
import Message from '../../models/Message';
import { isMessage } from '../../models/Message';

interface CardProps {
    action: Action;
    children?: React.ReactNodeArray;
    isSelected?: boolean;
    isRoot?: boolean;
    isTransparent?: boolean;
    isExpanded?: boolean;
    onSelect?: (action: Action) => void;
    onRootExpand: (isExpanded: boolean) => void;
}

export function ActionCard({ action, children, isSelected, onSelect, isRoot, isTransparent, isExpanded, onRootExpand }: CardProps) {
    const {
        id,
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

    const rootClassName = createStyleSelector(
        "action-card",
        status.status,
        isRoot && !isSelected ? "root" : null,
        isSelected ? "selected" : null);

    const headerClassName = createStyleSelector(
        "ac-header",
        status.status,
        isTransparent && !isSelected ? "transparent" : null);

    const inputParametersClassName = createStyleSelector(
        "ac-body__input-params",
        isTransparent && !isSelected ? "transparent" : null);
    
    const headerTitleElemClassName = createStyleSelector(
        'ac-header__name-element',
        isSelected ? 'selected' : null);

    const elapsedTime = getSecondsPeriod(startTime, finishTime);

    const isEmptyParameters = parameters === null,
          isSingleEmptyMessage = !isEmptyParameters && parameters.length == 1 && parameters[0].name === "Message" &&
                                 parameters[0].value === "" && parameters[0].subParameters.length == 0,
          showParams = !isEmptyParameters && !isSingleEmptyMessage

    return (
        <div className={rootClassName}
            onClick={stopPropagationHandler(onSelect, action)}
            key={action.id}>
            <ExpandablePanel
                isExpanded={isExpanded}
                onExpand={isExpanded => onRootExpand(isExpanded)}>
                {
                    toggleExpand => (
                        <div className={headerClassName}>
                            <div className="ac-header__title">
                                <div className="ac-header__name"
                                    onClick={() => toggleExpand()}>
                                    {
                                        matrixId &&
                                        <div className={headerTitleElemClassName} title="Matrix ID">
                                            <SearchableContent
                                                content={matrixId}
                                                contentKey={keyForAction(id, 'matrixId')} />
                                        </div>
                                    }
                                    {
                                        serviceName &&
                                        <div className={headerTitleElemClassName} title="Service name">
                                            <SearchableContent
                                                content={serviceName}
                                                contentKey={keyForAction(id, 'serviceName')} />
                                        </div>
                                    }
                                    {
                                        name &&
                                        <div className={headerTitleElemClassName} title="Action name">
                                            <SearchableContent
                                                content={name}
                                                contentKey={keyForAction(id, 'name')} />
                                        </div>
                                    }
                                    {
                                        messageType &&
                                        <div className={headerTitleElemClassName} title="Message type">
                                            <SearchableContent
                                                content={messageType}
                                                contentKey={keyForAction(id, 'messageType')} />
                                        </div>
                                    }
                                </div>
                                <div className="ac-header__description">
                                    <SearchableContent
                                        content={description}
                                        contentKey={keyForAction(id, 'description')} />
                                    {outcome}
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
                                <span>{elapsedTime}</span>
                            </div>
                            <div className="ac-header__controls">
                                <div className="ac-header__status">
                                    <span>{action.status.status.toUpperCase()}</span>
                                </div>
                                {
                                    action.relatedMessages.length > 0 ? (
                                        <div className="ac-header__chips">
                                            <Chip
                                                text={action.relatedMessages.length.toString()} />
                                        </div>
                                    ) : null
                                }
                                <ActionMlUploadButton actionId={action.id} />
                            </div>
                        </div>
                    )
                }

                <div className="ac-body">
                    {showParams && <div className={inputParametersClassName}>
                        <SearchExpandablePanel
                            searchKeyPrefix={keyForAction(id, 'parameters')}
                            stateKey={keyForAction(id, 'parameters')}>
                            {toggleExpand => (
                                <div className="ac-body__item-title"
                                    onClick={stopPropagationHandler(toggleExpand)}>
                                        Input parameters</div>
                            )}
                            <ParamsTable
                                actionId={action.id}
                                stateKey={action.id + '-input-params-nodes'}
                                params={parameters}
                                name={name} />
                        </SearchExpandablePanel>
                    </div>}

                    {action.isTruncated ? (
                        <div className="ac-body__truncated-warning">
                            {action.verificationCount - action.subNodes.filter(node => isVerification(node)).length} verifications were truncated
                        </div>
                    ) : null}

                    {
                        // rendering inner nodes
                        children
                    }

                    {
                        action.status.status == StatusType.FAILED && action.status.cause != null ? (
                            <div className="action-card-status">
                                <RecoverableExpandablePanel
                                    stateKey={keyForAction(id, 'status')}>
                                    {toggleExpand => (
                                        <div className="ac-body__item-title"
                                            onClick={stopPropagationHandler(toggleExpand)}>
                                                Status</div>
                                    )}
                                    <RecoverableExceptionChain
                                        exception={action.status.cause}
                                        stateKey={`${keyForAction(id, 'status')}-exception`} />
                                </RecoverableExpandablePanel>
                            </div>
                        ) : null
                    }
                </div>
            </ExpandablePanel>
        </div>
    )
}
