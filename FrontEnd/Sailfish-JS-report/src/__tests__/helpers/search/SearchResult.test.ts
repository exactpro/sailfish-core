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

import SearchResult from "../../../helpers/search/SearchResult";
import { keyForAction } from "../../../helpers/keys";
import { createSearchSplitResult, createSearchToken } from "../../util/creators";

describe('[Helpers] Search - SearchResult', () => {

    const normalToken = createSearchToken();
    const ignoredToken = createSearchToken('ignored content', 'filter', false, false);

    const firstKey = keyForAction(1, 'name');
    const secondKey = keyForAction(2, 'name');

    const defaultSearchResult = new SearchResult([
        [firstKey, [
            createSearchSplitResult('content'),
            createSearchSplitResult('test', normalToken),
            createSearchSplitResult('another content')
        ]],
        [secondKey, [
            createSearchSplitResult('test', normalToken),
            createSearchSplitResult('content'),
            createSearchSplitResult('test', normalToken)
        ]]
    ]);

    const searchResultWithIgnored = new SearchResult([
        [firstKey, [
            createSearchSplitResult('ignored content', ignoredToken),
            createSearchSplitResult('test', normalToken),
            createSearchSplitResult('content')
        ]],
        [secondKey, [
            createSearchSplitResult('ignored content', ignoredToken),
            createSearchSplitResult('test', normalToken),
            createSearchSplitResult('ignored content', ignoredToken),
            createSearchSplitResult('content'),
            createSearchSplitResult('test', normalToken),
            createSearchSplitResult('ignored content', ignoredToken),
        ]]
    ]);

    test('SearchResult.sum() with some simple results', () => {
        expect(defaultSearchResult.sum()).toEqual(3);
    });

    test('SearchResult.sum() with some ignored results', () => {
        expect(searchResultWithIgnored.sum()).toEqual(3);
    });

    test('SearchResult.getByIndex() with some simple results', () => {
        expect(defaultSearchResult.getByIndex(0)[0]).toEqual(firstKey);
        expect(defaultSearchResult.getByIndex(1)[0]).toEqual(secondKey);
        expect(defaultSearchResult.getByIndex(2)[0]).toEqual(secondKey);
    });

    test('SearchResult.getByIndex() with some not scrollable results', () => {
        expect(searchResultWithIgnored.getByIndex(0)[0]).toEqual(firstKey);
        expect(searchResultWithIgnored.getByIndex(1)[0]).toEqual(secondKey);
        expect(searchResultWithIgnored.getByIndex(2)[0]).toEqual(secondKey);
    });

    test('SearchResults.getStartIndexForKey() with simple results', () => {
        expect(defaultSearchResult.getStartIndexForKey(firstKey)).toEqual(0);
        expect(defaultSearchResult.getStartIndexForKey(secondKey)).toEqual(1);
    });

    test('SearchResults.getStartIndexForKey() with some ignored results', () => {
        expect(searchResultWithIgnored.getStartIndexForKey(firstKey)).toEqual(0);
        expect(searchResultWithIgnored.getStartIndexForKey(secondKey)).toEqual(1);
    });
});