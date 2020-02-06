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
import { performSearch } from '../../thunks/search';
import SearchCounter from './SearchCounter';
import '../../styles/search.scss';
import SearchResult from '../../helpers/search/SearchResult';
import KeyCodes from "../../util/KeyCodes";
import SearchToken from "../../models/search/SearchToken";

const REACTIVE_SEARCH_DELAY = 500;

const COLORS = [
    '#E69900',
    '#FF5500',
    '#1F66AD',
    '#45A155',
    '#00BBCC'
];

interface StateProps {
    searchTokens: SearchToken[];
    currentIndex: number;
    resultsCount: number;
    searchResults: SearchResult;
}

interface DispatchProps {
    updateSearchTokens: (searchValues: SearchToken[]) => void;
    nextSearchResult: () => void;
    prevSearchResult: () => void;
    clear: () => void;
}

interface Props extends StateProps, DispatchProps { }

interface State {
    inputValue: string;
    isLoading: boolean;
}

class SearchInputBase extends React.PureComponent<Props, State> {

    private inputElement = React.createRef<HTMLInputElement>();
    
    constructor(props: Props) {
        super(props);

        this.state = {
            inputValue: '',
            isLoading: false
        }
    }

    componentDidMount() {
        document.addEventListener("keydown", this.documentOnKeyDown);
    }

    componentWillUnmount() {
        document.removeEventListener("keydown", this.documentOnKeyDown);
    }

    componentDidUpdate(prevProps: Props) {
        if (this.props.searchResults !== prevProps.searchResults) {
            // search string and search results updated, so we need to stop loader
            // this.setState({
            //     isLoading: false,
            //     inputValue: this.props.searchTokens
            // });
        }
    }
    
    render() {
        const { searchTokens } = this.props,
            { inputValue, isLoading } = this.state;
        
        return (
            <div className="search-field">
                {
                    searchTokens.map(({color, pattern}, index) => (
                        <span style={{backgroundColor: color}} key={index}>
                            {pattern}
                        </span>
                    ))
                }
                <input
                    className="search-field__input"
                    ref={this.inputElement}
                    type="text"
                    value={inputValue}
                    onChange={this.inputOnChange}
                    onKeyDown={this.onKeyDown}/>
                {
                    inputValue ? (
                        isLoading ? (
                            <div className="loader"/>
                        ) : (
                            <SearchCounter/>
                        )
                    ) : (
                        <div className="search-field__icon"/>
                    ) 
                }
            </div>
        )
    }

    private onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.keyCode == KeyCodes.ENTER && e.shiftKey) {
            this.props.prevSearchResult();
            return;
        }

        if (e.keyCode == KeyCodes.ENTER) {
            this.props.nextSearchResult();
            return;
        }

        if (e.keyCode == KeyCodes.ESCAPE) {
            this.inputElement.current.blur();
            this.props.clear();
            return;
        }

        if (e.keyCode == KeyCodes.SPACE && e.currentTarget.value) {
            this.props.updateSearchTokens([
                ...this.props.searchTokens, {
                    pattern: e.currentTarget.value.trim(),
                    color: COLORS[this.props.searchTokens.length % COLORS.length]
                }
            ]);

            this.setState({
                inputValue: ''
            })
        }
    };

    private documentOnKeyDown = (e: KeyboardEvent) => {
        if (e.keyCode === KeyCodes.F3 || (e.keyCode === KeyCodes.F && e.ctrlKey)) {
            // cancel browser search opening
            e.preventDefault();

            this.inputElement.current.focus();
        }
    };

    private inputOnChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const currentValue = e.target.value;

        this.setState({
            inputValue: currentValue,
            //isLoading: true
        });

        setTimeout(() => {
            // if (this.state.inputValue === currentValue) {
            //     this.props.updateSearchTokens(currentValue);
            // }
        }, REACTIVE_SEARCH_DELAY)
    }
}

const SearchInput = connect(
    (state: AppState): StateProps => ({
        searchTokens: state.selected.searchTokens,
        resultsCount: state.selected.searchResultsCount,
        currentIndex: state.selected.searchIndex,
        searchResults: state.selected.searchResults
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateAction>): DispatchProps => ({
        updateSearchTokens: searchTokens => dispatch(performSearch(searchTokens)),
        nextSearchResult: () => dispatch(nextSearchResult()),
        prevSearchResult: () => dispatch(prevSearchResult()),
        clear: () => dispatch(clearSearch())
    })
)(SearchInputBase);

export default SearchInput;
