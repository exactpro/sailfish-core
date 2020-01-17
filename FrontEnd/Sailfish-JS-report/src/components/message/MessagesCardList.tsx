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
import MessageCard from './MessageCard';
import {StatusType} from '../../models/Status';
import {connect} from 'react-redux';
import AppState from '../../state/models/AppState';
import {isAdmin, isCheckpoint} from '../../helpers/messageType';
import {AdminMessageWrapper} from './AdminMessageWrapper';
import {selectMessage} from '../../actions/actionCreators';
import {messagesHeatmap} from '../../helpers/heatmapCreator';
import StateSaverProvider from '../util/StateSaverProvider';
import {VirtualizedList} from '../VirtualizedList';
import CheckpointMessage from './CheckpointMessage';
import {getFilteredMessages, getTransparentMessages} from "../../selectors/messages";

interface MessagesListStateProps {
    messages: Message[];
    scrolledMessageId: Number;
    selectedMessages: number[];
    selectedStatus: StatusType;
}

interface MessagesListProps extends MessagesListStateProps {}

interface MessagesListState {
    // Number object is used here because in some cases (eg one message / action was selected several times by different entities)
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

    componentDidUpdate(prevProps: MessagesListProps) {
        if (this.props.scrolledMessageId !== prevProps.scrolledMessageId && this.props.scrolledMessageId != null) {
            this.setState({ 
                scrolledIndex: this.getScrolledIndex(this.props.scrolledMessageId, this.props.messages)
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
                <StateSaverProvider>
                    <VirtualizedList
                        selectedElements={messagesHeatmap(messages, selectedMessages, selectedStatus)}
                        rowCount={messages.length}
                        renderElement={this.renderMessage}
                        scrolledIndex={scrolledIndex}
                    />
                </StateSaverProvider>
            </div>
        );
    }

    private renderMessage = (index: number) => {
        const message = this.props.messages[index];

        if (isCheckpoint(message)) {
            return <CheckpointMessage message={message}/>;
        }

        if (isAdmin(message)) {
            return (
                <AdminMessageWrapper
                    key={message.id}
                    message={message}/>
            )
        }

        return (
            <MessageCard
                key={message.id}
                message={message}/>
        );
    }
}

export const MessagesCardList = connect(
    (state: AppState): MessagesListStateProps => ({
        messages: getFilteredMessages(state),
        scrolledMessageId: state.selected.scrolledMessageId,
        selectedMessages: state.selected.messagesId,
        selectedStatus: state.selected.selectedActionStatus
    }),
    () => ({}),
    (stateProps, dispatchProps, ownProps) => ({ ...stateProps, ...dispatchProps, ...ownProps}),
    {
        forwardRef: true
    }
)(MessagesCardListBase);
