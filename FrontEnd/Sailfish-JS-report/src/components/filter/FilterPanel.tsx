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
import {FilterConfig, FilterPath, FilterType} from '../../helpers/filter/FilterConfig';
import {performFilter} from "../../thunks/filter";
import FilterRow from "./FilterRow";
import {removeByIndex, replaceByIndex} from "../../helpers/array";

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

    const onNewBlockChange = (values: string[]) => {
        updateConfig({
            ...config,
            blocks: [...config.blocks, {
                path: FilterPath.ALL,
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
                    path={'type'}
                    values={config.types}
                    onChange={onTypesChange}/>
            </div>
            {
                config.blocks.map((block, index) => (
                    <div className="filter__row-wrapper" key={index}>
                        <div className="filter__row-divider">and</div>
                        <FilterRow
                            path={block.path}
                            values={block.values}
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
                    values={[]}
                    onChange={onNewBlockChange}/>
            </div>
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
