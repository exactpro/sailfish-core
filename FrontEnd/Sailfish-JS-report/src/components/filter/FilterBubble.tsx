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
import AutocompleteInput from "../util/AutocompleteInput";
import { stopPropagationHandler } from "../../helpers/react";
import { createBemBlock } from "../../helpers/styleCreators";

interface Props {
    className?: string;
    value: string;
    isValid?: boolean;
    autocompleteVariants: string[] | null;
    onChange: (nextValue: string) => void;
    onRemove: () => void;
}

export default function FilterBubble({ value, onChange, onRemove, autocompleteVariants, className = '', isValid = true }: Props) {
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

        onChange(nextValue);
        setIsEditing(false);
    };

    const rootClass = createBemBlock(
        "filter-bubble",
        !isValid && !isEditing ? 'invalid' : null
    );

    return (
        <div
            className={`${rootClass} ${className}`}
            onBlur={onBlur}
            onClick={rootOnClick}>
            {
                isEditing ? (
                    <AutocompleteInput
                        ref={inputRef}
                        className="filter-bubble__input"
                        value={value}
                        onSubmit={inputOnSubmit}
                        onRemove={onRemove}
                        onEmptyBlur={onRemove}
                        autocomplete={autocompleteVariants}
                        datalistKey="bubble-autocomplete"
                    />
                ) : (
                    <React.Fragment>
                        {value}
                        <div className="filter-bubble__remove">
                            <div className="filter-bubble__remove-icon" onClick={stopPropagationHandler(onRemove)}/>
                        </div>
                    </React.Fragment>
                )
            }
        </div>
    )
}
