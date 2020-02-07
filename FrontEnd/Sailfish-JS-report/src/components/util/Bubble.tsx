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
import AutocompleteInput from "./AutocompleteInput";
import { stopPropagationHandler } from "../../helpers/react";
import { createBemBlock, createBemElement } from "../../helpers/styleCreators";
import "../../styles/bubble.scss";
import KeyCodes from "../../util/KeyCodes";

interface Props {
    className?: string;
    size?: 'small' | 'medium' | 'large';
    style?: React.CSSProperties;
    removeIconType?: 'default' | 'white';
    value: string;
    isValid?: boolean;
    autocompleteVariants?: string[] | null;
    submitKeyCodes?: number[];
    onSubmit?: (nextValue: string) => void;
    onRemove: () => void;
}

export default function Bubble(props: Props) {
    const {
        value,
        autocompleteVariants,
        onRemove,
        onSubmit = () => null,
        className = '',
        size = 'medium',
        removeIconType = 'default',
        isValid = true,
        style = {},
        submitKeyCodes = [KeyCodes.ENTER]
    } = props;

    const [isEditing, setIsEditing] = React.useState(false);
    const inputRef = React.useRef<HTMLInputElement>();

    React.useEffect(() => {
        if (isEditing) {
            inputRef.current?.select();
        }
    }, [isEditing]);

    const onBlur = () => {
        if (inputRef.current?.value == '') {
            onRemove();
        }

        setIsEditing(false);
    };

    const rootOnClick = () => {
        if (!isEditing) {
            setIsEditing(true);
        }
    };

    const inputOnSubmit = (nextValue: string) => {
        if (nextValue.length == 0) {
            onRemove();
            return;
        }

        onSubmit(nextValue);
        setIsEditing(false);
    };

    const rootClass = createBemBlock(
        "bubble",
        size,
        !isValid && !isEditing ? 'invalid' : null
    );

    const iconClass = createBemElement(
        "bubble",
        "remove-icon",
        removeIconType
    );

    return (
        <div
            className={`${className} ${rootClass}`}
            style={style}
            onBlur={onBlur}
            onClick={rootOnClick}>
            {
                isEditing ? (
                    <AutocompleteInput
                        ref={inputRef}
                        className="bubble__input"
                        value={value}
                        onSubmit={inputOnSubmit}
                        onRemove={onRemove}
                        onEmptyBlur={onRemove}
                        autocomplete={autocompleteVariants}
                        datalistKey="bubble-autocomplete"
                        submitKeyCodes={submitKeyCodes}
                    />
                ) : (
                    <React.Fragment>
                        {value}
                        <div className="bubble__remove">
                            <div className={iconClass} onClick={stopPropagationHandler(onRemove)}/>
                        </div>
                    </React.Fragment>
                )
            }
        </div>
    )
}
