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
import { connect } from "react-redux";
import AppState from "../state/models/AppState";
import { nextCyclicItemByIndex, prevCyclicItemByIndex } from "../helpers/array";
import { setSelectedTestCase } from "../actions/actionCreators";
import { StatusType } from '../models/Status';

interface StateProps {
    failedTestCaseIds: string[];
    selectedTestCaseId: string;
}

interface DispatchProps {
    setSelectedTestCase: (testCaseId: string) => any;
}

interface SelectionCarouselControlProps extends StateProps, DispatchProps { }

class SelectionCarouselControlBase extends React.Component<SelectionCarouselControlProps, {}> {

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
        const { selectedTestCaseId, failedTestCaseIds } = this.props,
            failedTestCaseCount = failedTestCaseIds.length,
            hasFailedTestCases = failedTestCaseCount > 0;

        return (
            <div className="carousel-control">
                <div className={"carousel-control__title" + (hasFailedTestCases ? " enabled" : " disabled")}>
                    {hasFailedTestCases ? "Failed" : "No failed"}
                </div>
                {
                    hasFailedTestCases ? (
                        <React.Fragment>
                            <div className={"carousel-control__icon prev enabled"}
                                title="Go to previous"
                                onClick={() => this.selectPrevTestCase(selectedTestCaseId)} />
                            <div className={"layout-control__counter enabled"}>
                                <p>{this.props.failedTestCaseIds.indexOf(selectedTestCaseId) + 1} of {failedTestCaseCount}</p>
                            </div>
                            <div className={"carousel-control__icon next enabled"}
                                title="Go to next"
                                onClick={() => this.selectNextTestCase(selectedTestCaseId)} />
                        </React.Fragment>
                    ) : null
                }
            </div>
        )
    }
}

export const SelectionCarouselControl = connect(
    (state: AppState): StateProps => ({
        selectedTestCaseId: state.selected.selectedTestCaseId,
        failedTestCaseIds: (state.report.metadata || []).filter(item => item.status.status === StatusType.FAILED).map(item => item.id)
    }),
    (dispatch): DispatchProps => ({
        setSelectedTestCase: (testCaseId: string) => dispatch(setSelectedTestCase(testCaseId)),
    })
)(SelectionCarouselControlBase);
