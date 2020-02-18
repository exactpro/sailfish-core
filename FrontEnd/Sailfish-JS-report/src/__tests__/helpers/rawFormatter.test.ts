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

import * as Raw from "../../helpers/rawFormatter";

describe('[Helpers] rawFormatter tests', () => {

    test('mapOctetOffsetsToHumanReadableOffsets() with empty range', () => {
        // emty range should be mapped to empty range
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(0, 0)).toStrictEqual([0, 0]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(1, 1)).toStrictEqual([0, 0]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(2, 2)).toStrictEqual([1, 1]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(3, 3)).toStrictEqual([1, 1]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(4, 4)).toStrictEqual([2, 2]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(5, 5)).toStrictEqual([2, 2]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(6, 6)).toStrictEqual([2, 2]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(7, 7)).toStrictEqual([3, 3]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(8, 8)).toStrictEqual([3, 3]);
    })

    test('mapOctetOffsetsToHumanReadableOffsets() with non-empty range', () => {
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(0, 1)).toStrictEqual([0, 1]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(0, 2)).toStrictEqual([0, 1]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(1, 2)).toStrictEqual([0, 1]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(1, 3)).toStrictEqual([0, 2]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(2, 3)).toStrictEqual([1, 2]);
        // second column:
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(5, 6)).toStrictEqual([2, 3]);
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(6, 7)).toStrictEqual([2, 3]);
        // second line:
        expect(Raw.mapOctetOffsetsToHumanReadableOffsets(40, 41)).toStrictEqual([17, 18]);
    })

    test('mapHumanReadableOffsetsToOctetOffsets() with empty range', () => {
        // emty range should be mapped to empty range
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(0, 0)).toStrictEqual([0, 0]);
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(1, 1)).toStrictEqual([2, 2]);
    })

    test('mapHumanReadableOffsetsToOctetOffsets() with non-empty range', () => {
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(0, 1)).toStrictEqual([0, 2]);
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(0, 2)).toStrictEqual([0, 4]);
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(1, 2)).toStrictEqual([2, 4]);
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(1, 3)).toStrictEqual([2, 7]);
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(2, 3)).toStrictEqual([4, 7]);
        // second line:
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(17, 18)).toStrictEqual([40, 42]);
        expect(Raw.mapHumanReadableOffsetsToOctetOffsets(17, 19)).toStrictEqual([40, 44]);
    })
    
})
