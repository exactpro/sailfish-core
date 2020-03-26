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
import { clearSearch, nextSearchResult, prevSearchResult, setSearchTokens } from '../../actions/actionCreators';
import { ThunkDispatch } from 'redux-thunk';
import StateAction from '../../actions/stateActions';
import '../../styles/search.scss';
import SearchResult from '../../helpers/search/SearchResult';
import KeyCodes from "../../util/KeyCodes";
import SearchToken from "../../models/search/SearchToken";
import Bubble from "../util/Bubble";
import { nextCyclicItem, removeByIndex, replaceByIndex } from "../../helpers/array";
import AutosizeInput from "react-input-autosize";
import { createBemBlock } from "../../helpers/styleCreators";
import SearchPanelControl from "./SearchPanelsControl";

export const REACTIVE_SEARCH_DELAY = 500;
const INPUT_PLACEHOLDER = 'Separate words with a space to find multiple words';

const COLORS = [
    '#E69900',
    '#FF5500',
    '#1F66AD',
    '#45A155',
    '#987DB3'
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

export interface Props extends StateProps, DispatchProps {}

interface State {
    inputValue: string;
    isActive: boolean;
}

export class SearchInputBase extends React.PureComponent<Props, State> {

    private inputElement: React.MutableRefObject<HTMLInputElement> = React.createRef();
    private root = React.createRef<HTMLDivElement>();

    constructor(props: Props) {
        super(props);

        this.state = {
            inputValue: '',
            isActive: false
        }
    }

    componentDidMount() {
        document.addEventListener("keydown", this.documentOnKeyDown);
        document.addEventListener("click", this.documentOnClick);
    }

    componentWillUnmount() {
        document.removeEventListener("keydown", this.documentOnKeyDown);
        document.removeEventListener("click", this.documentOnClick);
    }

    focus() {
        this.setState({
            isActive: true
        }, () => {
            this.inputElement.current.focus();
        })
    }

    blur() {
        this.setState({
            isActive: false,
            inputValue: ''
        })
    }

    render() {
        const { currentIndex, resultsCount, prevSearchResult, nextSearchResult, searchTokens } = this.props,
            { inputValue, isActive } = this.state;

        const notActiveTokens = searchTokens.filter(({ isActive }) => !isActive),
            activeTokens = searchTokens.find(({ isActive }) => isActive);

        const showControls = searchTokens.length > 0;

        const wrapperClassName = createBemBlock(
            "search-field-wrapper",
            isActive ? "active" : null
            ),
            rootClassName = createBemBlock(
                "search-field",
                isActive ? "active" : null
            );

        return (
            <div className={wrapperClassName}>
                <div className={rootClassName}
                     ref={this.root}
                     onClick={this.rootOnClick}>
                    {
                        isActive ? (
                            <React.Fragment>
                                {
                                    showControls ? (
                                        <div className="search-controls">
                                            <div className="search-controls__prev"
                                                 onClick={prevSearchResult}/>
                                            <div className="search-controls__next"
                                                 onClick={nextSearchResult}/>
                                            <div className="search-controls__clear"
                                                 onClick={this.clear}/>
                                        </div>
                                    ) : null
                                }
                                {
                                    notActiveTokens.map(({ color, pattern }, index) => (
                                        <Bubble
                                            key={index}
                                            className="search-bubble"
                                            size="small"
                                            removeIconType="white"
                                            submitKeyCodes={[KeyCodes.ENTER, KeyCodes.SPACE]}
                                            value={pattern}
                                            style={{ backgroundColor: color, color: '#FFF' }}
                                            onSubmit={this.bubbleOnChangeFor(index)}
                                            onRemove={this.bubbleOnRemoveFor(index)}/>
                                    ))
                                }
                                <AutosizeInput
                                    inputClassName="search-field__input"
                                    className="search-field__input-wrapper"
                                    inputRef={ref => this.inputElement.current = ref}
                                    inputStyle={
                                        inputValue.length > 0 ? {
                                            backgroundColor: activeTokens?.color ?? this.getNextColor(),
                                            color: '#FFF'
                                        } : undefined
                                    }
                                    placeholder={notActiveTokens.length < 1 && inputValue.length < 1 ? INPUT_PLACEHOLDER : undefined}
                                    type="text"
                                    spellCheck={false}
                                    value={inputValue}
                                    onChange={this.inputOnChange}
                                    onKeyDown={this.onKeyDown}/>
                                {
                                    showControls ? (
                                        <span className="search-field__counter">
                                            {currentIndex != null ? currentIndex + 1 : 0} of {resultsCount ?? 0}
                                        </span>
                                    ) : null
                                }
                                <SearchPanelControl/>
                            </React.Fragment>
                        ) : (
                            <div className="search-field__icon"/>
                        )
                    }
                </div>
            </div>
        )
    }

    private documentOnClick = (e: MouseEvent) => {
        if (!this.root.current.contains(e.target as HTMLElement) &&
            this.state.isActive &&
            this.props.searchTokens.length < 1 &&
            this.state.inputValue.length < 1
        ) {
            this.blur();
        }
    };

    private rootOnClick = (e: React.MouseEvent) => {
        if (e.target == this.root.current) {
            this.focus();
        }
    };

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
            this.clear();
            return;
        }

        if (e.keyCode == KeyCodes.BACKSPACE && this.state.inputValue == '' && this.props.searchTokens.length > 0) {
            // we should check is the last item active or not and remove it
            const nextTokens = this.props.searchTokens[this.props.searchTokens.length - 1].isActive ?
                this.props.searchTokens.slice(0, -1) :
                [...this.props.searchTokens];

            if (nextTokens.length > 0) {
                const [lastItem, ...restItems] = nextTokens.reverse();

                this.props.updateSearchTokens([...restItems.reverse(), {
                    ...lastItem,
                    isActive: true
                }]);
                this.setState({
                    inputValue: lastItem.pattern
                });
            } else {
                this.props.updateSearchTokens([]);
            }

            e.preventDefault();
        }

        if (e.keyCode == KeyCodes.SPACE && e.currentTarget.value != "") {
            if (e.ctrlKey) {
                this.setState({
                    inputValue: this.state.inputValue + ' '
                });
                return;
            }

            const [lastItem, ...restItems] = [...this.props.searchTokens].reverse();

            if (lastItem?.isActive) {
                this.props.updateSearchTokens([...restItems.reverse(), {
                    ...lastItem,
                    isActive: false,
                    pattern: e.currentTarget.value
                }]);
            } else {
                this.props.updateSearchTokens([
                    ...this.props.searchTokens,
                    this.createToken(e.currentTarget.value, undefined, false)
                ]);
            }

            this.setState({
                inputValue: ''
            })
        }
    };

    private documentOnKeyDown = (e: KeyboardEvent) => {
        if (e.keyCode === KeyCodes.F3 || (e.keyCode === KeyCodes.F && e.ctrlKey)) {
            // cancel browser search opening
            e.preventDefault();

            this.focus();
        }
    };

    private inputOnChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const currentValue = e.target.value;

        this.setState({
            inputValue: currentValue.trim()
        });

        setTimeout(() => {
            if (this.state.inputValue === currentValue) {
                // clear last active value
                if (currentValue == '') {
                    this.props.updateSearchTokens(
                        this.props.searchTokens.filter(({ isActive }) => !isActive)
                    );

                    return;
                }

                if (this.props.searchTokens.length == 0) {
                    this.props.updateSearchTokens([this.createToken(this.state.inputValue)]);
                    return;
                }

                const activeItem = this.props.searchTokens.find(({ isActive }) => isActive);

                if (activeItem != null) {
                    this.props.updateSearchTokens(replaceByIndex(
                        this.props.searchTokens,
                        this.props.searchTokens.indexOf(activeItem),
                        this.createToken(this.state.inputValue, activeItem.color)
                    ));
                    return;
                }

                this.props.updateSearchTokens([
                    ...this.props.searchTokens,
                    this.createToken(this.state.inputValue)
                ]);
            }
        }, REACTIVE_SEARCH_DELAY)
    };

    private bubbleOnChangeFor = (index: number) => (nextValue: string) => {
        this.props.updateSearchTokens(replaceByIndex(
            this.props.searchTokens,
            index,
            this.createToken(nextValue, this.props.searchTokens[index].color, false)
        ));
    };

    private bubbleOnRemoveFor = (index: number) => () => {
        this.props.updateSearchTokens(removeByIndex(this.props.searchTokens, index));
    };

    private clear = () => {
        this.props.clear();
        this.blur();
    };

    private createToken(value: string, color?: string, isActive: boolean = true): SearchToken {
        return {
            pattern: value.trim(),
            color: color ?? this.getNextColor(),
            isScrollable: true,
            isActive
        }
    }

    private getNextColor(): string {
        return this.props.searchTokens.length > 0 ?
            nextCyclicItem(COLORS, this.props.searchTokens[this.props.searchTokens.length - 1].color) :
            COLORS[0]
    }
}

const SearchInput = connect(
    (state: AppState): StateProps => ({
        searchTokens: state.selected.search.tokens,
        resultsCount: state.selected.search.resultsCount,
        currentIndex: state.selected.search.index,
        searchResults: state.selected.search.results
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateAction>): DispatchProps => ({
        updateSearchTokens: searchTokens => dispatch(setSearchTokens(searchTokens)),
        nextSearchResult: () => dispatch(nextSearchResult()),
        prevSearchResult: () => dispatch(prevSearchResult()),
        clear: () => dispatch(clearSearch())
    })
)(SearchInputBase);

export default SearchInput;
