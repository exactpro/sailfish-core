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

import { createAction, createMessage, createSearchToken, createTestCase } from "../../util/creators";
import { findAll } from "../../../helpers/search/searchEngine";
import { keyForAction, keyForMessage } from "../../../helpers/keys";
import SearchToken, { PanelSearchToken } from "../../../models/search/SearchToken";
import SearchSplitResult from "../../../models/search/SearchSplitResult";
import multiTokenSplit from "../../../helpers/search/multiTokenSplit";

describe('[Helpers] Search - searchEngine', () => {

    test('One token search in action\'s name', async () => {
        const tokens: PanelSearchToken[] = [createSearchToken()];

        const action = {
            ...createAction(1),
            name: 'some test name here'
        };
        const testCase = createTestCase('0', [action]);

        const results = await findAll(tokens, testCase);

        const expectedResults: Array<[string, SearchSplitResult[]]> = [
            [keyForAction(1, 'name'), multiTokenSplit(action.name, tokens)]
        ];

        expect(results.entries).toEqual(expectedResults);
    });

    test('Several tokens search in action\'s name', async () => {
        const tokens: PanelSearchToken[] = [
            createSearchToken('test', 'first'),
            createSearchToken('some', 'second'),
            createSearchToken('me', 'third')
        ];

        const action = {
            ...createAction(1),
            name: 'some test name here'
        };
        const testCase = createTestCase('0', [action]);

        const results = await findAll(tokens, testCase);

        const expectedResults: Array<[string, SearchSplitResult[]]> = [
            [keyForAction(1, 'name'), multiTokenSplit(action.name, tokens)]
        ];

        expect(results.entries).toEqual(expectedResults);
    });

    test('Several tokens in actions and messages names', async () => {
        const tokens: PanelSearchToken[] = [
            createSearchToken('test', 'first'),
            createSearchToken('some', 'second'),
            createSearchToken('me', 'third')
        ];

        const action = {
            ...createAction(1),
            name: 'some test name here'
        };
        const message = {
            ...createMessage(2),
            msgName: 'some another name'
        };
        const testCase = createTestCase('0', [action], [message]);

        const results = await findAll(tokens, testCase);

        const expectedResults: Array<[string, SearchSplitResult[]]> = [
            [keyForAction(1, 'name'), multiTokenSplit(action.name, tokens)],
            [keyForMessage(2, 'msgName'), multiTokenSplit(message.msgName, tokens)]
        ];

        expect(results.entries).toEqual(expectedResults);
    })

});

