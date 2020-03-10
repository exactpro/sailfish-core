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
import { raf } from '../../helpers/raf';
import { setShouldScrollToSearchItem } from '../../actions/actionCreators';
import '../../styles/search.scss';
import SearchSplitResult from "../../models/search/SearchSplitResult";
import { createStyleSelector } from "../../helpers/styleCreators";
import multiTokenSplit from "../../helpers/search/multiTokenSplit";
import { getTokens } from "../../selectors/search";

interface OwnProps {
    content: string;
    contentKey: string;
    /**
     * If true, component will split passed content and pass it instead of received from redux store.
     */
    shouldPerformSplit?: boolean;
}

interface StateProps {
    targetIndex: number;
    startIndex: number;
    searchResult: SearchSplitResult[] | undefined;
    needsScroll: boolean;
}

interface DispatchProps {
    onScrolled: () => void;
}

interface Props extends Omit<OwnProps, 'contentKey'>, StateProps, DispatchProps {}

function SearchableContentBase({ content, startIndex, targetIndex, searchResult, needsScroll, onScrolled }: Props) {
    if (!searchResult) {
        return <React.Fragment>{content}</React.Fragment>;
    }

    // we are using 'useRef' instead of 'createRef' in functional components
    // because 'createRef' creates new Ref object for each call, when 'useRef' does not
    const targetElement = React.useRef<HTMLSpanElement>();

    // fires only if target index has been changed
    React.useEffect(() => {
        // we need to scroll to target element after VirtualizedList scrolled to current row
        raf(() => {
            // 'needsScroll' flag is used here not to scroll to target in case of second render after virtualized row unmount.
            // TODO - it's really bad solution to store some flags in redux - we need to think how to handle this situation without it.
            if (targetElement.current && needsScroll) {
                targetElement.current.scrollIntoView({ block: 'center', inline: 'nearest' });
                onScrolled();
            }
        });
    }, [targetIndex]);

    const internalTargetIndex = targetIndex != null ? targetIndex - startIndex : -1;
    let foundPartIndex = -1;

    const renderContentPart = ({ token, content }: SearchSplitResult, index: number) => {
        const isFound = token != null;
        const isTarget = isFound && token.isScrollable && ++foundPartIndex == internalTargetIndex;
        const className = createStyleSelector(
            '',
            isFound ? 'found' : null,
            isTarget ? 'target' : null
        );

        return (
            <span
                key={index}
                className={className}
                style={{ backgroundColor: token?.color }}
                ref={isTarget ? targetElement : undefined}>
                {content}
            </span>
        )
    };

    return (
        <span>{searchResult.map(renderContentPart)}</span>
    )
}

const SearchableContent = connect(
    (state: AppState, ownProps: OwnProps): StateProps => ({
        targetIndex: state.selected.search.index,
        startIndex: state.selected.search.results.getStartIndexForKey(ownProps.contentKey),
        searchResult: state.selected.search.results.has(ownProps.contentKey) ? (
            // in some cases (e. g. message's beautified content) we need to split content again, instead of using redux value
            // because content passed in own props differ from the original.
            ownProps.shouldPerformSplit ?
                multiTokenSplit(ownProps.content, getTokens(state)) :
                state.selected.search.results.get(ownProps.contentKey)
        ) : undefined,
        needsScroll: state.selected.search.shouldScrollToItem
    }),
    (dispatch): DispatchProps => ({
        onScrolled: () => dispatch(setShouldScrollToSearchItem(false))
    })
)(SearchableContentBase);

export default SearchableContent;
