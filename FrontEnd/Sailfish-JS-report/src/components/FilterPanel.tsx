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
import '../styles/header.scss';
import { ToggleButton } from './ToggleButton';

interface FilterPanelProps {
    actionFilterHandler: (status: StatusType) => void;
    fieldsFilterHandler: (status: StatusType) => void;
    actionsFilters: StatusType[];
    fieldsFilters: StatusType[];
}

export const FilterPanel = ({ actionFilterHandler, fieldsFilterHandler, actionsFilters, fieldsFilters }: FilterPanelProps) => {
    return (
        <div className="header-filter">
            <div className="header-filter__togglers">
                <div className="header-filter__togglers-title">Actions</div>
                <ToggleButton text="Passed"
                    isToggled={actionsFilters.includes(StatusType.PASSED)}
                    onClick={() => actionFilterHandler(StatusType.PASSED)}
                    theme="green" />
                <ToggleButton text="Failed"
                    isToggled={actionsFilters.includes(StatusType.FAILED)}
                    onClick={() => actionFilterHandler(StatusType.FAILED)}
                    theme="green" />
                <ToggleButton text="Conditioanlly passed"
                    isToggled={actionsFilters.includes(StatusType.CONDITIONALLY_PASSED)}
                    onClick={() => actionFilterHandler(StatusType.CONDITIONALLY_PASSED)}
                    theme="green" />
            </div>
            <div className="header-filter__togglers">
                <div className="header-filter__togglers-title">Fields</div>
                <ToggleButton text="Passed"
                    isToggled={fieldsFilters.includes(StatusType.PASSED)}
                    onClick={() => fieldsFilterHandler(StatusType.PASSED)}
                    theme="green" />
                <ToggleButton text="Failed"
                    isToggled={fieldsFilters.includes(StatusType.FAILED)}
                    onClick={() => fieldsFilterHandler(StatusType.FAILED)}
                    theme="green" />
                <ToggleButton text="Conditioanlly passed"
                    isToggled={fieldsFilters.includes(StatusType.CONDITIONALLY_PASSED)}
                    onClick={() => fieldsFilterHandler(StatusType.CONDITIONALLY_PASSED)}
                    theme="green" />
                <ToggleButton text="N/A"
                    isToggled={fieldsFilters.includes(StatusType.NA)}
                    onClick={() => fieldsFilterHandler(StatusType.NA)}
                    theme="green" />
            </div>
        </div>
    )
}