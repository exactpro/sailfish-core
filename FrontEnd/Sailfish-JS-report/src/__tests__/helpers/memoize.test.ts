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

import { memoizeLast } from '../../helpers/memoize';

describe('[Helpers] memoizeOne funciton', () => {
    
    test('Run funciton once', () => {
        const targetFn = jest.fn(),
            memoized = memoizeLast(targetFn);

        memoized();

        expect(targetFn.mock.calls.length).toBe(1);
    })

    test('Run function once with args', () => {
        const targetFn = jest.fn<number, [number, number]>(),
            memoized = memoizeLast(targetFn);

        memoized(1, 2);

        expect(targetFn.mock.calls.length).toBe(1);
        expect(targetFn.mock.calls[0][0]).toBe(1);
        expect(targetFn.mock.calls[0][1]).toBe(2);
    })
    
    test('Run funciton with the same args multiple times', () => {
        const targetFn = jest.fn<number, [number, number]>((n1, n2) => n1 + n2),
            memoized = memoizeLast(targetFn);

        const first = memoized(1, 2),
            second = memoized(1, 2),
            third = memoized(1, 2);

        expect(first).toEqual(3);
        expect(first).toEqual(second);
        expect(second).toEqual(third);
        expect(targetFn.mock.calls.length).toBe(1);
        expect(targetFn.mock.calls[0][0]).toBe(1);
        expect(targetFn.mock.calls[0][1]).toBe(2);
    })

    test('Run funciton with different args', () => {
        const targetFn = jest.fn<number, [number, number]>((n1, n2) => n1 + n2),
            memoized = memoizeLast(targetFn);

        const first  = memoized(1, 2),
            second = memoized(1, 2),
            third = memoized(2, 3);

        expect(targetFn.mock.calls.length).toEqual(2);
        expect(targetFn.mock.calls[0][0]).toBe(1);
        expect(targetFn.mock.calls[0][1]).toBe(2);
        expect(targetFn.mock.calls[1][0]).toBe(2);
        expect(targetFn.mock.calls[1][1]).toBe(3);
    })
    
})

