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
import { createCaseInsensitiveRegexp } from '../../helpers/regexp';
import { raf } from '../../helpers/raf';
import { setShouldScrollToSearchItem } from '../../actions/actionCreators';
import '../../styles/search.scss';

interface OwnProps {
    content: string;
    contentKey: string;
}

interface StateProps {
    targetIndex: number;
    startIndex: number;
    searchString: string;
    resultsCount: number;
    needsScroll: boolean;
}

interface DispatchProps {
    onScrolled: () => any;
}

interface Props extends Omit<OwnProps, 'contentKey'>, StateProps, DispatchProps {}

const SearchableContentBase = ({ content, startIndex, targetIndex, searchString, resultsCount, needsScroll, onScrolled }: Props) => {
    if (!searchString || !content || !resultsCount) {
        return (
            <React.Fragment>{content}</React.Fragment>
        );
    }

    const splittedContent = content.split(createCaseInsensitiveRegexp(searchString)),
        internalTargetIndex = targetIndex != null && targetIndex - startIndex;

    // we are using 'useRef' instead of 'createRef' in functional components
    // because 'createRef' creates new Ref object for each call, when 'useRef' does not
    const targetElement = React.useRef<HTMLSpanElement>();

    // fires only if target index has been changed
    React.useEffect(() => {
        // we neeed to scroll to target element after VirtualizedList scrolled to current row
        raf(() => {
            // 'needsScroll' flag is used here not to scroll to target in case of second render after virtualized row unmount.
            // TODO - it's realy bad solution to store some flags in redux - we need to think how to handle this situation without it.
            if (targetElement.current && needsScroll) {
                targetElement.current.scrollIntoView({ block: 'center', inline: 'nearest' });
                onScrolled();
            }
        });
    }, [targetIndex]);

    let contentCounter = 0;

    return (
        <span>
            {
                splittedContent.map((contentPart, index) => {
                    contentCounter += contentPart.length;

                    const foundContent = content.substring(contentCounter, contentCounter + searchString.length);

                    contentCounter += searchString.length;

                    return [
                        <span key={index}>
                            {contentPart}
                        </span>,
                        index !== splittedContent.length - 1 ? 
                            <span 
                                className={'found' + (index === internalTargetIndex ? ' target' : '')} 
                                key={splittedContent.length + index}
                                ref={index === internalTargetIndex ? targetElement : undefined}>
                                {foundContent}
                            </span> : 
                            null
                    ]
                })
            }
        </span>
    )
}

const SearchableContent = connect(
    ({ selected: state }: AppState, ownProps: OwnProps): StateProps => ({
        searchString: state.searchString,
        targetIndex: state.searchIndex,
        startIndex: state.searchResults.getStartIndexForKey(ownProps.contentKey),
        resultsCount: state.searchResults.get(ownProps.contentKey),
        needsScroll: state.shouldScrollToSearchItem
    }),
    (dispatch): DispatchProps => ({
        onScrolled: () => dispatch(setShouldScrollToSearchItem(false))
    })
)(SearchableContentBase);

export default SearchableContent;
