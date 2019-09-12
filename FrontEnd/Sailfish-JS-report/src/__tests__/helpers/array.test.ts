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

import { areArraysEqual } from "../../helpers/array";

describe('[Helpers] arrays', () => {
    test('Same array', () => {
        const arr = [1, 2, 3];

        const result = areArraysEqual(arr, arr);

        expect(result).toBe(true);
    })
    
    test('Equal arrays', () => {
        const arr1 = [1, 2, 3],
            arr2 = [1, 2, 3];

        const result = areArraysEqual(arr1, arr2);

        expect(result).toBe(true);
    })

    test('Different arrays', () => {
        const arr1 = [1, 2, 3],
            arr2 = [4, 5, 6];

        const result = areArraysEqual(arr1, arr2);

        expect(result).toBe(false);
    })

    test('Different length arrays', () => {
        const arr1 = [1, 2, 3],
            arr2 = ['test', 2];

        const result = areArraysEqual(arr1, arr2);

        expect(result).toBe(false);
    })
    
    test('Empty arrays', () => {
        const empty1 = [],
            empty2 = [];

        const result = areArraysEqual(empty1, empty2);

        expect(result).toBe(true);
    })
})
