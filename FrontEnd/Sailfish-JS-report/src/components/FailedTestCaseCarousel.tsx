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

import { connect } from 'react-redux';
import AppState from '../state/models/AppState';
import { StatusType } from '../models/Status';
import { setSelectedTestCase } from '../actions/actionCreators';
import { nextCyclicItemByIndex, prevCyclicItemByIndex } from '../helpers/array';
import SelectionCarousel, { SelectionCarouselProps } from './SelectionCarousel';
import { isTestCaseMetadata } from '../models/TestcaseMetadata';

export const FailedTestCaseCarousel = connect(
    (state: AppState) => {
        const failedTestCases = (state.report.metadata || [])
            .filter(isTestCaseMetadata)
            .filter(item => item.status.status === StatusType.FAILED)
            .map(tc => tc.id);

        return {
            failedTestCases,
            index: failedTestCases.indexOf(state.selected.selectedTestCaseId),
            itemsCount: failedTestCases.length,
            isEnabled: failedTestCases.length > 0
        }
    },
    (dispatch) => ({
        selectTestCase: (testCaseId: string) => dispatch(setSelectedTestCase(testCaseId)),
    }),
    ({ failedTestCases, index, ...stateProps }, { selectTestCase }, ownProps): SelectionCarouselProps => ({
        ...stateProps,
        ...ownProps,
        currentIndex: index + 1,
        next: () => selectTestCase(nextCyclicItemByIndex(failedTestCases, index)),
        prev: () => selectTestCase(prevCyclicItemByIndex(failedTestCases, index))
    })
)(SelectionCarousel);
