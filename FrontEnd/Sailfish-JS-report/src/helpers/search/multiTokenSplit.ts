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

import { createCaseInsensitiveRegexp } from "../regexp";
import SearchToken from "../../models/search/SearchToken";
import SearchSplitResult from "../../models/search/SearchSplitResult";

export default function multiTokenSplit(content: string, tokens: ReadonlyArray<SearchToken>): SearchSplitResult[] {
    const sortRule = (a: SearchToken, b: SearchToken): number => {
        if (a.isScrollable == b.isScrollable) {
            // we are sorting tokens from longer to shorter one because in case of intersected tokens will truncate longer tokens
            return a.pattern.length < b.pattern.length ? 1 : -1;
        }

        // also not scrollable items must be truncated by others
        return a.isScrollable ? 1 : -1;
    };

    const splitContent = (token: SearchToken) => {
        return {
            content: content
                .split(createCaseInsensitiveRegexp(token.pattern))
                .slice(0, -1),
            token
        }
    };

    const tokenSplitResults = [...tokens]
        .filter(({ pattern }) => pattern.length > 0)
        .sort(sortRule)
        .map(splitContent)
        .filter(res => res.content.length > 0);

    const result: SearchSplitResult[] = [{
        content,
        token: null
    }];

    for (const splitResult of tokenSplitResults) {
        let currentContentIndex = 0;

        for (const splitContentPart of splitResult.content) {
            currentContentIndex += splitContentPart.length;

            const appendingResult: SearchSplitResult = {
                // we need to get original part of content, not pattern in case of case insensitive
                content: content.substring(currentContentIndex, currentContentIndex + splitResult.token.pattern.length),
                token: splitResult.token
            };

            let acc = 0,
                startBlockOffset = 0,
                endBlockOffset = 0;

            // trying to find part of previous result, that need to be modified
            const startIndex = result.findIndex(res => {
                acc += res.content.length;

                if (acc > currentContentIndex) {
                    startBlockOffset = res.content.length - (acc - currentContentIndex);
                    return true;
                }

                return false;
            });

            acc = 0;

            const endIndex = result.findIndex(res => {
                acc += res.content.length;

                if (acc >= currentContentIndex + splitResult.token.pattern.length) {
                    endBlockOffset = res.content.length - (acc - (currentContentIndex + splitResult.token.pattern.length));

                    return true;
                }

                return false;
            });

            // part of previous result that need to be replaced
            const blocks = result.slice(startIndex, endIndex + 1);

            if (blocks.length == 0) {
                throw new Error(`Can't merge search results with token "${splitResult.token.pattern}"`);
            }

            let nextResults: SearchSplitResult[] = [];

            if (blocks.length == 1 ) {
                const block = blocks[0];

                nextResults = [{
                    ...block,
                    content: block.content.substring(0, startBlockOffset)
                }, appendingResult, {
                    ...block,
                    content: block.content.substring(endBlockOffset)
                }];

            } else {
                const startBlock = blocks[0],
                    endBlock = blocks[blocks.length - 1];

                const nextStartBlock = {
                    ...startBlock,
                    content: startBlock.content.substring(0, startBlockOffset)
                };

                const nextEndBlock = {
                    ...endBlock,
                    content: endBlock.content.substring(endBlockOffset)
                };

                nextResults = [
                    nextStartBlock,
                    appendingResult,
                    nextEndBlock
                ];
            }

            result.splice(startIndex, blocks.length, ...nextResults);
            currentContentIndex += splitResult.token.pattern.length;
        }
    }

    return result.filter(res => res.content.length > 0);
}
