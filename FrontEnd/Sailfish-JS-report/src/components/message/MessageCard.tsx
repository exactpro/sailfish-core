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
import Message from '../../models/Message';
import { StatusType } from '../../models/Status';
import { MessageRaw } from './MessageRaw';
import { getHashCode } from '../../helpers/stringHash';
import { formatTime } from '../../helpers/dateFormatter';
import { MessageCardActionChips } from './MessageCardActionChips';
import { MlUploadButton } from '../machinelearning/MlUploadButton';
import '../../styles/messages.scss';
import { createBemElement } from '../../helpers/styleCreators';
import { createBemBlock } from '../../helpers/styleCreators';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { selectMessage, toggleMessageBeautifier } from '../../actions/actionCreators';
import { isRejected } from '../../helpers/messageType';
import SearchableContent from '../search/SearchableContent';
import { keyForMessage } from '../../helpers/keys';
import {MessagePredictionIndicator} from "../machinelearning/MlPredictionIndicator";
import StateSaver from '../util/StateSaver';
import ErrorBoundary from '../util/ErrorBoundary';
import BeautifiedContent from '../action/BeautifiedContent';

const HUE_SEGMENTS_COUNT = 36;

export interface MessageCardOwnProps {
    message: Message;
    onExpand: () => void;
}

export interface RecoveredProps {
    showRaw: boolean;
    showRawHandler: (showRaw: boolean) => any;
}

export interface MessageCardStateProps {
    rejectedMessagesCount: number;
    isSelected: boolean;
    status: StatusType;
    adminEnabled: Boolean;
    isContentBeautified: boolean;
}

export interface MessageCardDispatchProps {
    selectHandler: (status?: StatusType) => any;
    toggleBeautify: () => void;
}

export interface MessageCardProps extends MessageCardOwnProps, MessageCardStateProps, MessageCardDispatchProps, RecoveredProps { }

export const MessageCardBase = ({ 
        message, 
        isSelected, 
        status, 
        rejectedMessagesCount, 
        selectHandler, 
        showRaw, 
        showRawHandler, 
        isContentBeautified, 
        toggleBeautify, 
        onExpand 
    }: MessageCardProps) => {

    const { id, msgName, timestamp, from, to, contentHumanReadable, raw } = message;

    const rejectedTitle = message.content.rejectReason,
        labelsCount = getLabelsCount(message);

    const rootClass = createBemBlock("message-card", status, isSelected ? "selected" : null), 
        showRawClass = createBemElement("mc-show-raw", "icon", showRaw ? "expanded" : "hidden"),
        beautifyIconClass = createBemElement("mc-beautify", "icon", isContentBeautified ? "plain" : "beautify"),
        // session arrow color, we calculating it for each session from-to pair, based on hash 
        sessionArrowStyle = { filter: `invert(1) sepia(1) saturate(5) hue-rotate(${calculateHueValue(from, to)}deg)` };

    // we need to remeasure card's height when 'showRaw' or 'isContentBeautified' state changed
    React.useEffect(onExpand, [showRaw, isContentBeautified]);

    return (
        <div className={rootClass} data-lb-count={labelsCount}>
            <div className="message-card__labels">
                {renderMessageTypeLabels(message)}
            </div>
            <div className="mc-header default" 
                onClick={() => selectHandler()}>
                {
                    rejectedMessagesCount && message.relatedActions.length == 0 ?
                        (
                            <div className="mc-header__info rejected">
                                <p>Rejected {rejectedMessagesCount}</p>
                            </div>
                        ) : (
                            <MessageCardActionChips
                                message={message} />
                        )
                }
                <div className="mc-header__name">
                    <span>Name</span>
                </div>
                <div className="mc-header__name-value">
                    <SearchableContent  
                        content={msgName}
                        contentKey={keyForMessage(id, 'msgName')}/>
                </div>
                <div className="mc-header__timestamp">
                    <p>{timestamp}</p>
                </div>
                <div className="mc-header__session">
                    <span>Session</span>
                </div>
                <div className="mc-header__from">
                    <SearchableContent
                        content={from}
                        contentKey={keyForMessage(id, 'from')}/>
                </div>
                {
                    from && to ?
                        <div className="mc-header__session-icon"
                            style={sessionArrowStyle} />
                        : null
                }
                <div className="mc-header__to">
                    <SearchableContent
                        content={to}
                        contentKey={keyForMessage(id, 'to')}/>
                </div>
                <MlUploadButton messageId={message.id}/>
            </div>
            <div className="message-card__body   mc-body">
                {
                    message.content.rejectReason !== null ? (
                        <div className="mc-body__title">
                            <p>{rejectedTitle}</p>
                        </div>
                    ) : null
                }
                <div className="mc-body__human">
                    <div className="mc-beautify" onClick={() => toggleBeautify()}>
                        <div className={beautifyIconClass}/>
                    </div>
                    {
                        isContentBeautified ? (
                            <ErrorBoundary
                                errorMessage="Can't parse message.">
                                <BeautifiedContent
                                    content={contentHumanReadable}
                                    msgId={id}/>
                            </ErrorBoundary>
                        ) : (
                            <SearchableContent 
                                content={contentHumanReadable} 
                                contentKey={keyForMessage(id, 'contentHumanReadable')}/>
                        )
                    }
                    {
                        (raw && raw !== 'null') ? (
                            <div className="mc-show-raw"
                                onClick={() => showRawHandler(!showRaw)}>
                                <div className="mc-show-raw__title">RAW</div>
                                <div className={showRawClass} />
                            </div>
                        ) : null
                    }
                    {
                        showRaw ?
                            <MessageRaw
                                rawContent={raw} />
                            : null
                    }
                </div>
            </div>
        </div>
    );
};

