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

import * as React from 'react';
import { StatusType } from '../models/Status';
import { createBemBlock, createBemElement } from '../helpers/styleCreators';
import '../styles/header.scss';
import { stopPropagationHandler } from '../helpers/react';

interface Props {
    status: StatusType;
    transparencyFilter: boolean;
    visibilityFilter: boolean;
    transparencyFilterHandler: () => any;
    visibilityFilterHandler: () => any;
}

function StatusFilterToggler({ status, transparencyFilter, visibilityFilter, transparencyFilterHandler, visibilityFilterHandler }: Props) {
    const formattedStatus = status.replace('_', ' ');

    const rootClassName = createBemBlock(
        'filter-toggler', 
        status,
        transparencyFilter ? null : 'transparent'
    ), iconClassName = createBemElement(
        'filter-toggler',
        'icon',
        visibilityFilter ? null : 'hidden'
    );

    return (
        <div className={rootClassName}
            onClick={stopPropagationHandler(transparencyFilterHandler)}
            title='Transparency filter'>
            <div className='filter-toggler__title'>{formattedStatus}</div>
            <div className={iconClassName}
                title='Visibility filter'
                onClick={stopPropagationHandler(visibilityFilterHandler)}/>
        </div>
    )
}

export default StatusFilterToggler;
