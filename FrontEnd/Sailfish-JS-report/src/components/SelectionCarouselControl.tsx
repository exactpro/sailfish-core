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

import {Component, h} from 'preact';
import {connect} from "preact-redux";
import AppState from "../state/models/AppState";
import {nextCyclicItemByIndex, prevCyclicItemByIndex} from "../helpers/array";
import {setSelectedTestCase} from "../actions/actionCreators";

interface SelectionCarouselControlProps {
    failedTestCaseIds: string[];
    selectedTestCaseId: string;

    setSelectedTestCase: (testCaseId: string) => any;
}

class SelectionCarouselControlBase extends Component<SelectionCarouselControlProps, {}> {

    private selectNextTestCase(currentId: string) {
        this.selectTestCase(nextCyclicItemByIndex(this.props.failedTestCaseIds, this.props.failedTestCaseIds.indexOf(currentId)));
    }

    private selectPrevTestCase(currentId: string) {
       this.selectTestCase(prevCyclicItemByIndex(this.props.failedTestCaseIds, this.props.failedTestCaseIds.indexOf(currentId)));

    }

    private selectTestCase(newId: string) {
        this.props.setSelectedTestCase(newId);
    }

    render() {
        const selectedTestCaseId = this.props.selectedTestCaseId;
        const failedTestCaseCount = this.props.failedTestCaseIds.length;
        const hasFailedTestCases = failedTestCaseCount > 0;

        return (
            <div class="carousel-control">
                <div class={"carousel-control__title" + (hasFailedTestCases ? " enabled" : " disabled")}>
                    {hasFailedTestCases ? "Failed" : "No failed"}
                </div>
                {
                    hasFailedTestCases ? ([
                        <div class={"carousel-control__icon prev enabled"}
                             title="Go to previous"
                             onClick={() => this.selectPrevTestCase(selectedTestCaseId)}/>,
                        <div class={"layout-control__counter enabled"}>
                            <p>{this.props.failedTestCaseIds.indexOf(selectedTestCaseId) + 1} of {failedTestCaseCount}</p>
                        </div>,
                        <div class={"carousel-control__icon next enabled"}
                             title="Go to next"
                             onClick={() => this.selectNextTestCase(selectedTestCaseId)}/>
                    ]) : null
                }
            </div>
        )
    }
}

export const SelectionCarouselControl = connect(
    (state: AppState) => ({
        selectedTestCaseId: state.selected.selectedTestCaseId
    }),
    (dispatch) => ({
        setSelectedTestCase: (testCaseId: string) => dispatch(setSelectedTestCase(testCaseId)),
    })
)(SelectionCarouselControlBase);
