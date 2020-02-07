/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
import { StatusType } from '../../models/Status';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { isAdmin, isCheckpointMessage } from '../../helpers/messageType';
import { AdminMessageWrapper } from './AdminMessageWrapper';
import { selectMessage } from '../../actions/actionCreators';
import { messagesHeatmap } from '../../helpers/heatmapCreator';
import StateSaverProvider from '../util/StateSaverProvider';
import { VirtualizedList } from '../VirtualizedList';
import CheckpointMessage from './CheckpointMessage';
import { getFilteredMessages } from "../../selectors/messages";
import { getIsFilterApplied, getMessagesFilterResultsCount } from "../../selectors/filter";
import FilterType from "../../models/filter/FilterType";
import { createBemElement } from "../../helpers/styleCreators";

interface StateProps {
    messages: Message[];
    scrolledMessageId: Number;
    selectedMessages: number[];
    selectedStatus: StatusType;
    isFilterApplied: boolean;
    filteredCount: number;
}

interface Props extends StateProps {}

interface State {
    // Number objects is used here because in some cases (eg one message / action was selected several times by different entities)
    // We can't understand that we need to scroll to the selected entity again when we are comparing primitive numbers.
    // Objects and reference comparison is the only way to handle numbers changing in this case.
    scrolledIndex: Number;
}

export class MessagesCardListBase extends React.PureComponent<Props, State> {

    constructor(props: Props) {
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

    componentDidUpdate(prevProps: Props) {
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
        const { messages, selectedMessages, selectedStatus, isFilterApplied, filteredCount } = this.props,
            { scrolledIndex } = this.state;

        const listClassName = createBemElement(
            "messages",
            "list",
            isFilterApplied ? "filter-applied" : null
        );

        return (
            <div className="messages">
                <div className={listClassName}>
                    {
                        isFilterApplied ? (
                            <div className="messages__filter-info">
                                {filteredCount} Messages Filtered
                            </div>
                        ) : null
                    }
                    <StateSaverProvider>
                        <VirtualizedList
                            selectedElements={messagesHeatmap(messages, selectedMessages, selectedStatus)}
                            rowCount={messages.length}
                            renderElement={this.renderMessage}
                            scrolledIndex={scrolledIndex}
                        />
                    </StateSaverProvider>
                </div>
            </div>
        );
    }

    private renderMessage = (index: number) => {
        const message = this.props.messages[index];

        if (isCheckpointMessage(message)) {
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
    (state: AppState): StateProps => ({
        messages: getIsFilterApplied(state) && !state.filter.isTransparent ?
            getFilteredMessages(state) :
            state.selected.testCase.messages,
        scrolledMessageId: state.selected.scrolledMessageId,
        selectedMessages: state.selected.messagesId,
        selectedStatus: state.selected.selectedActionStatus,
        isFilterApplied: getIsFilterApplied(state) &&
            state.filter.blocks.some(({ types }) => types.includes(FilterType.MESSAGE)),
        filteredCount: getMessagesFilterResultsCount(state)
    }),
    () => ({}),
    (stateProps, dispatchProps, ownProps) => ({ ...stateProps, ...dispatchProps, ...ownProps}),
    {
        forwardRef: true
    }
)(MessagesCardListBase);
