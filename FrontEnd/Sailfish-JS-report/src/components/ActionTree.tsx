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
import { Checkpoint } from './Checkpoint';


export interface ActionTreeProps {
    action: ActionNode;
    actionSelectHandler: (action: Action) => any;
    messageSelectHandler: (id: number, status: StatusType) => any;
    checkpointSelectHandler: (action: Action) => any;
    selectedMessageId: number;
    selectedActionId: number;
    selectedCheckpointId: number;
    checkpoints: Action[];
    actionsFilter: StatusType[];
    filterFields: StatusType[];
}

export class ActionTree extends Component<ActionTreeProps> {

    private expandedTreePath = null;
    private treeElements: Component[] = []; 

    componentWillMount() {
        if (this.props.action.actionNodeType == "action") {
            this.expandedTreePath = this.getExpandedTreePath(this.props.action as Action, [], this.props.selectedActionId);
        }
    }

    // scrolling to action, selected by url sharing
    componentDidMount() {
        if (!this.treeElements[this.props.selectedActionId]) {
            return;
        }

        // https://stackoverflow.com/questions/26556436/react-after-render-code/28748160#comment64053397_34999925
        // At his point (componentDidMount) DOM havn't fully rendered, so, we calling RAF twice:
        // At this point React passed components tree to DOM, however it still could be not redered.
        // First callback will be called before actual render
        // Second callback will be called when DOM is fully rendered.
        window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => {
                this.scrollToAction(this.props.selectedActionId);
            });
        });
    }

    componentWillReceiveProps(nextProps: ActionTreeProps) {
        // handling action change to update expand tree
        if (this.props.action !== nextProps.action && nextProps.action.actionNodeType == "action") {
            this.expandedTreePath = this.getExpandedTreePath(this.props.action as Action, [], this.props.selectedActionId);
        } else {
            this.expandedTreePath = null
        }
    }

    componentDidUpdate(prevProps: ActionTreeProps) {
        if (prevProps.selectedCheckpointId != this.props.selectedCheckpointId) {
            this.scrollToAction(this.props.selectedCheckpointId);
        }
    }

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
        // the first condition - current action is selected and we should update to show it
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

        // same as first if statement, but with checkpoint
        if (nextProps.checkpoints.includes(action) && nextProps.selectedCheckpointId !== prevProps.selectedCheckpointId && 
            (nextProps.selectedCheckpointId === action.id || prevProps.selectedCheckpointId === action.id)) {
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

    updateTreePath() {
        if (this.props.action.actionNodeType == "action") {
            this.expandedTreePath = this.getExpandedTreePath(this.props.action as Action, [], this.props.selectedActionId);
        } else {
            this.expandedTreePath = [];
        }
    }

    getExpandedTreePath(action: Action, treePath: number[], targetActionId: number): number[] {

        const actionTreePath = [...treePath, action.id]; 

        if (action.id == targetActionId) {
            return actionTreePath;
        }

        if (action.subNodes) {
            for (let i = 0; i < action.subNodes.length; i++) {
                const subNode = action.subNodes[i];

                if (subNode.actionNodeType == "action") {
                    const subNodePath = this.getExpandedTreePath(subNode as Action, actionTreePath, targetActionId);

                    if (subNodePath.includes(targetActionId)) {
                        // target action found in one of subNodes
                        return subNodePath;
                    }
                }
            }
        }

        // we did't find any sub node with the given action id
        return treePath;
    }

    scrollToAction(actionId: number) {
        if (this.treeElements[actionId]) {
            this.treeElements[actionId].base.scrollIntoView({block: 'center'});
        }
    }

    render(props: ActionTreeProps): JSX.Element {
        return this.renderNode(props, true, this.expandedTreePath);
    }

    renderNode(props: ActionTreeProps, isRoot = false, expandTreePath: number[] = null): JSX.Element {
        const { actionSelectHandler, messageSelectHandler, selectedActionId, selectedMessageId, selectedCheckpointId, actionsFilter, filterFields, checkpoints, checkpointSelectHandler } = props;

        switch (props.action.actionNodeType) {
            case 'action': {
                const action = props.action as Action;

                if (checkpoints.includes(action)) {
                    return this.renderCheckpoint(props, action);
                }

                const isExpanded = expandTreePath ? expandTreePath[0] == action.id : null,
                    newTreePath = expandTreePath ? expandTreePath.slice(1) : null;

                return (
                    <ActionCard action={action}
                        isSelected={action.id === selectedActionId}
                        isTransaparent={!actionsFilter.includes(action.status.status)}
                        onSelect={actionSelectHandler}
                        isRoot={isRoot}
                        isExpanded={isExpanded}
                        ref={ref => this.treeElements[action.id] = ref }>
                        {
                            action.subNodes ? action.subNodes.map(
                                action => this.renderNode({...props, action: action}, false, newTreePath)) : null
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
        // italic style value - only for fontStyle css property
        //bold style value - only for fontWeight css property
        const messageStyle = {
            color: (color || "").toLowerCase(),
            fontStyle: (style || "").toLowerCase(),
            fontWeight: (style || "").toLowerCase()
        };

        if (exception) {
            return (
                <div class="action-card">
                    <ExpandablePanel>
                        <div class="action-card-header">
                            <h3 style={messageStyle}>{message} - {level}</h3>
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
                        <h3 style={messageStyle}>{message} - {level}</h3>
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

    renderCheckpoint({checkpoints, selectedCheckpointId, checkpointSelectHandler}: ActionTreeProps, action: Action) {
        const checkpointIndex = checkpoints.indexOf(action) + 1,
            isSelected = selectedCheckpointId == action.id;

        return (
            <Checkpoint
                name={action.name}
                count={checkpointIndex}
                isSelected={isSelected}
                clickHandler={() => checkpointSelectHandler(action)}
                ref={ref => this.treeElements[action.id] = ref}/>
        )
    }
}