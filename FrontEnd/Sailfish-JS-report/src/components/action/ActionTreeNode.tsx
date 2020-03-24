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
import '../../styles/action.scss';
import Action, { ActionNode, ActionNodeType } from '../../models/Action';
import { StatusType } from '../../models/Status';
import Tree from '../../models/util/Tree';
import ActionExpandStatus from '../../models/util/ActionExpandStatus';
import { ActionCard } from './ActionCard';
import { CustomMessage } from './CustomMessage';
import VerificationCard from './VerificationCard';
import UserTableCard from './UserTableCard';
import { isCheckpointAction } from '../../helpers/action';
import { getSubTree } from '../../helpers/tree';
import CheckpointAction from './CheckpointAction';
import CustomLink from './CustomLink';
import { keyForAction, keyForVerification } from "../../helpers/keys";
import PanelArea from "../../util/PanelArea";

interface Props {
    action: ActionNode;
    panelArea: PanelArea;
    isRoot?: boolean;
    parentAction?: Action | null;
    selectedActionsId: number[];
    selectedVerificationId: number;
    expandPath: Tree<ActionExpandStatus>;
    filter: {
        results: string[];
        isActive: boolean;
        isTransparent: boolean;
    }
    onRootExpand: (actionId: number, isExpanded: boolean) => void;
    onActionSelect: (action: Action) => void;
    onVerificationSelect: (messageId: number, rootActionId: number, status: StatusType) => void;
}

export default function ActionTreeNode(props: Props) {
    const {
        action,
        isRoot = false,
        parentAction = null,
        panelArea,
        onActionSelect,
        onVerificationSelect,
        selectedActionsId,
        selectedVerificationId,
        onRootExpand,
        expandPath,
        filter
    } = props;

    // https://www.typescriptlang.org/docs/handbook/advanced-types.html#discriminated-unions
    switch (action.actionNodeType) {
        case ActionNodeType.ACTION: {
            if (isCheckpointAction(action)) {
                return (
                    <CheckpointAction
                        action={action}/>
                );
            }

            let isTransparent = false;

            if (filter.isActive && !filter.results.includes(keyForAction(action.id))) {
                if (filter.isTransparent) {
                    isTransparent = true;
                } else {
                    return null;
                }
            }

            return (
                <ActionCard
                    action={action}
                    panelArea={panelArea}
                    isSelected={selectedActionsId.includes(action.id)}
                    isTransparent={isTransparent}
                    onSelect={onActionSelect}
                    isRoot={isRoot}
                    isExpanded={expandPath.value.isExpanded}
                    onRootExpand={isExpanded => onRootExpand(action.id, isExpanded)}>
                    {
                        action.subNodes?.map((childAction, index) => (
                            <ActionTreeNode
                                {...props}
                                isRoot={false}
                                key={index}
                                parentAction={action}
                                action={childAction}
                                expandPath={getSubTree(childAction, expandPath)}/>
                        ))
                    }
                </ActionCard>
            );
        }

        case ActionNodeType.VERIFICATION: {
            const verification = action,
                isSelected = verification.messageId === selectedVerificationId;

            let isTransparent = false;

            if (filter.isActive && !filter.results.includes(keyForVerification(parentAction?.id, verification.messageId))) {
                if (filter.isTransparent) {
                    isTransparent = true;
                } else {
                    return null;
                }
            }

            return (
                <VerificationCard
                    key={verification.messageId}
                    verification={verification}
                    isSelected={isSelected}
                    isTransparent={isTransparent}
                    onSelect={onVerificationSelect}
                    parentActionId={parentAction?.id}/>
            )
        }

        case ActionNodeType.CUSTOM_MESSAGE: {
            return (
                <CustomMessage
                    userMessage={action}
                    parent={parentAction}/>
            );
        }

        case ActionNodeType.LINK: {
            return (
                <CustomLink action={action}/>
            );
        }

        case ActionNodeType.TABLE: {
            return (
                <UserTableCard
                    table={action}
                    parent={parentAction}/>
            );
        }

        default: {
            console.warn("WARNING: unknown action node type");
            return <div/>;
        }
    }
}
