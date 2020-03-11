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

import AppState from "../state/models/AppState";
import StateActionType from "../actions/stateActions";
import { Middleware } from "redux";
import StateAction from "../actions/stateActions";

export const TEST_CASE_PARAM_KEY = 'tc',
    ACTION_PARAM_KEY = 'ac',
    MESSAGE_PARAM_KEY = 'message';

// we can't use window.location.search because URL can contain another search params

export function getUrlSearchString(url: string) {
    return url.includes('?') ?
        url.substring(url.lastIndexOf('?')) :
        '';
}

// redux middleware
export const urlHandler: Middleware<never, AppState> = store => next => (action: StateActionType) => {

    const prevState = store.getState(),
        result = next(action),
        nextState = store.getState();

    handleStateUpdate(prevState, nextState, action);

    return result;
};

function handleStateUpdate(prevState: AppState, nextState: AppState, action: StateAction) {
    if (prevState.selected.actionsId == nextState.selected.actionsId &&
        prevState.selected.messagesId == nextState.selected.messagesId &&
        prevState.selected.testCase == nextState.selected.testCase) {

        return;
    }

    const searchString = getUrlSearchString(window.location.href),
        searchParams = new URLSearchParams(searchString),
        nextSearchParams = getNextSearchParams(searchParams, prevState, nextState),
        nextUrl = getNextUrl(window.location.href, searchString, nextSearchParams);

    // handle goBack and goForward browser buttons clicks - we don't need to update current url
    const currentUrl = window.location.href;

    if (currentUrl !== nextUrl) {
        window.window.history.pushState(action, "", nextUrl);
    }
}

// returns new search params, based on state change
function getNextSearchParams(searchParams: URLSearchParams, prevState: AppState, nextState: AppState): URLSearchParams {
    if (
        (prevState.selected.testCase && nextState.selected.testCase) && 
        prevState.selected.testCase.order !== nextState.selected.testCase.order
        ) {
        searchParams.set(TEST_CASE_PARAM_KEY, nextState.selected.testCase.order.toString());
        searchParams.delete(MESSAGE_PARAM_KEY);
        searchParams.delete(ACTION_PARAM_KEY);
        return searchParams;
    }

    if (!prevState.selected.testCase && nextState.selected.testCase) {
        searchParams.set(TEST_CASE_PARAM_KEY, nextState.selected.testCase.order.toString());
    }

    if (prevState.selected.testCase && !nextState.selected.testCase) {
        searchParams.delete(TEST_CASE_PARAM_KEY);
        searchParams.delete(MESSAGE_PARAM_KEY);
        searchParams.delete(ACTION_PARAM_KEY);

        return searchParams;
    }

    if (prevState.selected.actionsId != nextState.selected.actionsId) {
        if (nextState.selected.actionsId.length == 1) {
            const actionId = nextState.selected.actionsId[0].toString();
            searchParams.set(ACTION_PARAM_KEY, actionId);
            searchParams.delete(MESSAGE_PARAM_KEY);
        } else {
            searchParams.delete(ACTION_PARAM_KEY);
        }
    }

    if (prevState.selected.messagesId != nextState.selected.messagesId) {
        if (nextState.selected.messagesId.length == 1 && nextState.selected.actionsId.length !== 1) {
            const messageId = nextState.selected.messagesId[0].toString();
            searchParams.set(MESSAGE_PARAM_KEY, messageId);
        } else {
            searchParams.delete(MESSAGE_PARAM_KEY);
        }
    }

    return searchParams;
}

function getNextUrl(prevUrl: string, prevSearchString: string, nextSearchParams: URLSearchParams): string {

    if (prevSearchString && prevSearchString != '?') {
        return prevUrl.replace(prevSearchString, '?' + nextSearchParams.toString());
    } else {
        // search params not found or empty

        return [
            prevUrl,
            // optional '?' - we need this  not to create two '?' symbols
            prevUrl[prevUrl.length - 1] != '?' ? '?' : null,
            nextSearchParams.toString()
        ].join('');
    }
}
