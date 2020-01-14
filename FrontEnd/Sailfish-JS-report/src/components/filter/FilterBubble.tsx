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
interface Props {
    className?: string;
    value: string;
    prefix?: string;
    autocompleteVariants: string[];
    onChange: (nextValue: string) => void;
    onRemove: () => void;
}
export default function FilterBubble({value, onChange, onRemove, autocompleteVariants, className, prefix = ''}: Props) {
    const [isEditing, setIsEditing] = React.useState(false);
    const [currentValue, setCurrentValue] = React.useState('');
    React.useEffect(() => {
        setCurrentValue(value);
    }, [value]);
    const rootRef = React.useRef<HTMLDivElement>();
    useOutsideClickListener(rootRef, () => {
        setIsEditing(false);
        setCurrentValue(value);
    });
    const rootOnClick = () => {
        if (!isEditing) {
            setIsEditing(true);
        }
    };
    const inputOnKeyDown = (e: React.KeyboardEvent) => {
        if (e.keyCode == KeyCodes.ENTER && currentValue.length > 0) {
            setIsEditing(false);
            onChange(currentValue);
            return;
        }
        if (e.keyCode == KeyCodes.BACKSPACE && currentValue.length == 0) {
            setIsEditing(false);
            onRemove();
            return;
        }
        if (e.keyCode == KeyCodes.ESCAPE) {
            setIsEditing(false);
            setCurrentValue(value);
        }
    };
    return (
        <div
            className={`filter__bubble ${className ?? ''}`}
            ref={rootRef}
            onClick={rootOnClick}>
            {
                isEditing ? (
                    <React.Fragment>
                        <input
                            className="filter__bubble-input"
                            list="bubble-autocomplete"
                            value={currentValue}
                            onChange={e => setCurrentValue(e.target.value)}
                            onKeyDown={inputOnKeyDown}/>
                        {
                            autocompleteVariants.length > 0 ? (
                                <datalist id="bubble-autocomplete">
                                    {
                                        autocompleteVariants.map((val, index) => (
                                            <option key={index} value={val}/>
                                        ))
                                    }
                                </datalist>
                            ) : null
                        }
                    </React.Fragment>
                ) : prefix + value
            }
        </div>
    )
}
