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

/**
 * Returns next item after current index in array if it exists, or retruns first item if it doesn't exist.
 * @param array Target array
 * @param item Current item
 */
export function nextCyclicItemByIndex<T>(array: Array<T>, index: number): T {
    return array[(index + 1) % array.length];
}

/**
 * Returns previous item before current index in array if it exists, or retruns last item if it doesn't exist.
 * @param array Target array
 * @param item Current item
 */
export function prevCyclicItemByIndex<T>(array: Array<T>, index: number): T {
    return array[(array.length + index - 1) % array.length];
}

/**
 * Returns next item after current in array if it exists, or retruns first item if it doesn't exist.
 * @param array Target array
 * @param item Current item
 */
export function nextCyclicItem<T>(array: Array<T>, item: T): T {
    const currentIndex = array.indexOf(item);

    if (currentIndex == -1) {
        return null;
    }

    return nextCyclicItemByIndex(array, currentIndex);
}

/**
 * Returns previous item before current in array if it exists, or retruns last item if it doesn't exist.
 * @param array Target array
 * @param item Current item
 */
export function prevCyclicItem<T>(array: Array<T>, item: T): T {
    const currentIndex = array.indexOf(item);

    if (currentIndex == -1) {
        return null;
    }

    return prevCyclicItemByIndex(array, currentIndex);
}

/**
 * Returns next item after current item in array if it exists, or retruns first item if it doesn't exist.
 * @param array Target array
 * @param item Current item
 */
export function findNextCyclicItem<T>(array: Array<T>, predicateFn: (item: T) => boolean): T {
    const item = array.find(predicateFn);
    
    return nextCyclicItem(array, item);
}

/**
 * Returns previous item before current item in array if it exists, or retruns last item if it doesn't exist.
 * @param array Target array
 * @param item Current item
 */
export function findPrevCyclicItem<T>(array: Array<T>, predicateFn: (item: T) => boolean): T {
    const item = array.find(predicateFn);

    return prevCyclicItem(array, item);
}