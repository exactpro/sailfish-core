/*******************************************************************************
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
 *  limitations under the License.
 ******************************************************************************/

import AppState from "../state/models/AppState";
import { createSelector } from "reselect";
import { getFilterBlocks, getFilterResults, getIsFilterTransparent, getIsMessageFilterApplied } from "./filter";
import { keyForMessage } from "../helpers/keys";
import { isCheckpointMessage, isRejected, isAdmin } from "../helpers/message";
import FilterType from "../models/filter/FilterType";
import Message from "../models/Message";
import { ScrollHint } from '../models/util/ScrollHint';

const EMPTY_MESSAGES_ARRAY = new Array<Message>();

export const getMessages = (state: AppState) => state.selected.testCase?.messages ?? EMPTY_MESSAGES_ARRAY;

export const getMessagesIds = createSelector(
    [getMessages],
    (messages) => messages.map(msg => msg.id)
);

export const getFilteredMessages = createSelector(
    [getMessages, getFilterBlocks, getFilterResults],
    (messages, blocks, results) => {
        if (blocks.some(({ types }) => types.includes(FilterType.MESSAGE))) {
            return messages
                .filter(msg => isCheckpointMessage(msg) || results.includes(keyForMessage(msg.id)))
        }

        return messages;
    }
);

export const getTransparentMessages = createSelector(
    [getMessages, getFilterBlocks, getFilterResults],
    (messages, blocks, results): number[] => {
        if (blocks.some(({ types }) => types.includes(FilterType.MESSAGE))) {
            return messages
                .filter(msg => !isCheckpointMessage(msg) && !results.includes(keyForMessage(msg.id)))
                .map(msg => msg.id);
        }

        return [];
    }
);

export const getRejectedMessages = createSelector(
    [getFilteredMessages],
    (messages) => messages.filter(isRejected)
);

export const getAdminMessages = createSelector(
    [getFilteredMessages],
    (messages) => messages.filter(isAdmin)
);

export const getMessagesFilesCount = (state: AppState) => state.selected.testCase.files?.message.count || 0;

export const getMessagesCount = createSelector(
    [getIsMessageFilterApplied, getIsFilterTransparent, getFilteredMessages, getMessagesFilesCount],
    (isMessageFilterApplied, isTransparent, filteredMessages, messagesFileCount) => 
        isMessageFilterApplied && !isTransparent ? filteredMessages.length : messagesFileCount
);

export const getCurrentMessages = createSelector(
    [getIsMessageFilterApplied, getIsFilterTransparent, getFilteredMessages, getMessages],
    (isMessageFilterApplied, isTransparent, filteredMessages, messages) =>
        isMessageFilterApplied && !isTransparent ? filteredMessages : messages
)

export const getMessagesScrollHintsIds = (state: AppState) => state.selected.messagesScrollHintsIds;

export const getMessagesScrollHints = createSelector(
    [getCurrentMessages, getMessagesScrollHintsIds],
    (messages, scrollHintsIds) => {
        const scrollHints: ScrollHint[] = [];
        messages
            .forEach(({ id }, index) => {
                if (scrollHintsIds.includes(id)) {
                    scrollHints.push({
                        index,
                        id,
                        type: 'Message',
                    })
                }
            })
        return scrollHints;
    }
)