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

import *  as React from 'react';
import '../../styles/select.scss';

interface Props {
    className?: string;
    options: string[];
    selected: string;
    prefix?: string;
    onChange: (option: string) => void;
}

export default function Select({ options, selected, onChange, className = '', prefix= '' }: Props) {
    return (
        <select
            className={`options-select ${className}`}
            value={prefix + selected}
            onChange={e => onChange(e.target.value.substring(prefix.length))}>
            {
                options.map((opt, index) => (
                    <option key={index}>{prefix + opt}</option>
                ))
            }
        </select>
    )
}
