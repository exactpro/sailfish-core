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

const OFFSET_END = 10,
      HEXADECIMAL_END = 50;
 
/**
 * Splits a raw content into 3 lines : offset, hexadecimal representation and human-readable string.
 * @param raw raw content
 * @return {string[]} firts item - offset, second item - hexadecimal representation, third item - human-readable string
 */
export function splitRawContent(raw: string): string[] {
    // first - offset, second - hexadecimal representation, third - human-readable string
    const ret : string[] = ['', '', ''];

    raw.split('\r\n').forEach(row => {
        ret[0] += row.substring(0, OFFSET_END) + '\r\n';
        ret[1] += row.substring(OFFSET_END, HEXADECIMAL_END) + '\r\n';
        ret[2] += row.substring(HEXADECIMAL_END, row.length) + '\r\n';
    })

    return ret;
}