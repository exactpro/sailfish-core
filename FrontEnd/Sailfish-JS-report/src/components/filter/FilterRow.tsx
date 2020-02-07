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
import Bubble from "../util/Bubble";
import { complement, removeByIndex, replaceByIndex } from "../../helpers/array";
import AutocompleteInput from "../util/AutocompleteInput";
import FilterPath, { isFilterPath } from "../../models/filter/FilterPath";
import { FilterBlock } from "../../models/filter/FilterBlock";
import NullableFields from "../../models/util/NullableFields";
import FilterType from "../../models/filter/FilterType";
import { StatusType } from "../../models/Status";
import Select from "../util/Select";

interface Props {
    block: NullableFields<FilterBlock>;
    rowIndex: number;
    onChange: (nextBlock: FilterBlock) => void;
    onRemove?: () => void;
}

const TYPE_PATH_PREFIX = '#',
    ALL_FILTER_TYPE = 'All';

const TYPE_OPTIONS = [ALL_FILTER_TYPE, FilterType.MESSAGE, FilterType.ACTION],
    PATH_OPTIONS = [FilterPath.ALL, FilterPath.STATUS, FilterPath.SERVICE];

const AUTOCOMPLETE_MAP = new Map<FilterPath, string[]>([
    [FilterPath.STATUS, [StatusType.PASSED, StatusType.FAILED, StatusType.CONDITIONALLY_PASSED, StatusType.CONDITIONALLY_FAILED]]
]);

export default function FilterRow(props: Props) {
    const { block, rowIndex, onChange, onRemove = () => null } = props;

    const [currentValue, setValue] = React.useState('');
    const [tempBlock, setTempBlock] = React.useState<NullableFields<FilterBlock>>(block);

    React.useEffect(() => {
        if (tempBlock != block) {
            setTempBlock(block);
        }
    }, [block]);

    const { values, path, types } = tempBlock;
    const input = React.useRef<HTMLInputElement>();

    React.useEffect(() => {
        input.current?.focus();
    }, []);

    const submitChange = (nextBlock: NullableFields<FilterBlock>) => {
        onChange({
            types: nextBlock.types ?? [FilterType.ACTION, FilterType.MESSAGE],
            path: nextBlock.path ?? FilterPath.ALL,
            values: nextBlock.values ?? []
        })
    };

    const inputOnRemove = () => {
        if (values.length == 0) {
            onRemove();
            return;
        }

        const lastValue = values[values.length - 1],
            restValues = values.slice(0, values.length - 1);

        setValue(lastValue);
        submitChange({
            ...block,
            values: restValues
        });
    };

    const inputOnSubmit = (nextValue: string) => {
        if (values.length > 0 && path != null && types != null) {
            submitChange({
                ...tempBlock,
                values: [...values, nextValue]
            });
            return;
        }

        if (nextValue.startsWith(TYPE_PATH_PREFIX) && types == null) {
            const nextTypes = nextValue.substring(TYPE_PATH_PREFIX.length);

            if (TYPE_OPTIONS.includes(nextTypes)) {
                setTempBlock({
                    ...tempBlock,
                    types: getTypesByOption(nextTypes)
                });
                return;
            }
        }

        if (nextValue.startsWith(TYPE_PATH_PREFIX) && path == null) {
            const nextPath = nextValue.substring(TYPE_PATH_PREFIX.length);

            if (isFilterPath(nextPath)) {
                setTempBlock({
                    ...tempBlock,
                    path: nextPath
                });

                return;
            }
        }

        submitChange({
            ...tempBlock,
            values: [...values, nextValue]
        })
    };

    const valueBubbleOnChangeFor = (index: number) => (nextValue: string) => {
        submitChange({
            ...tempBlock,
            values: replaceByIndex(values, index, nextValue)
        })
    };

    const valueBubbleOnRemoveFor = (index: number) => () => {
        submitChange({
            ...tempBlock,
            values: removeByIndex(values, index)
        })
    };

    const getAutocomplete = () => {
        if (types == null) {
            return TYPE_OPTIONS.map(option => TYPE_PATH_PREFIX + option);
        }

        if (path == null) {
            return PATH_OPTIONS.map(option => TYPE_PATH_PREFIX + option);
        }

        if (AUTOCOMPLETE_MAP.has(path)) {
            return complement(AUTOCOMPLETE_MAP.get(path), values);
        }

        return null;
    };

    const getPlaceholder = () => {
        if (types == null) {
            return TYPE_PATH_PREFIX + FilterType.MESSAGE;
        }

        if (path == null) {
            return TYPE_PATH_PREFIX + FilterPath.SERVICE;
        }

        return '';
    };

    const bubbleIsValid = (value: string) => {
        if (AUTOCOMPLETE_MAP.has(path)) {
            return AUTOCOMPLETE_MAP.get(path).includes(value);
        }

        return true;
    };

    return (
        <div className="filter-row__wrapper">
            {
                types != null ? (
                    <Select
                        className="filter-row__select"
                        options={TYPE_OPTIONS}
                        selected={getTypesOptionName(types)}
                        onChange={option => submitChange({...tempBlock, types: getTypesByOption(option)})}
                    />
                ) : null
            }
            {
                path != null ? (
                    <React.Fragment>
                        by
                        <Select
                            className="filter-row__select"
                            options={PATH_OPTIONS}
                            selected={path}
                            onChange={(nextPath: FilterPath) => submitChange({...tempBlock, path: nextPath})}
                        />
                    </React.Fragment>
                ) : null
            }
            {
                values?.map((value, index) => (
                    <React.Fragment key={index}>
                        <Bubble
                            className="filter__bubble"
                            value={value}
                            isValid={bubbleIsValid(value)}
                            onSubmit={valueBubbleOnChangeFor(index)}
                            onRemove={valueBubbleOnRemoveFor(index)}
                            autocompleteVariants={getAutocomplete()}
                        />
                        <span>or</span>
                    </React.Fragment>
                ))
            }
            <AutocompleteInput
                ref={input}
                className="filter-row__input"
                value={currentValue}
                onSubmit={inputOnSubmit}
                onRemove={inputOnRemove}
                readonly={false}
                autoresize={false}
                placeholder={getPlaceholder()}
                onlyAutocompleteValues={path != null && types != null && AUTOCOMPLETE_MAP.has(path)}
                autocomplete={getAutocomplete()}
                datalistKey={`autocomplete-${rowIndex}`}
            />
        </div>
    )
}

function getTypesOptionName(types: FilterType[]): FilterType | typeof ALL_FILTER_TYPE {
    if (types.length > 1) {
        return ALL_FILTER_TYPE;
    }

    return types[0];
}

function getTypesByOption(option: string): FilterType[] {
    if (option == ALL_FILTER_TYPE) {
        return [FilterType.ACTION, FilterType.MESSAGE];
    }

    return [option as FilterType];
}
