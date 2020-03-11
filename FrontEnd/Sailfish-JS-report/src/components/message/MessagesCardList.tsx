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
import { StatusType } from '../../models/Status';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { messagesHeatmap } from '../../helpers/heatmapCreator';
import StateSaverProvider from '../util/StateSaverProvider';
import { VirtualizedList } from '../VirtualizedList';
import { getCurrentMessages, getMessagesScrollHints } from "../../selectors/messages";
import { getMessagesFilterResultsCount, getIsMessageFilterApplied } from "../../selectors/filter";
import { createBemElement } from "../../helpers/styleCreators";
import SkeletonedMessageCardListItem from './SkeletonedMessageCardListItem';
import { getMessagesCount } from '../../selectors/messages';
import { ScrollHint } from '../../models/util/ScrollHint';

interface StateProps {
    messages: Message[];
    scrolledMessageId: Number;
    selectedMessages: number[];
    selectedStatus: StatusType;
    isFilterApplied: boolean;
    filteredCount: number;
    messagesCount: number;
    messagesScrollHints: ScrollHint[];
}

interface Props extends StateProps {}

interface State {
    // Number objects is used here because in some cases (eg one message / action was selected several times by different entities)
    // We can't understand that we need to scroll to the selected entity again when we are comparing primitive numbers.
    // Objects and reference comparison is the only way to handle numbers changing in this case.
    scrolledIndex: Number;
}

interface OwnProps {
    ref?: React.MutableRefObject<MessagesCardListBase>
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
        const { messages, selectedMessages, selectedStatus, isFilterApplied, filteredCount, messagesCount, messagesScrollHints } = this.props,
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
                            rowCount={messagesCount}
                            renderElement={this.renderMessage}
                            computeItemKey={this.computeKey}
                            scrolledIndex={scrolledIndex}
                            scrollHints={messagesScrollHints}
                        />
                    </StateSaverProvider>
                </div>
            </div>
        );
    }

    private renderMessage = (index: number) => {
        return (
            <SkeletonedMessageCardListItem index={index} />
        )
    };

    private computeKey = (index: number) => {
        return this.props.messages[index]?.id ?? index;
    };
}

export const MessagesCardList = connect<StateProps, {}, OwnProps, StateProps & OwnProps>(
    (state: AppState): StateProps => ({
        messages: getCurrentMessages(state),
        messagesCount: getMessagesCount(state),
        scrolledMessageId: state.selected.scrolledMessageId,
        selectedMessages: state.selected.messagesId,
        selectedStatus: state.selected.selectedActionStatus,
        isFilterApplied: getIsMessageFilterApplied(state),
        filteredCount: getMessagesFilterResultsCount(state),
        messagesScrollHints: getMessagesScrollHints(state),
    }),
    () => ({}),
    (stateProps, dispatchProps, ownProps) => ({ ...stateProps, ...dispatchProps, ...ownProps}),
    {
        forwardRef: true
    }
)(MessagesCardListBase);
