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
import SearchSplitResult from "../../models/search/SearchSplitResult";

export default class SearchResult {

    private results: Map<string, SearchSplitResult[]>;

    constructor(initMap: Iterable<[string, SearchSplitResult[]]> = []) {
        this.results = new Map(initMap);
    }

    has = (key: string) => this.results.has(key);

    get = (key: string) => this.results.get(key);

    get size() {
        return this.results.size;
    }

    get isEmpty() {
        return this.results.size < 1;
    }

    get entries() { 
        return [...this.results.entries()];
    }

    get keys() {
        return [...this.results.keys()];
    }

    get values() {
        return [...this.results.values()];
    }

    map = <T>(fn: (entry: [string, SearchSplitResult[]]) => T): Array<T> => this.entries.map(fn);

    mapKeys = <T>(fn: (key: string) => T): Array<T> => this.keys.map(fn);

    mapValues = <T>(fn: (value: SearchSplitResult[]) => T): Array<T> => this.values.map(fn);

    sum = () => this.values.reduce((sum, value) =>
        sum + value
            .filter(res => res.token != null && res.token.isScrollable)
            .length
    , 0);

    /**
     * Returns entry which includes target index
     * @param index target index for search result
     */
    getByIndex(index: number): [string, SearchSplitResult[]] {
        let count = 0;

        const targetEntry = this.entries.find(([key, result]) => {
            count += result
                .filter(res => res.token != null && res.token.isScrollable)
                .length;
            return index < count;
        });

        return targetEntry || [undefined, undefined];
    }

    /**
     * Returns index of first search result by the key
     * @param targetKey key to search
     */
    getStartIndexForKey(targetKey: string): number {
        if (!this.results.has(targetKey)) {
            return null;
        }

        return this.values
            .slice(0, this.keys.findIndex(key => key === targetKey))
            .reduce((acc, result) =>
                acc + result
                    .filter(res => res.token != null && res.token.isScrollable)
                    .length
            , 0);
    }
}