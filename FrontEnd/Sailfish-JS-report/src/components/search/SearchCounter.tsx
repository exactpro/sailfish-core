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
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { nextSearchResult, prevSearchResult, clearSearch } from '../../actions/actionCreators';
import '../../styles/search.scss';

interface StateProps {
    resultsCount: number;
    currentIndex: number;
}

interface DispatchProps {
    next: () => any;
    prev: () => any;
    clear: () => any;
}

interface Props extends StateProps, DispatchProps {}

function SearchCounterBase({ resultsCount, currentIndex, next, prev, clear }: Props) {
    return (
        <div className="search-counter">
            <div className="search-counter__index">
                {currentIndex != null ? currentIndex + 1 : 0} / {resultsCount}
            </div>
            <div className="search-counter__prev"
                onClick={() => prev()}/>
            <div className="search-counter__next"
                onClick={() => next()}/>
            <div className="search-counter__clear"
                onClick={() => clear()}/>
        </div> 
    )
}

const SearchCounter = connect(
    (state: AppState): StateProps => ({
        resultsCount: state.selected.searchResultsCount,
        currentIndex: state.selected.searchIndex
    }), 
    (dispatch): DispatchProps => ({
        next: () => dispatch(nextSearchResult()),
        prev: () => dispatch(prevSearchResult()),
        clear: () => dispatch(clearSearch())
    })
)(SearchCounterBase);

export default SearchCounter;
