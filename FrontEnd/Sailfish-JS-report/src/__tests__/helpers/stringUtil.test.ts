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

import { replaceNonPrintableChars } from "../../helpers/stringUtils";

const MIDDLE_DOT = '\u00b7',
    WHITE_SQUARE = '\u25a1';

describe('[Helpers] stringUtil tests', () => {

    test('replaceNonPrintableChars() with spaces', () => {
        const str = '  test  ';

        const result = replaceNonPrintableChars(str);

        const excpectedResult = `${MIDDLE_DOT.repeat(2)}test${MIDDLE_DOT.repeat(2)}`;

        expect(result).toBe(excpectedResult);
    })
    
    test('replaceNonPrintableChars() with unprintable chars', () => {
        const str = '\uFEFF';

        const result = replaceNonPrintableChars(str);

        const expectedResult = `${WHITE_SQUARE}`;

        expect(result).toBe(expectedResult);
    })
    
})
