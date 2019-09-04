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
import "../../styles/messages.scss";
import { MessageCardContainer, RecoverableMessageCard, MessageCardStateProps, MessageCardDispatchProps, MessageCardOwnProps } from "./MessageCard";
import Message from '../../models/Message';
import { MessageCardActionChips } from "./MessageCardActionChips";
import { createBemBlock } from '../../helpers/styleCreators';
import StateSaver from '../util/StateSaver';

interface RecoveredProps {
    isExpanded: boolean;
    expandHandler: (isExpanded: boolean) => any;
}

interface MessageCardOuterProps extends MessageCardStateProps, MessageCardDispatchProps, MessageCardOwnProps {}

interface WrapperProps extends MessageCardOuterProps, RecoveredProps {}

const AdminMessageWrapperBase = ({ isExpanded, expandHandler, ...props }: WrapperProps) => {

    // we need to remeasure card's height when 'isExpanded' state changed
    React.useEffect(props.onExpand, [isExpanded]);

    if (isExpanded) {
        const expandButtonClass = createBemBlock(
            "mc-expand-btn", 
            props.message.content.rejectReason != null ? "rejected" : null
        );

        return (
            <div style={{position: "relative"}}>
                <RecoverableMessageCard {...props}/>
                <div className={expandButtonClass}>
                    <div className="mc-expand-btn__icon" onClick={() => expandHandler(!isExpanded)}/>
                </div>
            </div>
        );
    }

    const rootClass = createBemBlock(
        "message-card",
        props.status,
        props.isSelected ? "selected" : null
    );

    return (
        <div className={rootClass}
            data-lb-count={getLabelsCount(props.message)}
            onClick={() => props.selectHandler()}>
            <div className="message-card__labels">
                {renderMessageTypeLabels(props.message)}
            </div>
            <div className="message-card__header   mc-header small">
                <div className="mc-header__info">
                    <MessageCardActionChips
                        message={props.message}/>
                </div>
                <div className="mc-header__name">Name</div>
                <div className="mc-header__name-value">{props.message.msgName}</div>
                <div className="mc-header__expand">
                    <div className="mc-header__expand-icon" onClick={() => expandHandler(!isExpanded)}/>
                </div>
            </div>
        </div>
    );
}

function renderMessageTypeLabels(message: Message): React.ReactNodeArray {
    let labels = [];

    if (message.content.rejectReason !== null) {
        labels.push(
            <div className="mc-label rejected" key="rejected">
                <div className="mc-label__icon rejected" style={{marginTop: "10px"}}/>
            </div>
        );
    }

    if (message.content.admin) {
        labels.push(
            <div className="mc-label admin" key="admin">
                <div className="mc-label__icon admin" style={{marginTop: "10px"}}/>
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

interface RecoverableState {
    isExpanded: boolean;
    lastAdminEnabled: Boolean;
};

const RecoverableAdminMessageWrapper = (props: MessageCardOuterProps) => (
    <StateSaver
        stateKey={`msg-${props.message.id}-admin`}
        getDefaultState={() : RecoverableState => ({ isExpanded: false, lastAdminEnabled: props.adminEnabled })}>
        {({ isExpanded, lastAdminEnabled }: RecoverableState, saveState) => (
            // We pass in adminEnabled flag only if it was changed since last component update.
            // It works because adminEnabled is Boolean object and we can use reference comparison.
            // (same logic in Actions / Messages lists with scrolled indexes)
            <AdminMessageWrapperBase
                {...props}
                // it is important to use 'Bolean.valueOf()' because Boolean(false) is truthy
                isExpanded={props.adminEnabled !== lastAdminEnabled ? props.adminEnabled.valueOf() : isExpanded}
                expandHandler={nextIsExpanded => saveState({ isExpanded: nextIsExpanded, lastAdminEnabled: props.adminEnabled })}/>
        )}
    </StateSaver>
)

export const AdminMessageWrapper = MessageCardContainer(RecoverableAdminMessageWrapper);
