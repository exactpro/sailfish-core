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
import useOutsideClickListener from "../../hooks/useOutsideClickListener";
import KeyCodes from "../../util/KeyCodes";
import AutocompleteInput from "../util/AutocompleteInput";

interface Props {
    className?: string;
    value: string;
    prefix?: string;
    readonly?: boolean;
    autocompleteVariants: string[];
    onChange: (nextValue: string) => void;
    onRemove: () => void;
}

export default function FilterBubble({value, onChange, onRemove, autocompleteVariants, className, prefix = '', readonly = false}: Props) {
    const [isEditing, setIsEditing] = React.useState(false);

    const rootRef = React.useRef<HTMLDivElement>();
    const inputRef = React.useRef<HTMLInputElement>();

    React.useEffect(() => {
        if (isEditing) {
            inputRef.current?.select();
        }
    }, [isEditing]);

    useOutsideClickListener(rootRef, () => {
        setIsEditing(false);
    });

    const rootOnClick = () => {
        if (!readonly && !isEditing) {
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

    return (
        <div
            className={`filter__bubble ${className ?? ''}`}
            ref={rootRef}
            onClick={rootOnClick}>
            {
                isEditing ? (
                    <AutocompleteInput
                        ref={inputRef}
                        className="filter__bubble-input"
                        value={value}
                        onSubmit={inputOnSubmit}
                        onRemove={onRemove}
                        onEmptyBlur={onRemove}
                        autocomplete={autocompleteVariants}
                        datalistKey="bubble-autocomplete"
                    />
                ) : prefix + value
            }
        </div>
    )
}


