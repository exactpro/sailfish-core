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

import multiTokenSplit from "../../../helpers/search/multiTokenSplit";
import SearchToken from "../../../models/search/SearchToken";
import SearchSplitResult from "../../../models/search/SearchSplitResult";
import { createSearchToken } from "../../util/creators";

describe('[Helpers] Search - multiTokenSearch', () => {

    const content = '' +
        'Lorem ipsum dolor TEST sit amet, consectetur TEST adipiscing elit.'  +
        'Phasellus fringilla viverra nisl, vitae tincidunt augue imperdiet et.';

    test('Empty tokens list', () => {
         const tokens = [];

         const result = multiTokenSplit(content, tokens);

         expect(result).toEqual<SearchSplitResult[]>([{
             content: content,
             token: null
         }])
    });

    test('One token with one occurrence', () => {
        const tokens: SearchToken[] = [createSearchToken('sit')];

        const result = multiTokenSplit(content, tokens);

        const [start, end] = content.split(new RegExp('sit', 'g'));

        const expectedResult: SearchSplitResult[] = [{
            content: start,
            token: null
        }, {
            content: 'sit',
            token: tokens[0]
        }, {
            content: end,
            token: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('One token with several occurrences', () => {
        const tokens: SearchToken[] = [createSearchToken('TEST')];

        const result = multiTokenSplit(content, tokens);

        const [firstPart, secondPart, thirdPart] = content.split(new RegExp(tokens[0].pattern, 'g'));

        const expectedResult: SearchSplitResult[] = [{
            content: firstPart,
            token: null
        }, {
            content: tokens[0].pattern,
            token: tokens[0]
        }, {
            content: secondPart,
            token: null
        }, {
            content: tokens[0].pattern,
            token: tokens[0]
        }, {
            content: thirdPart,
            token: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('Two token search', () => {
        const tokens: SearchToken[] = [
            createSearchToken('lorem', 'first'),
            createSearchToken('dolor', 'second')
        ];

        const result = multiTokenSplit(content, tokens);

        const expectedResult: SearchSplitResult[] = [{
            content: 'Lorem',
            token: tokens[0]
        }, {
            content: ' ipsum ',
            token: null
        }, {
            content: 'dolor',
            token: tokens[1]
        }, {
            content: content.split(new RegExp('dolor', 'g'))[1],
            token: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('Two intersected tokens search', () => {
        const target = 'consectetur';

        const tokens: SearchToken[] = [
            createSearchToken('consectet', 'first'),
            createSearchToken('tetur', 'second')
        ];

        const result = multiTokenSplit(content, tokens);

        const [startPart, endPart] = content.split(target);

        const expectedResult: SearchSplitResult[] = [{
            content: startPart,
            token: null
        }, {
            content: 'consec',
            token: tokens[0]
        }, {
            content: 'tetur',
            token: tokens[1]
        }, {
            content: endPart,
            token: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('Tree tokens with intersection search', () => {
        const target = 'consectetur';

        const tokens: SearchToken[] = [
            createSearchToken('consectet', 'first'),
            createSearchToken('tetur', 'second'),
            createSearchToken('TEST', 'third')
        ];

        const result = multiTokenSplit(content, tokens);

        const [startPart, endPart] = content.split(target);

        const expectedResult: SearchSplitResult[] = [{
            content: startPart.split(tokens[2].pattern)[0],
            token: null
        }, {
            content: tokens[2].pattern,
            token: tokens[2]
        }, {
            content: startPart.split(tokens[2].pattern)[1],
            token: null
        }, {
            content: 'consec',
            token: tokens[0]
        }, {
            content: 'tetur',
            token: tokens[1]
        }, {
            content: endPart.split(tokens[2].pattern)[0],
            token: null
        }, {
            content: tokens[2].pattern,
            token: tokens[2]
        }, {
            content: endPart.split(tokens[2].pattern)[1],
            token: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('Not found token', () => {
        const tokens: SearchToken[] = [createSearchToken('12345', 'not found')];

        const result = multiTokenSplit(content, tokens);

        const expectedResult: SearchSplitResult[] = [{
            content: content,
            token: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('One token at the end of the content string', () => {
        const tokens: SearchToken[] = [createSearchToken('et.')];

        const result = multiTokenSplit(content, tokens);

        const expectedResults: SearchSplitResult[] = [{
            content: content.substring(0, content.length - tokens[0].pattern.length),
            token: null
        }, {
            content: tokens[0].pattern,
            token: tokens[0]
        }];

        expect(result).toEqual(expectedResults);
    });

    test('One token at the start and another at the end', () => {
        const tokens: SearchToken[] = [
            createSearchToken('Lorem', 'first'),
            createSearchToken('et.', 'second')
        ];

        const result = multiTokenSplit(content, tokens);

        const expectedResults: SearchSplitResult[] = [{
            content: tokens[0].pattern,
            token: tokens[0]
        }, {
            content: content.substring(tokens[0].pattern.length, content.length - tokens[1].pattern.length),
            token: null
        }, {
            content: tokens[1].pattern,
            token: tokens[1]
        }];

        expect(result).toEqual(expectedResults);
    });

    test('Results produced by not scrollable tokens should be overridden by others.', () => {
        const target = 'consectetur';

        const tokens: SearchToken[] = [
            createSearchToken('consectet', 'first'),
            createSearchToken('tetur', 'second', false, false)
        ];

        const result = multiTokenSplit(content, tokens);

        const [startPart, endPart] = content.split(target);

        const expectedResult: SearchSplitResult[] = [{
            content: startPart,
            token: null
        }, {
            content: 'consectet',
            token: tokens[0]
        }, {
            content: 'ur',
            token: tokens[1]
        }, {
            content: endPart,
            token: null
        }];

        expect(result).toEqual(expectedResult);
    });

});

