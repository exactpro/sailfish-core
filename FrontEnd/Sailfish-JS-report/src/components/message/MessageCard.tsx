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
import { MlUploadButton } from './MlUploadButton';
import '../styles/messages.scss';
import { createBemElement } from '../helpers/styleCreators';
import { createBemBlock } from '../helpers/styleCreators';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { selectMessage } from '../../actions/actionCreators';
import { isRejected } from '../../helpers/messageType';

const HUE_SEGMENTS_COUNT = 36;

export interface MessageCardOwnProps {
    message: Message;
    showRaw?: boolean;
    rejectedMessagesCount?: number;
    showRawHandler?: (showRaw: boolean) => any;
}

interface MessageCardStateProps {
    rejectedMessagesCount?: number;
    isSelected?: boolean;
    status?: StatusType;
}

interface MessageCardDispatchProps {
    selectHandler: (status?: StatusType) => any;
}

export interface MessageCardProps extends MessageCardOwnProps, MessageCardStateProps, MessageCardDispatchProps { }

export const MessageCardBase = ({ message, isSelected, status, rejectedMessagesCount, selectHandler, showRaw, showRawHandler }: MessageCardProps) => {
    const { msgName, timestamp, from, to, contentHumanReadable, raw } = message;

    const rejectedTitle = message.content.rejectReason,
        labelsCount = getLabelsCount(message);

    const rootClass = createBemBlock("message-card", status, isSelected ? "selected" : null), 
        showRawClass = createBemElement("mc-show-raw", "icon", showRaw ? "expanded" : "hidden"),
        // session arrow color, we calculating it for each session from-to pair, based on hash 
        sessionArrowStyle = { filter: `invert(1) sepia(1) saturate(5) hue-rotate(${calculateHueValue(from, to)}deg)` };

    return (
        <div className={rootClass}>
            <div className="message-card__labels">
                {renderMessageTypeLabels(message)}
            </div>
                <div className="mc-header default" 
                    data-lb-count={labelsCount}
                    onClick={() => selectHandler()}>
                    {
                        rejectedMessagesCount && message.relatedActions.length == 0 ?
                            (
                                <div className="mc-header__info rejected">
                                    <p>Rejected {rejectedMessagesCount}</p>
                                </div>
                            )
                            : (
                                <MessageCardActionChips
                                    message={message} />
                            )
                    }
                    <div className="mc-header__name">
                        <span>Name</span>
                    </div>
                    <div className="mc-header__name-value">
                        <p>{msgName}</p>
                    </div>
                    <div className="mc-header__timestamp">
                        <p>{formatTime(timestamp)}</p>
                    </div>
                    <div className="mc-header__session">
                        <span>Session</span>
                    </div>
                    <div className="mc-header__from">
                        <p>{from}</p>
                    </div>
                    {
                        from && to ?
                            <div className="mc-header__session-icon"
                                style={sessionArrowStyle} />
                            : null
                    }
                    <div className="mc-header__to">
                        <p>{to}</p>
                    </div>
                    <MlUploadButton messageId={message.id}/>
                </div>
                <div className="message-card__body   mc-body">
                    {
                        (message.content.rejectReason !== null) ?
                            (
                                <div className="mc-body__title">
                                    <p>{rejectedTitle}</p>
                                </div>
                            )
                            : null
                    }
                    <div className="mc-body__human">
                        <div>
                            {contentHumanReadable}
                            {
                                (raw && raw !== 'null') ? (
                                    <div className="mc-show-raw"
                                        onClick={e => showRawHandler && showRawHandler(!showRaw)}>
                                        <div className="mc-show-raw__title">RAW</div>
                                        <div className={showRawClass} />
                                    </div>
                                ) : null
                            }
                        </div>
                    </div>
                    {
                        showRaw ?
                            <MessageRaw
                                rawContent={raw} />
                            : null
                    }
                </div>
            </div>
    );
}

function renderMessageTypeLabels(message: Message): JSX.Element[] {
    let labels = [];

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

export const MessageCardContainer = connect(
    (state: AppState, ownProps: MessageCardOwnProps): MessageCardStateProps => ({
        isSelected: state.selected.messagesId.includes(ownProps.message.id) || state.selected.rejectedMessageId === ownProps.message.id,
        status: state.selected.messagesId.includes(ownProps.message.id) ? state.selected.status : null,
        rejectedMessagesCount: isRejected(ownProps.message) ? state.selected.testCase.messages.filter(isRejected).indexOf(ownProps.message) + 1 : null
    }),
    (dispatch, ownProps: MessageCardOwnProps) : MessageCardDispatchProps => ({
        selectHandler: (status?: StatusType) => dispatch(selectMessage(ownProps.message, status))
    })
);

export const MessageCard = MessageCardContainer(MessageCardBase);
