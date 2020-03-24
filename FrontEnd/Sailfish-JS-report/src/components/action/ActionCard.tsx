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
import { RecoverableExceptionChain } from "../ExceptionChain";
import { createBemBlock, createBemElement } from '../../helpers/styleCreators';
import { keyForAction } from '../../helpers/keys';
import { StatusType } from '../../models/Status';
import { stopPropagationHandler } from '../../helpers/react';
import { isVerification } from "../../models/Verification";
import Message from '../../models/Message';
import PanelArea from "../../util/PanelArea";
import ActionCardHeader from "./ActionCardHeader";

interface CardProps {
    action: Action;
    children?: React.ReactNodeArray;
    panelArea: PanelArea;
    isSelected: boolean;
    isRoot: boolean;
    isTransparent: boolean;
    isExpanded: boolean;
    onSelect: (action: Action) => void;
    onRootExpand: (isExpanded: boolean) => void;
}

export function ActionCard(props: CardProps) {
    const {
        action,
        children,
        isSelected,
        onSelect,
        isRoot,
        isTransparent,
        isExpanded,
        panelArea,
        onRootExpand
    } = props;

    const { id, name, status, parameters } = action;

    const isEmptyParameters = parameters === null,
        isSingleEmptyMessage = !isEmptyParameters &&
            parameters.length == 1 &&
            parameters[0].name === "Message" &&
            parameters[0].value === "" &&
            parameters[0].subParameters.length == 0,
        showParams = !isEmptyParameters && !isSingleEmptyMessage,
        isDisabled = !showParams &&
            !action.isTruncated &&
            !children.length &&
            !(action.status.status == StatusType.FAILED && action.status.cause != null);

    const rootClassName = createBemBlock(
        "action-card",
        status.status,
        isRoot && !isSelected ? "root" : null,
        isSelected ? "selected" : null
    );

    const inputParametersClassName = createBemElement(
        "ac-body",
        "input-params",
        isTransparent && !isSelected ? "transparent" : null
    );

    return (
        <div className={rootClassName}
             onClick={stopPropagationHandler(onSelect, action)}
             key={action.id}>
            <ExpandablePanel
                isExpandDisabled={isDisabled}
                isExpanded={isExpanded}
                onExpand={isExpanded => onRootExpand(isExpanded)}>
                {
                    toggleExpand => (
                        <ActionCardHeader
                            action={action}
                            toggleExpand={toggleExpand}
                            panelArea={panelArea}
                            isTransparent={isTransparent}
                            isSelected={isSelected}
                            isExpanded={isExpanded}/>
                    )
                }
                <div className="ac-body">
                    {
                        showParams &&
                        <div className={inputParametersClassName}>
                            <SearchExpandablePanel
                                searchKeyPrefix={keyForAction(id, 'parameters')}
                                stateKey={keyForAction(id, 'parameters')}>
                                {
                                    toggleExpand => (
                                        <div className="ac-body__item-title"
                                             onClick={stopPropagationHandler(toggleExpand)}>
                                            Input parameters
                                        </div>
                                    )
                                }
                                <ParamsTable
                                    actionId={action.id}
                                    stateKey={action.id + '-input-params-nodes'}
                                    params={parameters}
                                    name={name}/>
                            </SearchExpandablePanel>
                        </div>
                    }
                    {
                        action.isTruncated &&
                        <div className="ac-body__truncated-warning">
                            {action.verificationCount - action.subNodes.filter(node => isVerification(node)).length} verifications
                            were truncated
                        </div>
                    }
                    {
                        // rendering inner nodes
                        children
                    }
                    {
                        action.status.status == StatusType.FAILED && action.status.cause != null &&
                        <div className="action-card-status">
                            <RecoverableExpandablePanel
                                stateKey={keyForAction(id, 'status')}>
                                {
                                    toggleExpand => (
                                        <div className="ac-body__item-title"
                                             onClick={stopPropagationHandler(toggleExpand)}>
                                            Status
                                        </div>
                                    )
                                }
                                <RecoverableExceptionChain
                                    exception={action.status.cause}
                                    stateKey={`${keyForAction(id, 'status')}-exception`}/>
                            </RecoverableExpandablePanel>
                        </div>
                    }
                </div>
            </ExpandablePanel>
        </div>
    )
}
