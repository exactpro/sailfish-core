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
import Action from '../models/Action';
import { StatusType } from '../models/Status';
import { connect } from 'preact-redux';
import AppState from '../state/models/AppState';
import { Checkpoint } from './Checkpoint';
import { isCheckpoint, isRejected, isAdmin } from '../helpers/messageType';
import { getActions } from '../helpers/actionType';
import { AdminMessageWrapper } from './AdminMessageWrapper';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { messagesHeatmap } from '../helpers/heatmapCreator';
import { selectMessage } from '../actions/actionCreators';
import { MessagesVirtualizedList } from './MessagesVirtualizedList';
import MessageCardExpandState from '../models/view/MessageCardExpandState';

interface MessagesListStateProps {
    messages: Message[];
    scrolledMessageId: Number;
    checkpoints: Message[];
    rejectedMessages: Message[];
    selectedCheckpointId: number;
}

interface MessagesListDispatchProps {
    messageSelectHandler: (message: Message, status: StatusType) => any;
}

interface MessagesListProps extends MessagesListStateProps ,MessagesListDispatchProps {}

export class MessagesCardListBase extends Component<MessagesListProps> {

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
        //const selectedMessageId = this.props.selectedMessages[0];

        // https://stackoverflow.com/questions/26556436/react-after-render-code/28748160#comment64053397_34999925
        // At his point (componentDidMount) DOM havn't fully rendered, so, we calling RAF twice:
        // At this point React passed components tree to DOM, however it still could be not redered.
        // First callback will be called before actual render
        // Second callback will be called when DOM is fully rendered.
        // window.requestAnimationFrame(() => {
        //     window.requestAnimationFrame(() => {
        //         // smooth behavior doesn't work here because render is not complete yet
        //         this.scrollToMessage(selectedMessageId, false);
        //     });
        // });
    }

    scrollToMessage(messageId: number, isSmooth: boolean = true) {
        // TODO: implement scroll to feature using react-virtualized
    }

    scrollToTop() {
        this.scrollbar && this.scrollbar.scrollToTop();
    }

    render({ messages }: MessagesListProps) {

        return (
            <div class="messages-list">
                <MessagesVirtualizedList
                    messagesCount={messages.length}
                    messageRenderer={(index, ...renderProps) => this.renderMessage(messages[index], ...renderProps)}/>
            </div>
        );
    }

    private renderMessage(message: Message, expandState: MessageCardExpandState, messageStateHandler: (nextState: MessageCardExpandState) => any) {

        const { checkpoints, rejectedMessages, selectedCheckpointId } = this.props;

        if (isCheckpoint(message)) {
            return this.renderCheckpoint(message, checkpoints, selectedCheckpointId)
        }

        if (isAdmin(message)) {
            return this.renderAdmin(message, expandState, messageStateHandler);
        }

        if (isRejected(message)) {
            return this.renderRejected(message, rejectedMessages, expandState.showRaw, showRaw => messageStateHandler({ ...expandState, showRaw: showRaw }));
        }

        return (
            <MessageCard
                message={message}
                showRaw={expandState.showRaw}
                showRawHandler={showRaw => messageStateHandler({ showRaw: showRaw })}
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
                description={description} />
        )
    }

    private renderRejected(message: Message, rejectedMessages: Message[], showRaw: boolean, showRawHandler: (showRaw: boolean) => any) {
        const rejectedCount = rejectedMessages.indexOf(message) + 1;

        return (
            <MessageCard
                message={message}
                showRaw={showRaw}
                showRawHandler={showRawHandler}
                rejectedMessagesCount={rejectedCount} />
        )
    }

    private renderAdmin(message: Message, expandState: MessageCardExpandState, messageStateHandler: (nextState: MessageCardExpandState) => any) {        
        return (
            <AdminMessageWrapper
                message={message}
                key={message.id}
                showRaw={expandState.showRaw}
                showRawHandler={showRaw => messageStateHandler({ ...expandState, showRaw: showRaw })}
                isExpanded={expandState.adminExpanded}
                expandHandler={isExpanded => messageStateHandler({ ...expandState, adminExpanded: isExpanded })}
            />
        )
    }
}

export const MessagesCardList = connect(
    (state: AppState): MessagesListStateProps => ({
        messages: state.selected.testCase.messages,
        scrolledMessageId: state.selected.scrolledMessageId,
        checkpoints: state.selected.testCase.messages.filter(isCheckpoint),
        rejectedMessages: state.selected.testCase.messages.filter(isRejected),
        selectedCheckpointId: state.selected.checkpointMessageId
    }),
    (dispatch): MessagesListDispatchProps => ({
        messageSelectHandler: (message: Message, status: StatusType) => dispatch(selectMessage(message, status))
    }),
    null,
    {
        withRef: true
    }
)(MessagesCardListBase);
