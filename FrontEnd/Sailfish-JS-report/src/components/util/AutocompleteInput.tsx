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
import AutosizeInput from "react-input-autosize";

const STUB_FUNCTION = () => {
};

interface Props<T extends string> {
    className?: string;
    value: T;
    readonly?: boolean;
    onlyAutocompleteValues?: boolean;
    autoresize?: boolean;
    onSubmit: (nextValue: T) => void;
    onRemove?: () => void;
    onEmptyBlur?: () => void;
    autocomplete: T[] | null;
    datalistKey?: string;
    placeholder?: string;
    submitKeyCodes?: number[];
}

const AutocompleteInput = React.forwardRef(function AutocompleteInput<T extends string>(props: Props<T>, ref: React.MutableRefObject<HTMLInputElement>) {
    const {
        value,
        onSubmit,
        onRemove = STUB_FUNCTION,
        onEmptyBlur = STUB_FUNCTION,
        autocomplete,
        autoresize = true,
        readonly = false,
        onlyAutocompleteValues = true,
        datalistKey,
        className = '',
        placeholder = '',
        submitKeyCodes = [KeyCodes.ENTER]
    } = props;

    const [currentValue, setCurrentValue] = React.useState<string>(value);

    React.useEffect(() => {
        setCurrentValue(value);
    }, [value]);

    const onChange: React.ChangeEventHandler<HTMLInputElement> = e => {
        if (autocomplete?.some(val => val.toUpperCase() === e.target.value.toUpperCase())) {
            onSubmit(e.target.value as T);
            setCurrentValue('');
        } else {
            setCurrentValue(e.target.value)
        }
    };

    const onKeyDown: React.KeyboardEventHandler<HTMLInputElement> = e => {
        if (submitKeyCodes.includes(e.keyCode) && currentValue.length > 0) {
            if (!onlyAutocompleteValues || (autocomplete == null || autocomplete.includes(currentValue as T))) {
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

    const inputProps: React.InputHTMLAttributes<HTMLInputElement> = {
        readOnly: readonly,
        value: currentValue,
        list: datalistKey,
        placeholder,
        onKeyDown,
        onChange,
        onBlur: () => currentValue.length == 0 && onEmptyBlur()
    };

    return (
        <React.Fragment>
            {
                autoresize ? (
                    <AutosizeInput
                        {...inputProps}
                        inputRef={input => ref.current = input}
                        inputClassName={className}
                    />
                ) : (
                    <input
                        {...inputProps}
                        ref={ref}
                        className={className}
                    />
                )
            }
            <datalist id={datalistKey}>
                {
                    autocomplete?.map((variant, index) => (
                        <option key={index} value={variant}/>
                    ))
                }
            </datalist>
        </React.Fragment>
    )
});

export default AutocompleteInput;
