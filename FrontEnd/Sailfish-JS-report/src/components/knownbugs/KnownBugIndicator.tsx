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
import '../../styles/knownbug.scss';
import KnownBug from '../../models/KnownBug';
import KnownBugCategory from '../../models/KnownBugCategory';
import { ActionNodeType } from '../../models/Action';
import { KnownBugStatus } from '../../models/KnownBugStatus';
import { createBemElement } from '../../helpers/styleCreators';

interface Props {
    data: (KnownBug | KnownBugCategory)[]
}

export function KnownBugIndicator({ data }: Props) {
    const className = createBemElement(
        'known-bugs',
        'indicator',
        hasReproducedBugs(data) ? "reproduced" : "not-reproduced"
    );

    return (
        <div className={className}/>
    )
}

function hasReproducedBugs(data: (KnownBug | KnownBugCategory)[]): boolean {
    const categories = data.filter(item => item.actionNodeType === ActionNodeType.KNOWN_BUG_CATEGORY);
    const bugs = data.filter(item => item.actionNodeType === ActionNodeType.KNOWN_BUG);

    return (bugs.some((item: KnownBug) => item.status === KnownBugStatus.REPRODUCED))
        ? true
        : categories.some((item: KnownBugCategory) => hasReproducedBugs(item.subNodes));
}
