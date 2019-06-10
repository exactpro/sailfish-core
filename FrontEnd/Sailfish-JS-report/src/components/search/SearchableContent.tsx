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
import '../../styles/search.scss';

interface OwnProps {
    content: string;
    contentKey: string;
}

interface StateProps {
    targetIndex: number;
    startIndex: number;
    searchString: string;
}

interface Props extends Omit<OwnProps, 'contentKey'>, StateProps {}

const SearchableContentBase = ({ content, startIndex, targetIndex, searchString }: Props) => {

    if (!searchString) {
        return (
            <span>{content}</span>
        )
    }

    const splittedContent = content.split(searchString),
        internalTargetIndex = targetIndex - startIndex;

    return (
        <span>
            {
                splittedContent.map((content, index) => [
                    <span key={index}>{content}</span>,
                    index !== splittedContent.length - 1 ? 
                        <span 
                            className={'found' + (index === internalTargetIndex ? ' target' : '')} 
                            key={splittedContent.length + index}>
                            {searchString}
                        </span> : 
                        null
                ])
            }
        </span>
    )
}

const SearchableContent = connect(
    ({ selected: state }: AppState, ownProps: OwnProps): StateProps => ({
        searchString: state.searchString,
        targetIndex: state.searchIndex,
        startIndex: state.searchResults.getStartIndexForKey(ownProps.contentKey)
    })
)(SearchableContentBase);

export default SearchableContent;
