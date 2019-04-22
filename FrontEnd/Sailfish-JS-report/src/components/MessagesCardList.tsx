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
import AppState from '../state/models/AppState';
import { generateActionsMap } from '../helpers/mapGenerator';
import { Checkpoint } from './Checkpoint';
import { isCheckpoint, isRejected, isAdmin } from '../helpers/messageType';
import { getActions } from '../helpers/actionType';
import { AdminMessageWrapper } from './AdminMessageWrapper';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { messagesHeatmap } from '../helpers/heatmapCreator';
import { selectMessage } from '../actions/actionCreators';

const MIN_CONTROL_BUTTONS_WIDTH = 880;

interface MessagesListStateProps {
    messages: Message[];
    scrolledMessageId: Number;
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

interface MessagesListDispatchProps {
    messageSelectHandler: (message: Message, status: StatusType) => any;
}

interface MessagesListProps extends MessagesListStateProps ,MessagesListDispatchProps {}

export class MessagesCardListBase extends Component<MessagesListProps> {

    private elements: MessageCard[] = [];
    private scrollbar: HeatmapScrollbar;

    constructor(props: MessagesListProps) {
        super(props);
    }

    componentDidUpdate(prevProps: MessagesListProps) {

        if (prevProps.scrolledMessageId != this.props.scrolledMessageId) {
            // disable smooth behaviour only for checkpoint messages
            if (this.props.scrolledMessageId == this.props.selectedCheckpointId) {
                // [Chrome] smooth behavior doesn't work in chrome with multimple elements
                // https://chromium.googlesource.com/chromium/src.git/+/bbf870d971d95af7ae1ee688a7ed100e3787d02b
                this.scrollToMessage(+this.props.scrolledMessageId, false);
            } else {
                this.scrollToMessage(+this.props.scrolledMessageId);
            }
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

    scrollToTop() {
        this.scrollbar && this.scrollbar.scrollToTop();
    }

    getMessageActions(message: Message): Action[] {
        return message.relatedActions.map(
            actionId => this.props.actionsMap.get(actionId));
    }

    render({ messages,selectedMessages, selectedStatus }: MessagesListProps) {

        return (
            <div class="messages">
                <div class="messages-list">
                    <HeatmapScrollbar
                        selectedElements={messagesHeatmap(messages, selectedMessages, selectedStatus)}
                        ref={ref => this.scrollbar = ref}>
                        {messages.map(message => this.renderMessage(message))}
                    </HeatmapScrollbar>
                </div>
            </div>
        );
    }

    private renderMessage(message: Message) {

        const { selectedMessages, selectedStatus, checkpoints, rejectedMessages, selectedCheckpointId, selectedRejectedMessageId, adminMessagesEnabled, messageSelectHandler } = this.props;
        const isSelected = selectedMessages.includes(message.id);

        if (isCheckpoint(message)) {
            return this.renderCheckpoint(message, checkpoints, selectedCheckpointId)
        }

        if (isAdmin(message)) {
            return this.renderAdmin(message, adminMessagesEnabled, selectedRejectedMessageId, isSelected, selectedStatus);
        }

        if (isRejected(message)) {
            return this.renderRejected(message, rejectedMessages, selectedRejectedMessageId, isSelected, selectedStatus);
        }

        return (
            <MessageCard
                ref={element => this.elements[message.id] = element}
                message={message}
                isSelected={isSelected}
                status={isSelected ? selectedStatus : null}
                key={message.id}
                actions={this.getMessageActions(message)}
                selectHandler={messageSelectHandler}
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

    private renderRejected(message: Message, rejectedMessages: Message[], selectedRejectedMessageId: number, isSelected: boolean, selectedStatus: StatusType) {
        const isRejectedSelected = message.id === selectedRejectedMessageId,
            rejectedCount = rejectedMessages.indexOf(message) + 1;

        return (
            <MessageCard
                ref={element => this.elements[message.id] = element}
                message={message}
                isSelected={isRejectedSelected || isSelected}
                key={message.id}
                actions={this.getMessageActions(message)}
                rejectedMessagesCount={rejectedCount} 
                status={isSelected ? selectedStatus : null}
                selectHandler={this.props.messageSelectHandler}/>
        )
    }

    private renderAdmin(message: Message, adminMessagesEnabled: boolean, selectedRejectedMessageId: number, isSelected: boolean, selectedStatus: StatusType) {
        const isRejectedSelected = message.id == selectedRejectedMessageId;
        
        return (
            <AdminMessageWrapper
                ref={ref => this.elements[message.id] = ref}
                message={message}
                key={message.id}
                actions={this.getMessageActions(message)}
                isExpanded={adminMessagesEnabled}
                isSelected={isSelected || isRejectedSelected}
                status={isSelected ? selectedStatus : null} 
                selectHandler={this.props.messageSelectHandler}/>
        )
    }
}

export const MessagesCardList = connect(
    (state: AppState): MessagesListStateProps => ({
        messages: state.selected.testCase.messages,
        scrolledMessageId: state.selected.scrolledMessageId,
        checkpoints: state.selected.testCase.messages.filter(isCheckpoint),
        rejectedMessages: state.selected.testCase.messages.filter(isRejected),
        selectedMessages: state.selected.messagesId,
        selectedCheckpointId: state.selected.checkpointMessageId,
        selectedRejectedMessageId: state.selected.rejectedMessageId,
        selectedStatus: state.selected.status,
        adminMessagesEnabled: state.view.adminMessagesEnabled,
        actionsMap: state.selected.actionsMap
    }),
    (dispatch): MessagesListDispatchProps => ({
        messageSelectHandler: (message: Message, status: StatusType) => dispatch(selectMessage(message, status))
    }),
    null,
    {
        withRef: true
    }
)(MessagesCardListBase);