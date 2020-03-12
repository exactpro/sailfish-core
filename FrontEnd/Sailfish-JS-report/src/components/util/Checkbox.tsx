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

import * as React from 'react';
import '../../styles/checkbox.scss';
import { createBemBlock } from "../../helpers/styleCreators";

interface Props {
    checked: boolean;
    label: string;
    id?: string;
    isDisabled?: boolean;
    className?: string;
    onChange: React.ChangeEventHandler<HTMLInputElement>;
}

export default function Checkbox({ checked, label, onChange, isDisabled = false, className = '', id = '' }: Props) {
    const rootClassName = createBemBlock(
        'checkbox',
        isDisabled ? 'disabled' : null
    );

    return (
        <div className={`${rootClassName} ${className}`}>
            <input
                className='checkbox__control'
                type='checkbox'
                id={id}
                checked={checked}
                onChange={onChange}
            />
            <label
                className='checkbox__label'
                htmlFor={id}>
                {label}
            </label>
        </div>
    )
}
