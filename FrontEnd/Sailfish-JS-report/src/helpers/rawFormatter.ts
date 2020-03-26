/******************************************************************************
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
 * limitations under the License.
 ******************************************************************************/

import { replaceNonPrintableCharsWithDot } from './stringUtils';
import Message from '../models/Message';

 /**
 * Generates columns for rendering and copying.
 * It splits raw content into 4 lines:
 * - offset
 * - hexadecimal
 * - human-readable (for copying)
 * - human-readable without unprintable characters (for rendering)
 * @param rawBase64Content raw base64 encoded content
 * @return {string[]}
 */
export function getRawContent([offset, hexadecimal, humanReadable]: string[][]): string[] {
    return [
        offset.map(offsetRow => offsetRow + ":").join("\n"), 
        hexadecimal.join("\n"), 
        humanReadable.join(""),
        humanReadable.map(replaceNonPrintableCharsWithDot).join("\n"),
    ]
}

 /**
 * Combines offset, hexadecimal and human-readable columns into single table
 * @param decodedRawContent
 * @return {string}
 */
export function getAllRawContent([offset, hexadecimal, humanReadable]: string[][]): string {
    let allContent = '';
    for (let i = 0; i < offset.length; i++) {
        const minHexadecimalColumnLength = 8 * 4 + 7;
        const row = [
            offset[i] + ':',
            hexadecimal[i] + " ".repeat(minHexadecimalColumnLength - hexadecimal[i].length),
            humanReadable[i],
            "\n"
        ].join(" ");
        allContent += row;
    }
    return allContent;
}

 /**
 * Decodes base64 string and splits a raw content into 3 groups: 
 * offset, hexadecimal representation and human-readable.
 * @param rawBase64Content raw base64 encoded content
 * @return {string[][]} firts item - offset, second item - hexadecimal representation, third item - human-readable group.
 */
export function decodeBase64RawContent(rawBase64Content: string): string[][] {
    const offset = [],
        hexadecimal = [],
        humanReadable = [];
    const raw = Uint16Array.from(atob(rawBase64Content), c => c.charCodeAt(0));
    let index = 0;
    let length = raw.length;
    while (index < length) {
        let offsetColumn = "", 
            hexadecimalColumn = "",
            humanReadableColumn = "";

        offsetColumn = index.toString(16).padStart(8, "0");

        let rowIndex = 0;
        while (rowIndex < 16 && index < length) {
            let b = raw[index];
            let xs = b.toString(16);
            if (xs.length < 2) {
                hexadecimalColumn += 0;
                hexadecimalColumn += xs;
            } else {
                // remove extra ffffff characters if any
                hexadecimalColumn += xs.substring(xs.length - 2);
            }
            if (rowIndex % 2 == 1 && rowIndex < 15) {
                hexadecimalColumn += " ";
            }
            humanReadableColumn += String.fromCharCode(b);
            index++;
            rowIndex++;
        }
        offset.push(offsetColumn);
        hexadecimal.push(hexadecimalColumn);
        humanReadable.push(humanReadableColumn);
    }

    return [
        offset, 
        hexadecimal,
        humanReadable
    ];
}

/**
 * This function returns pair of offsets that form range [startOffset, endOffset] of
 * human readable symbols to be highlited (endOffset not incluede).
 */
export function mapOctetOffsetsToHumanReadableOffsets(start: number, end: number) {
    // legend:
    // 40 = length of octet line
    // 17 = length of human-readable line: 16 human readable chars + '\n'
    const startOffset =
        Math.floor(start / 40) * 17 +    // line
        Math.floor(start % 40 / 5) * 2 + // symbols
        Math.floor(start % 40 % 5 / 2);  // correction when only one byte from octet has been selected
    const endOffset = 
        Math.floor(end / 40) * 17 +
        Math.floor(end % 40 / 5) * 2 +
        Math.ceil(end % 40 % 5 / 2);
    return start == end ? [startOffset, startOffset] : [startOffset, endOffset];
}

/**
 * This function returns pair of offsets that form range [startOffset, endOffset] of
 * octet symbols to be highlited (endOffset not incluede).
 */
export function mapHumanReadableOffsetsToOctetOffsets(start: number, end: number) {
    // legend:
    // 40 = length of octet line
    // 17 = length of human-readable line: 16 human readable chars + '\n'
    const startOffset =
        Math.floor(start / 17) * 40 +   // lines 
        start % 17 * 2 +                // symbols
        Math.max(0, Math.floor((start % 17 -1) / 2)); // space between octets
    const endOffset = 
        Math.floor(end / 17) * 40 +
        end % 17 * 2 +
        Math.max(0, Math.floor((end % 17 -1) / 2));
        
    return start == end ? [startOffset, startOffset] : [startOffset, endOffset];
}

/**
 * This function takes messages and required contentTypes as parameters
 * and returns messages content joined by empty line
 */
export function getMessagesContent(
    messages: Message[], 
    contentTypes: ('contentHumanReadable' | 'hexadecimal' | 'raw')[]
    ): string {
    return messages
        .map(msg =>
            contentTypes
                .filter(type => {
                    // 'hexadecimal' isn't a message property and relies on 'raw' field
                    if (type === 'hexadecimal') {
                        return msg['raw'] !== null;
                    }

                    return msg[type] !== null;
                })
                .map(type => {
                    switch(type) {
                        case 'contentHumanReadable':
                            return msg[type];
                        case 'hexadecimal': {
                            const decodedRawMessage = decodeBase64RawContent(msg['raw']);
                            return getAllRawContent(decodedRawMessage).replace(/\n$/, '');
                        }
                        case 'raw': {
                            const [,,humanReadable] = decodeBase64RawContent(msg['raw']);
                            return humanReadable.join('');
                        }
                        default:
                            return '';
                    }
                })
                .join('\n')
        )
        .filter(Boolean)
        .join('\n\n');
}
