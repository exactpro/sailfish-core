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

describe('[Helpers] Search - multiTokenSearch', () => {

    const content = '' +
        'Lorem ipsum dolor TEST sit amet, consectetur TEST adipiscing elit.'  +
        'Phasellus fringilla viverra nisl, vitae tincidunt augue imperdiet et.';

    test('Empty tokens list', () => {
         const tokens = [];

         const result = multiTokenSplit(content, tokens);

         expect(result).toEqual([{
             content: content,
             color: null
         }])
    });

    test('One token with one occurrence', () => {
        const tokens: SearchToken[] = [{
            pattern: 'sit',
            color: 'default'
        }];

        const result = multiTokenSplit(content, tokens);

        const [start, end] = content.split(new RegExp('sit', 'g'));

        const expectedResult: SearchSplitResult[] = [{
            content: start,
            color: null
        }, {
            content: 'sit',
            color: 'default'
        }, {
            content: end,
            color: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('One token with several occurrences', () => {
        const tokens: SearchToken[] = [{
            pattern: 'TEST',
            color: 'default'
        }];

        const result = multiTokenSplit(content, tokens);

        const [firstPart, secondPart, thirdPart] = content.split(new RegExp(tokens[0].pattern, 'g'));

        const expectedResult: SearchSplitResult[] = [{
            content: firstPart,
            color: null
        }, {
            content: tokens[0].pattern,
            color: tokens[0].color
        }, {
            content: secondPart,
            color: null
        }, {
            content: tokens[0].pattern,
            color: tokens[0].color
        }, {
            content: thirdPart,
            color: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('Two token search', () => {
        const tokens: SearchToken[] = [{
            pattern: 'lorem',
            color: 'first'
        }, {
            pattern: 'dolor',
            color: 'second'
        }];

        const result = multiTokenSplit(content, tokens);

        const expectedResult: SearchSplitResult[] = [{
            content: 'Lorem',
            color: 'first'
        }, {
            content: ' ipsum ',
            color: null
        }, {
            content: 'dolor',
            color: 'second'
        }, {
            content: content.split(new RegExp('dolor', 'g'))[1],
            color: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('Two intersected tokens search', () => {
        const target = 'consectetur';

        const tokens: SearchToken[] = [{
            pattern: 'consectet',
            color: 'first'
        }, {
            pattern: 'tetur',
            color: 'second'
        }];

        const result = multiTokenSplit(content, tokens);

        const [startPart, endPart] = content.split(target);

        const expectedResult: SearchSplitResult[] = [{
            content: startPart,
            color: null
        }, {
            content: 'consec',
            color: tokens[0].color
        }, {
            content: 'tetur',
            color: tokens[1].color
        }, {
            content: endPart,
            color: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('Tree tokens with intersection search', () => {
        const target = 'consectetur';

        const tokens: SearchToken[] = [{
            pattern: 'consectet',
            color: 'first'
        }, {
            pattern: 'tetur',
            color: 'second'
        }, {
            pattern: 'TEST',
            color: 'third'
        }];

        const result = multiTokenSplit(content, tokens);

        const [startPart, endPart] = content.split(target);

        const expectedResult: SearchSplitResult[] = [{
            content: startPart.split(tokens[2].pattern)[0],
            color: null
        }, {
            content: tokens[2].pattern,
            color: tokens[2].color
        }, {
            content: startPart.split(tokens[2].pattern)[1],
            color: null
        }, {
            content: 'consec',
            color: tokens[0].color
        }, {
            content: 'tetur',
            color: tokens[1].color
        }, {
            content: endPart.split(tokens[2].pattern)[0],
            color: null
        }, {
            content: tokens[2].pattern,
            color: tokens[2].color
        }, {
            content: endPart.split(tokens[2].pattern)[1],
            color: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('Not found token', () => {
        const tokens: SearchToken[] = [{
            pattern: '12345',
            color: 'not found'
        }];

        const result = multiTokenSplit(content, tokens);

        const expectedResult: SearchSplitResult[] = [{
            content: content,
            color: null
        }];

        expect(result).toEqual(expectedResult);
    });

    test('One token at the end of the content string', () => {
        const tokens: SearchToken[] = [{
            pattern: 'et.',
            color: 'default'
        }];

        const result = multiTokenSplit(content, tokens);

        const expectedResults: SearchSplitResult[] = [{
            content: content.substring(0, content.length - tokens[0].pattern.length),
            color: null
        }, {
            content: tokens[0].pattern,
            color: tokens[0].color
        }];

        expect(result).toEqual(expectedResults);
    });

    test('One token at the start and another at the end', () => {
        const tokens: SearchToken[] = [{
            pattern: 'Lorem',
            color: 'first'
        },{
            pattern: 'et.',
            color: 'second'
        }];

        const result = multiTokenSplit(content, tokens);

        const expectedResults: SearchSplitResult[] = [{
            content: tokens[0].pattern,
            color: tokens[0].color
        }, {
            content: content.substring(tokens[0].pattern.length, content.length - tokens[1].pattern.length),
            color: null
        }, {
            content: tokens[1].pattern,
            color: tokens[1].color
        }];

        expect(result).toEqual(expectedResults);
    })

});

