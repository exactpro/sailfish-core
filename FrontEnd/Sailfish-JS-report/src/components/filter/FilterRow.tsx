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
import FilterBubble from "./FilterBubble";
import {removeByIndex, replaceByIndex} from "../../helpers/array";
import AutocompleteInput from "../util/AutocompleteInput";
import FilterPath, {FILTER_PATH_PREFIX, FILTER_PATH_VALUES, isFilterPath} from "../../models/filter/FilterPath";

interface Props {
    path: FilterPath | 'type';
    values: string[];
    index: number;
    onChange: (nextValues: string[], path?: FilterPath) => void;
    onRemove?: () => void;
    autocompleteVariants: string[] | undefined;
    validateAutocomplete?: boolean;
}

function FilterRow({path, index, values, onChange, onRemove = () => {}, autocompleteVariants, validateAutocomplete = true}: Props) {
    const [currentValue, setValue] = React.useState('');
    const input = React.useRef<HTMLInputElement>();

    React.useEffect(() => {
        input.current?.focus();
    }, []);

    const inputOnRemove = () => {
        if (values.length == 0) {
            onRemove && onRemove();
            return;
        }

        const lastValue = values[values.length - 1],
            restValues = values.slice(0, values.length - 1);

        setValue(lastValue);
        onChange(restValues);
    };

    const inputOnSubmit = (nextValue: string) => {
        if (values.length == 0 && nextValue.startsWith(FILTER_PATH_PREFIX)) {
            const nextPath = nextValue.substring(FILTER_PATH_PREFIX.length);

            if (isFilterPath(nextPath)) {
                onChange(values, nextPath);
            }
        } else {
            onChange([...values, nextValue]);
            setValue('');
        }
    };

    const inputOnEmptyBlur = () => {
        if (values.length == 0 && path == FilterPath.ALL) {
            onRemove();
        }
    };

    return (
        <div className="filter__row">
            <FilterBubble
                className="filter__path"
                value={path}
                prefix={FILTER_PATH_PREFIX}
                autocompleteVariants={FILTER_PATH_VALUES}
                onChange={nextPath => onChange(values, nextPath as FilterPath)}
                onRemove={() => onChange(values, FilterPath.ALL)}/>
            {
                values.map((val, index) => (
                    <React.Fragment key={index}>
                        <FilterBubble
                            value={val}
                            onChange={nextValue => onChange(replaceByIndex(values, index, nextValue))}
                            onRemove={() => onChange(removeByIndex(values, index))}
                            autocompleteVariants={autocompleteVariants != null ? [val, ...autocompleteVariants] : []}
                        />
                        <span>or</span>
                    </React.Fragment>
                ))
            }
            <AutocompleteInput
                ref={input}
                className="filter__row-input"
                value={currentValue}
                onSubmit={inputOnSubmit}
                onRemove={inputOnRemove}
                onEmptyBlur={inputOnEmptyBlur}
                readonly={autocompleteVariants != null && autocompleteVariants.length == 0}
                validateAutocomplete={validateAutocomplete}
                autocomplete={autocompleteVariants ?? []}
                datalistKey={`autocomplete-${index}`}/>
        </div>
    )
}

export default FilterRow;
