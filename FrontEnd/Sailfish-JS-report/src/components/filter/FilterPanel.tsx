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
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { ThunkDispatch } from 'redux-thunk';
import StateAction from '../../actions/stateActions';
import { performFilter } from "../../thunks/filter";
import FilterRow from "./FilterRow";
import { removeByIndex, replaceByIndex } from "../../helpers/array";
import FilterType from "../../models/filter/FilterType";
import { resetFilter, setFilterIsHighlighted, setFilterIsTransparent } from "../../actions/actionCreators";
import {
    getActionsFilterResultsCount, getIsFilterApplied,
    getMessagesFilterResultsCount
} from "../../selectors/filter";
import { FilterBlock } from "../../models/filter/FilterBlock";
import Checkbox from "../util/Checkbox";

interface StateProps {
    blocks: FilterBlock[];
    isTransparent: boolean;
    isFilterApplied: boolean;
    isHighlighted: boolean;
    actionsResultsCount: number;
    messagesResultsCount: number;
}

interface DispatchProps {
    updateFilterBlocks: (blocks: FilterBlock[]) => void;
    setIsTransparent: (isTransparent: boolean) => void;
    setIsHighlighted: (isHighlighted: boolean) => void;
    resetFilter: () => void;
}

interface Props extends StateProps, DispatchProps {
}

function FilterPanelBase(props: Props) {
    const {
        blocks,
        isFilterApplied,
        isTransparent,
        isHighlighted,
        updateFilterBlocks,
        setIsTransparent,
        setIsHighlighted,
        actionsResultsCount,
        messagesResultsCount,
        resetFilter
    } = props;

    const onBlockChangeFor = (blockIndex: number) => (nextBlock: FilterBlock) => {
        updateFilterBlocks(replaceByIndex(blocks, blockIndex, nextBlock));
    };

    const onNewBlockChange = (newBlock: FilterBlock) => {
        updateFilterBlocks([...blocks, newBlock]);
    };

    const onBlockRemoveFor = (index: number) => () => {
        updateFilterBlocks(removeByIndex(blocks, index));
    };

    return (
        <div className="filter">
            {
                blocks.map((block, index) => (
                    <div className="filter-row" key={index}>
                        <div className="filter-row__divider">
                            <div className="filter-row__divider-text">
                                {index == 0 ? 'Filter' : 'and'}
                            </div>
                            <div
                                className="filter-row__remove-btn"
                                onClick={onBlockRemoveFor(index)}
                            />
                        </div>
                        <FilterRow
                            block={block}
                            rowIndex={index + 1}
                            onChange={onBlockChangeFor(index)}
                            onRemove={onBlockRemoveFor(index)}
                        />
                    </div>
                ))
            }
            <div className="filter-row">
                <div className="filter-row__divider">
                    {blocks.length == 0 ? 'Filter' : 'and'}
                </div>
                <FilterRow
                    block={{
                        types: null,
                        path: null,
                        values: []
                    }}
                    rowIndex={blocks.length + 1}
                    onChange={onNewBlockChange}/>
            </div>
            <div className="filter__controls filter-controls">
                <div className="filter-controls__counts">
                    {
                        isFilterApplied ? [
                            blocks.some(({ types }) => types.includes(FilterType.ACTION)) ?
                                `${actionsResultsCount} Actions ` :
                                null,
                            blocks.some(({ types }) => types.includes(FilterType.MESSAGE)) ?
                                `${messagesResultsCount} Messages ` :
                                null,
                        ].filter(Boolean).join(' and ') + 'Filtered' : null
                    }
                </div>
               <Checkbox
                   checked={isHighlighted}
                   label='Highlight'
                   onChange={() => setIsHighlighted(!isHighlighted)}
                   id='filter-highlight'/>
                <div className="filter-controls__transparency">
                    Filtered out
                    <input
                        type="radio"
                        id="filter-radio-hide"
                        checked={!isTransparent}
                        onChange={e => setIsTransparent(false)}
                    />
                    <label htmlFor="filter-radio-hide">Hide</label>
                    <input
                        type="radio"
                        id="filter-radio-transparent"
                        checked={isTransparent}
                        onChange={e => setIsTransparent(true)}
                    />
                    <label htmlFor="filter-radio-transparent">Transparent</label>
                </div>
                <div className="filter-controls__clear-btn" onClick={() => resetFilter()}>
                    <div className="filter-controls__clear-icon"/>
                    Clear All
                </div>
            </div>
        </div>
    )
}

const FilterPanel = connect(
    (state: AppState): StateProps => ({
        blocks: state.filter.blocks,
        isTransparent: state.filter.isTransparent,
        isHighlighted: state.filter.isHighlighted,
        isFilterApplied: getIsFilterApplied(state),
        actionsResultsCount: getActionsFilterResultsCount(state),
        messagesResultsCount: getMessagesFilterResultsCount(state)
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateAction>): DispatchProps => ({
        updateFilterBlocks: config => dispatch(performFilter(config)),
        setIsTransparent: isTransparent => dispatch(setFilterIsTransparent(isTransparent)),
        setIsHighlighted: isHighlighted => dispatch(setFilterIsHighlighted(isHighlighted)),
        resetFilter: () => dispatch(resetFilter())
    })
)(FilterPanelBase);

export default FilterPanel;
