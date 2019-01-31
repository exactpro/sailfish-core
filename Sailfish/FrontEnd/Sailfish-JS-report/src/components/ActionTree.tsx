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

import { h, Component } from 'preact';
import Action, { ActionNode } from '../models/Action';
import { ActionTreeProps } from './ActionTree';
import { ActionCard } from './ActionCard';
import { VerificationTable } from "./VerificationTable";
import MessageAction from '../models/MessageAction';
import Verification from '../models/Verification'
import '../styles/action.scss';
import ExpandablePanel from './ExpandablePanel';
import Link from '../models/Link';
import { StatusType } from '../models/Status';


export interface ActionTreeProps {
    action: ActionNode;
    actionSelectHandler: (action: Action) => void;
    messageSelectHandler: (id: number, status: StatusType) => void;
    selectedMessageId: number;
    selectedActionId: number;
    actionsFilter: StatusType[];
    filterFields: StatusType[];
}

export class ActionTree extends Component<ActionTreeProps, any> {

    shouldComponentUpdate(nextProps: ActionTreeProps) {
        if (nextProps.action !== this.props.action) return true;
        
        if (nextProps.action.actionNodeType === "action") {
            const nextAction = nextProps.action as Action;

            if (this.props.filterFields !== nextProps.filterFields) {
                return true;
            }
            if (this.props.actionsFilter !== nextProps.actionsFilter) {
                return true;
            }

            // compare current action id and selected action id
            return this.shouldActionUpdate(nextAction, nextProps, this.props);
        } else {
            return true;
        }
    }

    shouldActionUpdate(action: Action, nextProps: ActionTreeProps, prevProps: ActionTreeProps) : boolean {
        // the first condition - current action is selected and we should update to show id
        // the second condition - current action was selected and we should disable selection
        if (nextProps.selectedActionId === action.id || prevProps.selectedActionId === action.id) {
            return true;
        }

        // here we check verification selection
        if (nextProps.selectedMessageId !== prevProps.selectedMessageId && 
            action.relatedMessages && 
            (action.relatedMessages.some(msgId => msgId == nextProps.selectedMessageId || msgId == prevProps.selectedMessageId))) {
                return true;
        }

        if (action.subNodes) {
            // if at least one of the subactions needs an update, we update the whole action
            return action.subNodes.some(action => {
                    if (action.actionNodeType === "action") {
                        return this.shouldActionUpdate(action as Action, nextProps, prevProps)
                    } else {
                        return false;
                    }
                });
        }

        return false;
    }

    render(props: ActionTreeProps): JSX.Element {
        return this.renderNode(props, true);
    }

    renderNode(props: ActionTreeProps, isRoot = false): JSX.Element {
        const { actionSelectHandler, messageSelectHandler, selectedActionId, selectedMessageId, actionsFilter, filterFields } = props;

        switch (props.action.actionNodeType) {
            case 'action': {
                const action = props.action as Action;
                return (
                    <ActionCard action={action}
                        isSelected={action.id === selectedActionId}
                        isTransaparent={!actionsFilter.includes(action.status.status)}
                        onSelect={actionSelectHandler}
                        isRoot={isRoot}>
                        {
                            action.subNodes ? action.subNodes.map(
                                action => this.renderNode({...props, action: action})) : null
                        }
                    </ActionCard>
                );
            }

            case 'message': {
                const messageAction = props.action as MessageAction;

                return this.renderMessageAction(messageAction);
            }

            case 'verification': {
                const verification = props.action as Verification;
                const isSelected = verification.messageId === selectedMessageId;
                const isTransparent = !actionsFilter.includes(verification.status.status);

                return this.renderVerification(verification, messageSelectHandler, isSelected, isTransparent, filterFields)
            }

            case 'link': {
                const { link } = props.action as Link;

                return (
                    <div class="action-card">
                        <h3>{"Link : " + link}</h3>
                    </div>
                );
            }

            default: {
                return null;
            }
        }
    }

    renderMessageAction({ message, level, exception, color, style }: MessageAction) {
        if (exception) {
            return (
                <div class="action-card">
                    <ExpandablePanel>
                        <div class="action-card-header">
                            {/* italic style value - only for fontStyle css property
                            bold style value - only for fontWeight css property */}
                            <h3 style={{
                                color: (color || "").toLowerCase(),
                                fontStyle: (style || "").toLowerCase(),
                                fontWeight: (style || "").toLowerCase()
                            }}>{message} - {level}</h3>
                        </div>
                        <div class="action-card-body">
                            <pre>{exception && exception.stacktrace}</pre>
                        </div>
                    </ExpandablePanel>
                </div>
            );
        } else {
            return (
                <div class="action-card">
                    <div class="action-card-header">
                        {/* italic style value - only for fontStyle css property
                        bold style value - only for fontWeight css property */}
                        <h3 style={{
                            color: (color || "").toLowerCase(),
                            fontStyle: (style || "").toLowerCase(),
                            fontWeight: (style || "").toLowerCase()
                        }}>{message} - {level}</h3>
                    </div>
                </div>
            );
        }
    }

    renderVerification({ name, status, entries, messageId }: Verification,
        selectHandelr: Function, isSelected: boolean, isTransaparent, filterFields: StatusType[]) {

        const className = ["action-card-body-verification",
            (status ? status.status : ""),
            (isSelected ? "selected" : ""),
            (isTransaparent && !isSelected ? "transparent" : "")].join(' ').toLowerCase();

        return (
            <div class="action-card">
                <div class={className}
                    onClick={e => {
                        selectHandelr(messageId, status.status);
                        // here we cancel handling by parent divs
                        e.cancelBubble = true;
                    }}>
                    <ExpandablePanel>
                        <h4>{"Verification — " + name + " — " + status.status}</h4>
                        <VerificationTable params={entries} />
                    </ExpandablePanel>
                </div>
            </div>
        )
    }
}