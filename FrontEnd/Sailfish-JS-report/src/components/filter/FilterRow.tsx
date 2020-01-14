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
import {FilterPath, FILTER_PATH_VALUES} from '../../helpers/filter/FilterConfig';
import KeyCodes from "../../util/KeyCodes";
import FilterBubble from "./FilterBubble";
import {removeByIndex, replaceByIndex} from "../../helpers/array";
import {statusValues} from "../../models/Status";

interface Props {
    path: FilterPath | 'type';
    values: string[];
    onChange: (nextValues: string[], path?: FilterPath) => void;
    onRemove?: () => void;
}

const autocompleteMap = new Map([
    [FilterPath.STATUS, statusValues],
    ['type', ['action', 'message', 'verification']]
]);

function FilterRow({path, values, onChange, onRemove}: Props) {
    const [currentValue, setValue] = React.useState('');
    const input = React.useRef<HTMLInputElement>();

    React.useEffect(() => {
        input.current?.focus();
    }, []);

    const onKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (e) => {
        if (e.keyCode == KeyCodes.ENTER && currentValue.length > 0) {
            if (path == FilterPath.ALL && values.length == 0 && currentValue.startsWith('#')) {
                onChange(values, currentValue.substr(1) as FilterPath);
            } else {
                onChange([...values, currentValue]);
            }

            setValue('');
            return;
        }

        if (e.keyCode == KeyCodes.BACKSPACE && currentValue.length == 0 && values.length > 0) {
            const lastValue = values[values.length - 1],
                restValues = values.slice(0, values.length - 1);

            e.preventDefault();
            setValue(lastValue);
            onChange(restValues);
            return;
        }

        if (e.keyCode == KeyCodes.BACKSPACE && currentValue.length == 0 && values.length == 0) {
            onRemove && onRemove();
        }
    };

    const inputOnBlur = () => {
        if (currentValue.length == 0 && values.length == 0 && FilterPath.ALL) {
            onRemove && onRemove();
        }
    };

    return (
        <div className="filter__row">
            <FilterBubble
                className="filter__path"
                value={path}
                prefix="#"
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
                            autocompleteVariants={autocompleteMap.get(path) ?? []}
                        />
                        <span>or</span>
                    </React.Fragment>
                ))
            }
            <input
                className="filter__row-input"
                ref={input}
                value={currentValue}
                onChange={e => setValue(e.target.value)}
                onKeyDown={onKeyDown}
                onBlur={inputOnBlur}
            />
        </div>
    )
}

export default FilterRow;