function renderMessageTypeLabels(message: Message): React.ReactNodeArray {
    let labels = [];

    labels.push(
        <MessagePredictionIndicator className="mc-label" messageId={message.id} />
    );

    if (message.content.rejectReason !== null) {
        labels.push(
            <div className="mc-label rejected">
                <div className="mc-label__icon rejected" />
            </div>
        );
    }

    if (message.content.admin) {
        labels.push(
            <div className="mc-label admin">
                <div className="mc-label__icon admin" />
            </div>
        )
    }

    return labels;
}

function getLabelsCount(message: Message) {
    let count = 0;

    if (message.content.rejectReason != null) {
        count++;
    }

    if (message.content.admin) {
        count++;
    }

    return count;
}

function calculateHueValue(from: string, to: string): number {
    return (getHashCode([from, to].filter(str => str).sort((a, b) => a.localeCompare(b)).join(''))
        % HUE_SEGMENTS_COUNT) * (360 / HUE_SEGMENTS_COUNT);
}

export const RecoverableMessageCard = (props: MessageCardStateProps & MessageCardOwnProps & MessageCardDispatchProps) => (
    <StateSaver
        stateKey={`msg-${props.message.id}`}
        getDefaultState={() => false}>
        {(state, saveState) => (
            <MessageCardBase
                {...props}
                showRaw={state}
                showRawHandler={saveState}/>
        )}
    </StateSaver>
)

export const MessageCardContainer = connect(
    (state: AppState, ownProps: MessageCardOwnProps): MessageCardStateProps => ({
        isSelected: state.selected.messagesId.includes(ownProps.message.id) || state.selected.rejectedMessageId === ownProps.message.id,
        status: state.selected.messagesId.includes(ownProps.message.id) ? state.selected.status : null,
        rejectedMessagesCount: isRejected(ownProps.message) ? state.selected.testCase.messages.filter(isRejected).indexOf(ownProps.message) + 1 : null,
        adminEnabled: state.view.adminMessagesEnabled,
        isContentBeautified: state.view.beautifiedMessages.includes(ownProps.message.id)
    }),
    (dispatch, ownProps: MessageCardOwnProps) : MessageCardDispatchProps => ({
        selectHandler: (status?: StatusType) => dispatch(selectMessage(ownProps.message, status)),
        toggleBeautify: () => dispatch(toggleMessageBeautifier(ownProps.message.id))
    })
);

const MessageCard = MessageCardContainer(RecoverableMessageCard);

export default MessageCard;
