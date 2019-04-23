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

import { h } from 'preact';
import '../styles/messages.scss';
import Message from '../models/Message';
import { MessageCard } from './MessageCard';
import { StatusType } from '../models/Status';
import { connect } from 'preact-redux';
import AppState from '../state/models/AppState';
import { CheckpointMessage } from './Checkpoint';
import { isCheckpoint, isAdmin } from '../helpers/messageType';
import { AdminMessageWrapper } from './AdminMessageWrapper';
import { HeatmapScrollbar } from './HeatmapScrollbar';
import { selectMessage } from '../actions/actionCreators';
import { MessagesVirtualizedList } from './MessagesVirtualizedList';
import MessageCardExpandState from '../models/view/MessageCardExpandState';
import PureComponent from '../util/PureComponent';

interface MessagesListStateProps {
    messages: Message[];
    scrolledMessageId: Number;
    selectedCheckpointId: number;
}

interface MessagesListDispatchProps {
    messageSelectHandler: (message: Message, status: StatusType) => any;
}

interface MessagesListProps extends MessagesListStateProps ,MessagesListDispatchProps {}

export class MessagesCardListBase extends PureComponent<MessagesListProps> {

    private scrollbar: HeatmapScrollbar;

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
        // TODO: implement scrollto feature using react-virtualized
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

        if (isCheckpoint(message)) {
            return <CheckpointMessage message={message}/>;
        }

        if (isAdmin(message)) {
            return this.renderAdmin(message, expandState, messageStateHandler);
        }

        return (
            <MessageCard
                message={message}
                showRaw={expandState.showRaw}
                showRawHandler={showRaw => messageStateHandler({ showRaw: showRaw })}
            />
        );
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
