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
import KnownBug from '../../models/KnownBug'
import KnownBugCategory from '../../models/KnownBugCategory';
import { ActionNodeType } from '../../models/Action';
import { KnownBugCard } from './KnownBugCard';
import '../../styles/knownbug.scss';
import { KnownBugStatus } from '../../models/KnownBugStatus';

interface KnownBugCategoryComponentProps {
    category: KnownBugCategory;
    isRoot?: boolean;
    showArrows?: boolean;

}

export class KnownBugCategoryComponent extends React.Component<KnownBugCategoryComponentProps, {}> {
    render() {

        const topLevelBugs = this.props.category.subNodes
            .filter(item => item.actionNodeType === ActionNodeType.KNOWN_BUG)
            .sort((a: KnownBug, b: KnownBug) =>
                a.status === b.status
                    ? a.subject.localeCompare(b.subject)
                    : (a.status === KnownBugStatus.REPRODUCED ? -1 : 1)
            );

        const categories = this.props.category.subNodes
            .filter(item => item.actionNodeType === ActionNodeType.KNOWN_BUG_CATEGORY)
            .sort((a: KnownBugCategory, b: KnownBugCategory) => a.name.localeCompare(b.name));

        return (
            <div className="known-bugs__category">
                {this.props.isRoot ? null :
                    <div className="known-bugs__category__name">
                        {this.props.showArrows ? "⯈ " : null} {this.props.category.name}
                    </div>
                }
                <div className="known-bugs__category__container">
                    <div className="known-bugs__category__container__bugs">
                        {topLevelBugs.map(item => <KnownBugCard bug={item as KnownBug} />)}
                    </div>
                    {categories.map(item => <KnownBugCategoryComponent category={item as KnownBugCategory} showArrows={!this.props.isRoot} />)}
                </div>

            </div>
        )
    }
}
