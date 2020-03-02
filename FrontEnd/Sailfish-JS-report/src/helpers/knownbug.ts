/*******************************************************************************
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
 *  limitations under the License.
 ******************************************************************************/

import KnownBug, { isKnownBug, KnownBugNode } from "../models/KnownBug";
import { isKnownBugCategory } from "../models/KnownBugCategory";

export interface CategoryChainBugs {
    categoriesChain: string[];
    categoryBugs: KnownBug[];
}

export function getCategoryBugChains(nodes: KnownBugNode[], prevChain: string[] = []): CategoryChainBugs[] {
    const current: CategoryChainBugs = {
        categoriesChain: prevChain,
        categoryBugs: nodes.filter(isKnownBug)
    };

    const subNodesChains = nodes
        .filter(isKnownBugCategory)
        .flatMap(cat => getCategoryBugChains(cat.subNodes, [...prevChain, cat.name]));

    const resultNodes = [current, ...subNodesChains]
        .filter(({ categoryBugs }) => categoryBugs.length > 0);

    const categoriesMap = new Map<string, CategoryChainBugs>();

    resultNodes.forEach((chain) => {
        const currentKey = chain.categoriesChain.join('-');

        if (categoriesMap.has(currentKey)) {
            categoriesMap.get(currentKey).categoryBugs.push(...chain.categoryBugs);
        } else {
            categoriesMap.set(currentKey, chain);
        }
    });

    return [...categoriesMap.values()];
}
