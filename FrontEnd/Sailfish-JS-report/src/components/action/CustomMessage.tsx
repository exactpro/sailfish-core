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
import UserMessage, { isUserMessage } from '../../models/UserMessage';
import { RecoverableExpandablePanel } from './../ExpandablePanel';
import { createStyleSelector } from '../../helpers/styleCreators';
import '../../styles/action.scss';
import Action, { ActionNodeType } from '../../models/Action';
import { FontWeightProperty } from 'csstype';
import { keyForUserMessage } from '../../helpers/keys';
import { ExceptionChain } from '../ExceptionChain';

interface CustomMessageProps {
    userMessage: UserMessage;
    parent: Action; 
}

export const CustomMessage = ({ userMessage, parent }: CustomMessageProps) => {

    const { message, color, style, level, exception } = userMessage;

    if (!message && !exception) {
        return null;
    }

    // italic style value - only for fontStyle css property
    // bold style value - only for fontWeight css property
    const messageStyle: React.CSSProperties = {
        color: (color || "").toLowerCase(),
        fontStyle: (style || "").toLowerCase(),
        fontWeight: (style || "").toLowerCase() as FontWeightProperty
    };

    const rootClass = createStyleSelector(
        "action-card__custom-msg",
        level
    );

    if (exception) {
        return (
            <RecoverableExpandablePanel
                stateKey={keyForUserMessage(userMessage, parent)}>
                <div className={rootClass}>
                    <div className="ac-body__item-title" style={messageStyle}>{message}</div>
                </div>
                <ExceptionChain
                    exception={exception}/>
            </RecoverableExpandablePanel>
        )
    }

    return (
        <div className={rootClass + "   ac-body__item"}>
            <h3 style={messageStyle}>{message}</h3>
        </div>
    )
}
