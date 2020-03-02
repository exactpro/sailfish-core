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

import { KnownBugNode } from "../../models/KnownBug";
import { createKnownBug, createKnownBugCategory } from "../util/creators";
import { CategoryChainBugs, getCategoryBugChains } from "../../helpers/knownbug";

describe('[Helpers] getCategoryBugChains()', () => {

    test('Single category test', () => {
        const categoryBugs = [createKnownBug(1, 'KB')];
        const category = createKnownBugCategory('test', categoryBugs);

        const nodes: KnownBugNode[] = [category];

        const result = getCategoryBugChains(nodes);

        expect(result).toEqual<CategoryChainBugs[]>([{
            categoriesChain: [category.name],
            categoryBugs: categoryBugs
        }]);
    });

    test('2 sub categories test', () => {
        const bugs = [createKnownBug(1, 'KB')];
        const childCategory = createKnownBugCategory('child', bugs);
        const parentCategory = createKnownBugCategory('parent', [childCategory]);

        const nodes: KnownBugNode[] = [parentCategory];

        const result = getCategoryBugChains(nodes);

        expect(result).toEqual<CategoryChainBugs[]>([{
            categoriesChain: [parentCategory.name, childCategory.name],
            categoryBugs: bugs
        }])
    });

    test('2 categories with same name', () => {
        const firstBugs = [createKnownBug(1)];
        const secondBugs = [createKnownBug(2), createKnownBug(3)];
        const firstCategory = createKnownBugCategory('test', firstBugs);
        const secondCategory = createKnownBugCategory('test', secondBugs);

        const nodes: KnownBugNode[] = [firstCategory, secondCategory];

        const result = getCategoryBugChains(nodes);

        expect(result).toEqual<CategoryChainBugs[]>([{
            categoriesChain: [firstCategory.name],
            categoryBugs: [...firstBugs, ...secondBugs]
        }])
    });

    test('Sub category with its own bugs.', () => {
        const childBugs = [createKnownBug(1)];
        const parentBugs = [createKnownBug(2)];
        const child = createKnownBugCategory('child', childBugs);
        const parent = createKnownBugCategory('parent', [...parentBugs, child]);

        const nodes: KnownBugNode[] = [parent];

        const result =getCategoryBugChains(nodes);

        expect(result).toEqual<CategoryChainBugs[]>([{
            categoriesChain: [parent.name],
            categoryBugs: parentBugs
        }, {
            categoriesChain: [parent.name, child.name],
            categoryBugs: childBugs
        }]);
    })
});
