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
import '../../styles/messages.scss';
import Message from '../../models/Message';
import { MessageCard } from './MessageCard';
import { StatusType } from '../../models/Status';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { CheckpointMessage } from '../Checkpoint';
import { isCheckpoint, isAdmin } from '../../helpers/messageType';
import { AdminMessageWrapper } from './AdminMessageWrapper';
import { selectMessage } from '../../actions/actionCreators';
import { MessagesVirtualizedList } from './MessagesVirtualizedList';
import MessageCardExpandState from '../../models/view/MessageCardExpandState';
import { messagesHeatmap } from '../../helpers/heatmapCreator';

interface MessagesListStateProps {
    messages: Message[];
    scrolledMessageId: Number;
    selectedCheckpointId: number;
    selectedMessages: number[];
    selectedStatus: StatusType;
}

interface MessagesListDispatchProps {
    messageSelectHandler: (message: Message, status: StatusType) => any;
}

interface MessagesListProps extends MessagesListStateProps ,MessagesListDispatchProps {}

interface MessagesListState {
    // Number objects is used here because in some cases (eg one message / action was selected several times by diferent entities)
    // We can't understand that we need to scroll to the selected entity again when we are comparing primitive numbers.
    // Objects and reference comparison is the only way to handle numbers changing in this case.
    scrolledIndex: Number;
}

export class MessagesCardListBase extends React.PureComponent<MessagesListProps, MessagesListState> {

    constructor(props: MessagesListProps) {
        super(props);

        this.state = {
            scrolledIndex: this.getScrolledIndex(props.scrolledMessageId, props.messages)
        }
    }

    scrollToTop() {
        this.setState({
            scrolledIndex: new Number(0)
        });
    }

    componentWillReceiveProps(nextProps: MessagesListProps) {
        if (this.props.scrolledMessageId !== nextProps.scrolledMessageId && nextProps.scrolledMessageId != null) {
            this.setState({ 
                scrolledIndex: this.getScrolledIndex(nextProps.scrolledMessageId, nextProps.messages)
            });
        }
    }

    private getScrolledIndex(scrolledMessageId: Number, messages: Message[]): Number {
        const scrolledIndex = messages.findIndex(message => message.id === +scrolledMessageId);

        return scrolledIndex !== -1 ? new Number(scrolledIndex) : null;
    }

    render() {
        const { messages, selectedMessages, selectedStatus } = this.props,
            { scrolledIndex } = this.state;

        return (
            <div className="messages-list">
                <MessagesVirtualizedList
                    selectedElements={messagesHeatmap(messages, selectedMessages, selectedStatus)}
                    messagesCount={messages.length}
                    scrolledIndex={scrolledIndex}
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
        selectedCheckpointId: state.selected.checkpointMessageId,
        selectedMessages: state.selected.messagesId,
        selectedStatus: state.selected.status
    }),
    (dispatch): MessagesListDispatchProps => ({
        messageSelectHandler: (message: Message, status: StatusType) => dispatch(selectMessage(message, status))
    }),
    (stateProps, dispatchProps, ownProps) => ({ ...stateProps, ...dispatchProps, ...ownProps}),
    {
        forwardRef: true
    }
)(MessagesCardListBase);
