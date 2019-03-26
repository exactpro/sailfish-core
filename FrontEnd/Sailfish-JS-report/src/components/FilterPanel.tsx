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

import { h } from 'preact';
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
        <div class="header-filter">
            <div class="header-filter-togglers">
                <h5>Actions</h5>
                <ToggleButton text="Passed"
                    isToggled={actionsFilters.includes("PASSED")}
                    onClick={() => actionFilterHandler("PASSED")}
                    theme="green" />
                <ToggleButton text="Failed"
                    isToggled={actionsFilters.includes("FAILED")}
                    onClick={() => actionFilterHandler("FAILED")}
                    theme="green" />
                <ToggleButton text="Conditioanlly passed"
                    isToggled={actionsFilters.includes("CONDITIONALLY_PASSED")}
                    onClick={() => actionFilterHandler("CONDITIONALLY_PASSED")}
                    theme="green" />
            </div>
            <div class="header-filter-togglers">
                <h5>Fields</h5>
                <ToggleButton text="Passed"
                    isToggled={fieldsFilters.includes("PASSED")}
                    onClick={() => fieldsFilterHandler("PASSED")}
                    theme="green" />
                <ToggleButton text="Failed"
                    isToggled={fieldsFilters.includes("FAILED")}
                    onClick={() => fieldsFilterHandler("FAILED")}
                    theme="green" />
                <ToggleButton text="Conditioanlly passed"
                    isToggled={fieldsFilters.includes("CONDITIONALLY_PASSED")}
                    onClick={() => fieldsFilterHandler("CONDITIONALLY_PASSED")}
                    theme="green" />
                <ToggleButton text="N/A"
                    isToggled={fieldsFilters.includes("NA")}
                    onClick={() => fieldsFilterHandler("NA")}
                    theme="green" />
            </div>
        </div>
    )
}