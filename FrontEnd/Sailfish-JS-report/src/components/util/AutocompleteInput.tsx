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
import KeyCodes from "../../util/KeyCodes";

const STUB_FUNCTION = () => {
};

interface Props<T extends string> {
    className?: string;
    value: T;
    readonly?: boolean;
    validateAutocomplete?: boolean;
    onSubmit: (nextValue: T) => void;
    onRemove?: () => void;
    onEmptyBlur?: () => void;
    autocomplete: T[];
    datalistKey?: string;
}

const AutocompleteInput = React.forwardRef(<T extends string>(props: Props<T>, ref: React.Ref<HTMLInputElement>) => {
    const {
        value,
        onSubmit,
        onRemove = STUB_FUNCTION,
        onEmptyBlur = STUB_FUNCTION,
        autocomplete,
        readonly = false,
        validateAutocomplete = true,
        datalistKey,
        className = ''
    } = props;
    const [currentValue, setCurrentValue] = React.useState<string>(value);

    React.useEffect(() => {
        setCurrentValue(value);
    }, [value]);

    const onChange: React.ChangeEventHandler<HTMLInputElement> = e => {
        if (autocomplete.some(val => val.toUpperCase() === e.target.value.toUpperCase())) {
            onSubmit(e.target.value as T);
            setCurrentValue('');
        } else {
            setCurrentValue(e.target.value)
        }
    };

    const onKeyDown: React.KeyboardEventHandler<HTMLInputElement> = e => {
        if (e.keyCode == KeyCodes.ENTER && currentValue.length > 0) {
            if (!validateAutocomplete || (autocomplete.length < 1 || autocomplete.includes(currentValue as T))) {
                onSubmit(currentValue as T);
                setCurrentValue('');
            }

            return;
        }

        if (e.keyCode === KeyCodes.BACKSPACE && currentValue.length < 1) {
            onRemove();
            e.preventDefault();
        }
    };

    return (
        <React.Fragment>
            <input
                ref={ref}
                className={className}
                readOnly={readonly}
                value={currentValue}
                onKeyDown={onKeyDown}
                onChange={onChange}
                onBlur={() => currentValue.length == 0 && onEmptyBlur()}
                list={datalistKey}
            />
            <datalist id={datalistKey}>
                {
                    autocomplete.map((variant, index) => (
                        <option key={index} value={variant}/>
                    ))
                }
            </datalist>
        </React.Fragment>
    )
});

export default AutocompleteInput;
