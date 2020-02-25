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
import '../styles/buttons.scss';
import { createStyleSelector } from '../helpers/styleCreators';

type ToggleButtonContent = string | React.ReactNode

interface Props {
    onClick?: Function;
    isToggled?: boolean;
    isDisabled?: boolean;
    title?: string;
    theme?: string;
    textClass?: string;
    children: ToggleButtonContent;
}

export function ToggleButton({ onClick = () => { },
    isToggled = false,
    theme = 'default',
    isDisabled = false,
    title = '',
    textClass,
    children: content }: Props) {

    const className = createStyleSelector(
        "toggle-button", 
        theme, 
        isDisabled ? "disabled" : null,
        isToggled ? "toggled" : null
    );

    if (textClass) {
        content = (
            <span className={textClass}>
                {content}
            </span>
        )
    }

    return (
        <div className={className} onClick={() => !isDisabled && onClick(content)} title={title}>
            <div className="toggle-button__title">
                {content}
            </div>
        </div>
    )
}
