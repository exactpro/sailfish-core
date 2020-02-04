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

import { createAction, createMessage, createTestCase } from "../../util/creators";
import { findAll } from "../../../helpers/search/searchEngine";
import { keyForAction, keyForMessage } from "../../../helpers/keys";
import SearchToken from "../../../models/search/SearchToken";

describe('[Helpers] Search - searchEngine', () => {

    test('One token search in action\'s name', async () => {
        const tokens: SearchToken[] = [{
            pattern: 'test',
            color: 'default'
        }];

        const testCase = createTestCase('0', [{
            ...createAction(1),
            name: 'some test name here'
        }]);

        const results = await findAll(tokens, testCase);

        const expectedResults: Array<[string, number]> = [[keyForAction(1, 'name'), 1]];

        expect(results.entries).toEqual(expectedResults);
    });

    test('Several tokens search in action\'s name', async () => {
        const tokens: SearchToken[] = [{
            pattern: 'test',
            color: 'first'
        }, {
            pattern: 'some',
            color: 'second'
        }, {
            pattern: 'me',
            color: 'third'
        }];

        const testCase = createTestCase('0', [{
            ...createAction(1),
            name: 'some test name here'
        }]);

        const results = await findAll(tokens, testCase);

        const expectedResults: Array<[string, number]> = [
            [keyForAction(1, 'name'), 4]
        ];

        expect(results.entries).toEqual(expectedResults);
    });

    test('Several tokens in actions and messages names', async () => {
        const tokens: SearchToken[] = [{
            pattern: 'test',
            color: 'first'
        }, {
            pattern: 'some',
            color: 'second'
        }, {
            pattern: 'me',
            color: 'third'
        }];

        const testCase = createTestCase('0', [{
            ...createAction(1),
            name: 'some test name here'
        }], [{
            ...createMessage(2),
            msgName: 'some another name'
        }]);

        const results = await findAll(tokens, testCase);

        const expectedResults: Array<[string, number]> = [
            [keyForAction(1, 'name'), 4],
            [keyForMessage(2, 'msgName'), 3]
        ];

        expect(results.entries).toEqual(expectedResults);
    })

});

