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
import {createSelector} from "reselect";
import {getFilterConfig, getFilterResults} from "./filter";
import {keyForMessage} from "../helpers/keys";
import {isRejected} from "../helpers/messageType";
import FilterType from "../models/filter/FilterType";

export const getMessages = (state: AppState) => state.selected.testCase.messages;

export const getFilteredMessages = createSelector(
    [getMessages, getFilterConfig, getFilterResults],
    (messages, config, results) => {
        if (config.types.includes(FilterType.MESSAGE) && config.blocks.length > 0 && !config.isTransparent) {
            return messages.filter(msg => results.includes(keyForMessage(msg.id)))
        }

        return messages;
    }
);

export const getTransparentMessages = createSelector(
    [getMessages, getFilterConfig, getFilterResults],
    (messages, config, results): number[] => {
        if (config.types.includes(FilterType.MESSAGE) && config.blocks.length > 0 && config.isTransparent) {
            return messages
                .filter(msg => !results.includes(keyForMessage(msg.id)))
                .map(msg => msg.id);
        }

        return [];
    }
);

export const getRejectedMessages = createSelector(
    [getMessages],
    (messages) => messages.filter(isRejected)
);
