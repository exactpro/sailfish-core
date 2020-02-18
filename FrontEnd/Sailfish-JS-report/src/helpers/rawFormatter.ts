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

const OFFSET_END = 9,
      HEXADECIMAL_START = 10,
      HEXADECIMAL_END = 49,
      CONTENT_START = 51;
 
/**
 * Splits a raw content into 3 lines : offset, hexadecimal representation and human-readable string.
 * @param raw raw content
 * @return {string[]} firts item - offset, second item - hexadecimal representation, third item - human-readable string
 */
export function splitRawContent(raw: string): string[] {
    // first - offset, second - hexadecimal representation, third - human-readable string
    const ret : string[] = ['', '', ''];

    raw.split('\r\n').forEach(row => {
        ret[0] += row.substring(0, OFFSET_END) + '\n';
        ret[1] += row.substring(HEXADECIMAL_START, HEXADECIMAL_END) + '\n';
        ret[2] += row.substring(CONTENT_START, row.length) + '\n';
    })

    return ret;
}

/**
 * Removes all '\r' and '\n' symbols from string.
 * @param content content string
 */
export function getUnformattedContent(content: string): string {
    return content.replace(/\r|\n/g, '');
}

/**
 * Converts HEX content to raw using 'String.fromCharCode' (may contain unreadable symbols).
 * @param hexString HEX string
 */
export function getRawContentByHex(hexString: string): string {
    // filter out line breaks
    const unforamttedString = hexString.replace(/\r|\n/g, ' '),
        // split hex string to array of string - pairs of char codes
        hexSymbols = unforamttedString.split(' '),
        filtredSymbols  = hexSymbols.filter(Boolean),
        // splitting pairs of char codes in single char codes array
        charCodes = filtredSymbols.reduce(
            (acc, charCodes) => {
                const firstCode = charCodes.substring(0, 2),
                    secondCode = charCodes.substring(2),
                    result = secondCode ? [firstCode, secondCode] : [firstCode];

                return acc.concat(result);
            },
            []
        ),
        // converting hex char code to string
        charsArray = charCodes.map(
            hexCharCode => String.fromCharCode(parseInt(hexCharCode, 16))
        ),
        resultString = charsArray.join('');

    return resultString;
}

/**
 * This function returns pair of offsets that form range [startOffset, endOffset] of
 * human readable symbols to be highlited (endOffset not incluede).
 */
export function mapOctetOffsetsToHumanReadableOffsets(start: number, end: number) {
    // legend:
    // 40 = length of octet line
    // 17 = length of humen-readable line: 16 human readable chars + '\n'
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
    // 17 = length of humen-readable line: 16 human readable chars + '\n'
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