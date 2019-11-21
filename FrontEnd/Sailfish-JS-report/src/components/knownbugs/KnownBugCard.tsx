/*******************************************************************************
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

import * as React from 'react';
import { KnownBugStatus } from '../../models/KnownBugStatus';
import { createBemElement, createBemBlock } from '../../helpers/styleCreators';
import '../../styles/knownbug.scss';
import KnownBug from "../../models/KnownBug";
import KnownBugCategory from "../../models/KnownBugCategory";
import Action from "../../models/Action";
import { StatusType } from '../../models/Status';
import ChipsList from '../ChipsList';

const RIGHT_ARROW = '\u25B6';

interface KnownBugCardProps {
    bug: KnownBug;
    parentCategories: KnownBugCategory[];
    actionsMap: Map<number, Action>;
    isSelected: boolean;
    selectedStatus: StatusType;
    onSelect: (status?: StatusType) => void;
}

export function KnownBugCard({ bug, parentCategories, actionsMap, isSelected, onSelect, selectedStatus }: KnownBugCardProps) {
    const statusClassName = bug.status === KnownBugStatus.REPRODUCED ? "reproduced" : "not-reproduced";

    const rootClassName = createBemBlock(
            'known-bug-card',
            statusClassName,
            isSelected ? 'selected' : null
        ),
        indicatorClassName = createBemElement(
            'known-bug-card',
            'indicator', 
            statusClassName
        );

    return (
        <div className={rootClassName}
            onClick={() => onSelect()}>
            <div className="known-bug-card__left">
                <ChipsList
                    actions={bug.relatedActionIds.map(id => actionsMap.get(id))}
                    onStatusSelect={onSelect}
                    selectedStatus={isSelected && selectedStatus}/>
                <div className="known-bug-card__categories">
                    {
                        parentCategories
                            .map(cat => cat.name)
                            .join(` ${RIGHT_ARROW} `)
                    }
                </div>
            </div>
            <div className="known-bug-card__right">
                <div className="known-bug-card__name">{bug.subject}</div>
                <div className={indicatorClassName} />
            </div>
        </div>
    )
}
