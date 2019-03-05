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
import '../styles/messages.scss';
import Message from '../models/Message';
import { MessageCard } from './MessageCard';
import Action, { ActionNodeType } from '../models/Action';
import { StatusType, statusValues } from '../models/Status';
import { connect } from 'preact-redux';
import AppState from '../state/AppState';
import { generateActionsMap } from '../helpers/mapGenerator';
import { Checkpoint } from './Checkpoint';
import { isCheckpoint, isRejected, isAdmin } from '../helpers/messageType';
import { getActions } from '../helpers/actionType';
import { AdminMessageWrapper } from './AdminMessageWrapper';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { messagesHeatmap } from '../helpers/heatmapCreator';

const MIN_CONTROL_BUTTONS_WIDTH = 880;

interface MessagesListProps {
    messages: Message[];
    checkpoints: Message[];
    rejectedMessages: Message[];
    adminMessagesEnabled: boolean;
    selectedMessages: number[];
    selectedCheckpointId: number;
    selectedRejectedMessageId: number;
    actionsMap: Map<number, Action>;
    selectedStatus: StatusType;
    panelWidth?: number;
}

export class MessagesCardListBase extends Component<MessagesListProps> {

    private elements: MessageCard[] = [];

    constructor(props: MessagesListProps) {
        super(props);
    }

    componentDidUpdate(prevProps: MessagesListProps) {
        if (prevProps.selectedMessages !== this.props.selectedMessages) {
            this.scrollToMessage(this.props.selectedMessages[0]);
        }

        if (prevProps.selectedCheckpointId !== this.props.selectedCheckpointId) {
            // [Chrome] smooth behavior doesn't work in chrome with multimple elements
            // https://chromium.googlesource.com/chromium/src.git/+/bbf870d971d95af7ae1ee688a7ed100e3787d02b
            this.scrollToMessage(this.props.selectedCheckpointId, false);
        }

        if (prevProps.selectedRejectedMessageId !== this.props.selectedRejectedMessageId) {
            this.scrollToMessage(this.props.selectedRejectedMessageId);
        }
    }

    componentDidMount() {
        const selectedMessageId = this.props.selectedMessages[0];

        // https://stackoverflow.com/questions/26556436/react-after-render-code/28748160#comment64053397_34999925
        // At his point (componentDidMount) DOM havn't fully rendered, so, we calling RAF twice:
        // At this point React passed components tree to DOM, however it still could be not redered.
        // First callback will be called before actual render
        // Second callback will be called when DOM is fully rendered.
        window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => {
                // smooth behavior doesn't work here because render is not complete yet
                this.scrollToMessage(selectedMessageId, false);
            });
        });
    }

    scrollToMessage(messageId: number, isSmooth: boolean = true) {
        if (this.elements[messageId]) {
            this.elements[messageId].base.scrollIntoView({ 
                block: 'center', 
                behavior: isSmooth ? 'smooth' : 'auto',
                inline: 'nearest' 
            });
        }
    }

    getMessageActions(message: Message): Map<number, Action> {
        return new Map<number, Action>(message.relatedActions.map(
            (actionId): [number, Action] => [actionId, this.props.actionsMap.get(actionId)]));
    }

    render({ messages,selectedMessages, selectedStatus }: MessagesListProps) {

        return (
            <div class="messages">
                <div class="messages-list">
                    <HeatmapScrollbar
                        selectedElements={messagesHeatmap(messages, selectedMessages, selectedStatus)}>
                        {messages.map(message => this.renderMessage(message))}
                    </HeatmapScrollbar>
                </div>
            </div>
        );
    }

    private renderMessage(message: Message) {

        const { selectedMessages, selectedStatus, checkpoints, rejectedMessages, selectedCheckpointId, selectedRejectedMessageId, adminMessagesEnabled } = this.props;

        if (isCheckpoint(message)) {
            return this.renderCheckpoint(message, checkpoints, selectedCheckpointId)
        }

        if (isAdmin(message)) {
            return (
                <AdminMessageWrapper
                    ref={ref => this.elements[message.id] = ref}
                    message={message}
                    key={message.id}
                    actionsMap={this.getMessageActions(message)}
                    isExpanded={adminMessagesEnabled}
                    isSelected={selectedRejectedMessageId === message.id} />
            )
        }

        if (isRejected(message)) {
            return this.renderRejected(message, rejectedMessages, selectedRejectedMessageId);
        }

        const isSelected = selectedMessages.includes(message.id);

        return (
            <MessageCard
                ref={element => this.elements[message.id] = element}
                message={message}
                isSelected={isSelected}
                status={isSelected ? selectedStatus : null}
                key={message.id}
                actionsMap={this.getMessageActions(message)}
            />
        );
    }

    private renderCheckpoint(message: Message, checkpoints: Message[], selectedCheckpointId: number) {
        const isSelected = message.id === selectedCheckpointId,
            checkpointCount = checkpoints.indexOf(message) + 1,
            description = message.content["message"] ? message.content["message"]["Description"] : "";

        return (
            <Checkpoint name={message.msgName}
                count={checkpointCount}
                isSelected={isSelected}
                description={description}
                ref={ref => this.elements[message.id] = ref} />
        )
    }

    private renderRejected(message: Message, rejectedMessages: Message[], selectedRejectedMessageId: number) {
        const isSelected = message.id === selectedRejectedMessageId,
            rejectedCount = rejectedMessages.indexOf(message) + 1;

        return (
            <MessageCard
                ref={element => this.elements[message.id] = element}
                message={message}
                isSelected={isSelected}
                key={message.id}
                actionsMap={this.getMessageActions(message)}
                rejectedMessagesCount={rejectedCount} />
        )
    }
}

export const MessagesCardList = connect(
    (state: AppState) => ({
        messages: state.testCase.messages,
        checkpoints: state.testCase.messages.filter(isCheckpoint),
        rejectedMessages: state.testCase.messages.filter(isRejected),
        selectedMessages: state.selected.messagesId,
        selectedCheckpointId: state.selected.checkpointMessageId,
        selectedRejectedMessageId: state.selected.rejectedMessageId,
        selectedStatus: state.selected.status,
        adminMessagesEnabled: state.adminMessagesEnabled,
        actionsMap: generateActionsMap(getActions(state.testCase.actions))
    }),
    dispatch => ({}),
    null,
    {
        withRef: true
    }
)(MessagesCardListBase);