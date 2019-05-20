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
import { VirtualizedList } from '../VirtualizedList';
import MessageCardExpandState from '../../models/view/MessageCardExpandState';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import Message from '../../models/Message';

interface MessagesVirtualizedListOwnProps {
    messagesCount: number;
    scrolledIndex?: Number;
    messageRenderer: (
        index: number, 
        messageState: MessageCardExpandState,
        messageStateHandler: (nextState: MessageCardExpandState) => any
    ) => JSX.Element;
}

interface MessagesVirtualizedListStateProps {
    adminMessagesEnabled: boolean;
}

interface MessagesVirtualizedListProps extends MessagesVirtualizedListOwnProps, MessagesVirtualizedListStateProps {}

class MessagesVirtualizedListBase extends React.Component<MessagesVirtualizedListProps> {

    private messagesCardStates : MessageCardExpandState[] = [];
    private list : VirtualizedList;

    componentWillMount() {
        this.updateMessagesStates(this.props.messagesCount);
    }

    componentWillReceiveProps(nextProps: MessagesVirtualizedListProps) {
        if (nextProps.adminMessagesEnabled !== this.props.adminMessagesEnabled) {
            this.messagesCardStates = this.messagesCardStates.map(state => ({...state, adminExpanded: nextProps.adminMessagesEnabled }));
            this.list.remeasureAll();
        }
    }

    render() {
        const { messagesCount, scrolledIndex } = this.props;

        return (
            <VirtualizedList
                rowCount={messagesCount}
                elementRenderer={this.rowRenderer}
                itemSpacing={6}
                ref={ref => this.list = ref}
                scrolledIndex={scrolledIndex}
            />
        )
    }

    private rowRenderer = (index: number): JSX.Element => {
        const state = this.messagesCardStates[index];

        return this.props.messageRenderer(
            index,
            state,
            (nextState: MessageCardExpandState) => this.messageCardStateHandler(nextState, index)
        );
    }

    private updateMessagesStates(messagesCount: number) {
        // init message cards states 
        this.messagesCardStates = new Array<MessageCardExpandState>(messagesCount).fill({});
    }

    private measureElement(index: number) {
        this.list.remeasureRow(index);
    }

    private messageCardStateHandler(nextState: MessageCardExpandState, elementIndex: number) {
        this.messagesCardStates[elementIndex] = nextState;
        this.measureElement(elementIndex);
    }
}

export const MessagesVirtualizedList = connect(
    (state: AppState): MessagesVirtualizedListStateProps => ({
        adminMessagesEnabled: state.view.adminMessagesEnabled
    }),
    dispatch => ({})
)(MessagesVirtualizedListBase);
