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

import React from 'react';
import "../../styles/filter.scss";
import {connect} from 'react-redux';
import AppState from '../../state/models/AppState';
import {ThunkDispatch} from 'redux-thunk';
import StateAction from '../../actions/stateActions';
import {performFilter} from "../../thunks/filter";
import FilterRow from "./FilterRow";
import {complement, removeByIndex, replaceByIndex} from "../../helpers/array";
import {StatusType} from "../../models/Status";
import FilterPath, {FILTER_PATH_PREFIX, FILTER_PATH_VALUES} from "../../models/filter/FilterPath";
import FilterType from "../../models/filter/FilterType";
import {FilterConfig} from "../../models/filter/FilterConfig";

const AUTOCOMPLETE_MAP = new Map<FilterPath, string[]>([
    [FilterPath.STATUS, [StatusType.PASSED, StatusType.FAILED, StatusType.CONDITIONALLY_PASSED, StatusType.CONDITIONALLY_FAILED]]
]);

const FILTER_TYPE_AUTOCOMPLETE = [FilterType.ACTION, FilterType.MESSAGE, FilterType.VERIFICATION],
    FILTER_PATH_AUTOCOMPLETE = FILTER_PATH_VALUES.map(path => FILTER_PATH_PREFIX + path);

interface StateProps {
    config: FilterConfig;
}

interface DispatchProps {
    updateConfig: (config: FilterConfig) => void;
}

interface Props extends StateProps, DispatchProps {
}

function FilterPanelBase({config, updateConfig}: Props) {

    const onTypesChange = (values: string[]) => {
        updateConfig({
            ...config,
            types: values as FilterType[]
        })
    };

    const onBlockChangeFor = (blockIndex: number) => (values: string[], path?: FilterPath) => {
        updateConfig({
            ...config,
            blocks: replaceByIndex(config.blocks, blockIndex, {
                path: path ?? config.blocks[blockIndex].path,
                values
            })
        })
    };

    const onNewBlockChange = (values: string[], path?: FilterPath) => {
        updateConfig({
            ...config,
            blocks: [...config.blocks, {
                path: path ?? FilterPath.ALL,
                values
            }]
        })
    };

    const onBlockRemoveFor = (index: number) => () => {
        updateConfig({
            ...config,
            blocks: removeByIndex(config.blocks, index)
        })
    };

    return (
        <div className="filter">
            <div className="filter__row-wrapper">
                <div className="filter__row-divider"/>
                <FilterRow
                    index={0}
                    path={'type'}
                    values={config.types}
                    autocompleteVariants={complement(FILTER_TYPE_AUTOCOMPLETE, config.types)}
                    onChange={onTypesChange}/>
            </div>
            {
                config.blocks.map((block, index) => (
                    <div className="filter__row-wrapper" key={index}>
                        <div className="filter__row-divider">and</div>
                        <FilterRow
                            path={block.path}
                            index={index + 1}
                            values={block.values}
                            autocompleteVariants={
                                AUTOCOMPLETE_MAP.has(block.path) ?
                                    complement(AUTOCOMPLETE_MAP.get(block.path), block.values) :
                                    undefined
                            }
                            onChange={onBlockChangeFor(index)}
                            onRemove={onBlockRemoveFor(index)}
                        />
                    </div>
                ))
            }
            <div className="filter__row-wrapper">
                <div className="filter__row-divider">and</div>
                <FilterRow
                    path={FilterPath.ALL}
                    index={config.blocks.length + 1}
                    autocompleteVariants={FILTER_PATH_AUTOCOMPLETE}
                    validateAutocomplete={false}
                    values={[]}
                    onChange={onNewBlockChange}/>
            </div>
            <input
                type="checkbox"
                id="filter-transparent-checkbox"
                checked={config.isTransparent}
                onChange={e => updateConfig({ ...config, isTransparent: e.target.checked })}
            />
            <label
                htmlFor="filter-transparent-checkbox">
                Transparent
            </label>

        </div>
    )
}

const FilterPanel = connect(
    ({filter: state}: AppState): StateProps => ({
        config: state.config
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateAction>): DispatchProps => ({
        updateConfig: config => dispatch(performFilter(config))
    })
)(FilterPanelBase);

export default FilterPanel;
