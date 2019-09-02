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

const TAB = ' '.repeat(4),
    NEW_LINE = '\n';

const searchRegexp = /[{}\[\];]/g;

/**
 * This function 'beautifies' human-readable content - it adds line breaks and tab indents to content
 * @param origin human-readable content
 */
export default function beautify(origin: string): string {

    let tabLevel = 0,
        result = '';

    while (origin.length > 0) {
        const index = origin.search(searchRegexp),
            targetSymbol = origin.charAt(index),
            appendingFragment = origin.substring(0, index).trimLeft();

        // nothing found
        if (index == -1) {
            result += origin.trimLeft();
            break;
        }

        origin = origin.substring(index + 1);

        const lastResultChar = result[result.length - 1],
            nextChar = origin[0];

        switch(targetSymbol) {

            case ';': {
                // we don't need indent with tabs at the end of array / object
                if ((lastResultChar === '}' || lastResultChar === ']') && appendingFragment.length === 0) {
                    result = result + targetSymbol + NEW_LINE;
                } else {
                    result = result + TAB.repeat(tabLevel) + appendingFragment + targetSymbol + NEW_LINE;
                }

                break;
            }

            case '{': {
                // we don't need to add tab indent for for first object in array
                if (lastResultChar === '[' && appendingFragment.length === 0) {
                    result = result + targetSymbol + NEW_LINE;
                } else {
                    result = result + TAB.repeat(tabLevel) + appendingFragment + targetSymbol + NEW_LINE;
                }

                tabLevel++;

                break;
            }

            case '}': {
                result = result + TAB.repeat(tabLevel) + appendingFragment + NEW_LINE + TAB.repeat(tabLevel - 1) + targetSymbol;
                tabLevel--;
                break;
            }

            case '[': {
                if (nextChar === '{') {
                    result = result + TAB.repeat(tabLevel) + appendingFragment + targetSymbol;
                } else {
                    result = result + TAB.repeat(tabLevel) + appendingFragment + targetSymbol + NEW_LINE;
                    tabLevel++;
                }

                break;
            }

            case ']': {
                // we don't need tab indent and line breaks at the end of array
                if (lastResultChar === '}') {
                    result = result + appendingFragment + targetSymbol;
                } else {
                    result = result + TAB.repeat(tabLevel) + appendingFragment + NEW_LINE + TAB.repeat(tabLevel - 1) + targetSymbol;
                    tabLevel--;
                }

                break;
            }

            default: {
                result = result + appendingFragment + targetSymbol;
            }
        }

    }

    return result;
} 
