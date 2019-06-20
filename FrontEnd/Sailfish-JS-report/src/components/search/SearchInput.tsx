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
import { ThunkDispatch } from 'redux-thunk';
import StateAction from '../../actions/stateActions';
import { reactiveSearch } from '../../thunks/search';

const F_KEY_CODE = 70,
    F3_KEY_CODE = 114;

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

    const input = React.useRef<HTMLInputElement>();

    // same as componentDidMount
    React.useEffect(() => {
        // we need to use 'top' in case of working in iframe
        top.document.addEventListener("keydown", e => {
            if (e.keyCode === F3_KEY_CODE || (e.keyCode === F_KEY_CODE && e.ctrlKey)) {
                // cancel browser search opening
                e.preventDefault();

                input.current.focus();
            }
        });
    }, []);
    
    return (
        <div className="search-input">
            <input
                ref={input}
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
    (state: AppState): StateProps => ({
        searchString: state.selected.searchString,
        resultsCount: state.selected.searchResultsCount,
        currentIndex: state.selected.searchIndex
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateAction>): DispatchProps => ({
        updateSearchString: searchString => dispatch(reactiveSearch(searchString)),
        nextSearchResult: () => dispatch(nextSearchResult()),
        prevSearchResult: () => dispatch(prevSearchResult())
    })
)(SearchInputBase);

export default SearchInput;
