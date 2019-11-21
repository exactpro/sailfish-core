/*
 * ****************************************************************************
 *  Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ****************************************************************************
 */

import * as React from 'react';
import KnownBug, { isKnownBug } from '../../models/KnownBug';
import KnownBugCategory, { isKnownBugCategory } from '../../models/KnownBugCategory';
import { KnownBugBadge } from './KnownBugBadge';
import '../../styles/knownbug.scss';
import { KnownBugStatus } from '../../models/KnownBugStatus';

interface Props {
    category: KnownBugCategory;
    isRoot?: boolean;
    showArrows?: boolean;
}

export function KnownBugCategoryComponent({ category, isRoot, showArrows }: Props) {
    const topLevelBugs = filterDistinctBugs(category.subNodes.filter(isKnownBug))
        .sort((a, b) =>
            a.status === b.status
                ? a.subject.localeCompare(b.subject)
                : (a.status === KnownBugStatus.REPRODUCED ? -1 : 1)
        );

    const categories = category.subNodes
        .filter(isKnownBugCategory)
        .sort((a, b) => a.name.localeCompare(b.name));

    return (
        <div className="known-bugs__category">
            {isRoot ? null :
                <div className="known-bugs__category__name">
                    {showArrows ? "⯈ " : null} {category.name}
                </div>
            }
            <div className="known-bugs__category__container">
                <div className="known-bugs__category__container__bugs">
                {
                    topLevelBugs.map((item, index) => (
                        <KnownBugBadge bug={item} key={index} />
                    ))
                }
                </div>
                {
                    categories.map((item, index) => (
                        <KnownBugCategoryComponent 
                            category={item} 
                            showArrows={!isRoot}
                            key={index} />
                    ))
                }
            </div>

        </div>
    )
}

const filterDistinctBugs = (bugs: KnownBug[]): KnownBug[] => {
    const bugMap = new Map<string, KnownBug>();

    bugs.forEach((bug) => {
       if (!bugMap.has(bug.subject) || (bugMap.get(bug.subject).status == "NOT_REPRODUCED" && bug.status == "REPRODUCED")) {
           bugMap.set(bug.subject, bug)
       }
    });

    return Array.from(bugMap.values())
};
