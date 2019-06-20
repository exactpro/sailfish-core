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
import { ThunkDispatch } from 'redux-thunk';
import StateAction from '../../actions/stateActions';
import { reactiveSearch } from '../../thunks/search';

const F_KEY_CODE = 70,
    F3_KEY_CODE = 114,
    ENTER_KEY_CODE = 13,
    ECS_KEY_CODE = 27;

interface StateProps {
    searchString: string;
    currentIndex: number;
    resultsCount: number;
}

interface DispatchProps {
    updateSearchString: (searchString: string) => any;
    nextSearchResult: () => any;
    prevSearchResult: () => any;
    clear: () => any;
}

interface Props extends StateProps, DispatchProps { }

class SearchInputBase extends React.PureComponent<Props> {

    private inputElement = React.createRef<HTMLInputElement>();

    componentDidMount() {
        // we need to use 'top' in case of working in iframe
        top.document.addEventListener("keydown", this.documentOnKeyDown);
    }

    componentWillUnmount() {
        top.document.removeEventListener("keydown", this.documentOnKeyDown);
    }
    
    render() {
        const { searchString, updateSearchString, currentIndex, resultsCount } = this.props;
        
        return (
            <div className="search-field">
                <input
                    className="search-field__input"
                    ref={this.inputElement}
                    type="text"
                    value={searchString}
                    onChange={e => updateSearchString(e.target.value)}
                    onKeyDown={this.onKeyDown}/>
                {
                    searchString ? (
                        <div className="search-field__counter">
                            {currentIndex != null ? currentIndex + 1 : 0} / {resultsCount}
                        </div> 
                    ) : (
                        <div className="search-field__icon"/>
                    ) 
                }
            </div>
        )
    }

    private onKeyDown = (e: React.KeyboardEvent) => {
        if (e.keyCode == ENTER_KEY_CODE && e.shiftKey) {
            this.props.prevSearchResult();
            return;
        }

        if (e.keyCode == ENTER_KEY_CODE) {
            this.props.nextSearchResult();
            return;
        }

        if (e.keyCode == ECS_KEY_CODE) {
            this.inputElement.current.blur();
            this.props.clear();
        }
    }

    private documentOnKeyDown = (e: KeyboardEvent) => {
        if (e.keyCode === F3_KEY_CODE || (e.keyCode === F_KEY_CODE && e.ctrlKey)) {
            // cancel browser search opening
            e.preventDefault();

            this.inputElement.current.focus();
        }
    }
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
        prevSearchResult: () => dispatch(prevSearchResult()),
        clear: () => dispatch(clearSearch())
    })
)(SearchInputBase);

export default SearchInput;
