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
import { setSearchString, nextSearchResult, prevSearchResult } from '../../actions/actionCreators';

interface StateProps {
    searchString: string;
    currentIndex: number;
    resultsCount: number;
}

interface DispatchProps {
    updateSearchString: (searchString: string) => any;
    nextSearchResult: () => any;
    prevSearchResult: () => any;
}

interface Props extends StateProps, DispatchProps { }

const SearchInputBase = ({ searchString, updateSearchString, nextSearchResult, prevSearchResult, currentIndex, resultsCount }: Props) => {
    
    return (
        <div className="search-input">
            <input
                type="text"
                value={searchString}
                onChange={e => updateSearchString(e.target.value)}
                onKeyDown={e => {
                    if (e.keyCode == 13 && e.shiftKey) {
                        prevSearchResult();
                        return;
                    }

                    if (e.keyCode == 13) {
                        nextSearchResult();
                    }
                }}/>
            <div>{currentIndex != null ? currentIndex + 1 : 0} / {resultsCount}</div>
        </div>
    )
}

const SearchInput = connect(
    (state: AppState) => ({
        searchString: state.selected.searchString,
        testCase: state.selected.testCase,
        resultsCount: state.selected.searchResultsCount,
        currentIndex: state.selected.searchIndex
    }),
    (dispatch) => ({
        updateSearchString: (searchString, testCase) => dispatch(setSearchString(searchString, testCase)),
        nextSearchResult: () => dispatch(nextSearchResult()),
        prevSearchResult: () => dispatch(prevSearchResult())
    }),
    (stateProps, dispatchProps, ownProps): StateProps & DispatchProps => ({
        ...dispatchProps,
        resultsCount: stateProps.resultsCount,
        currentIndex: stateProps.currentIndex,
        searchString: stateProps.searchString,
        updateSearchString: searchString => dispatchProps.updateSearchString(searchString, stateProps.testCase)
    })
)(SearchInputBase);

export default SearchInput;
