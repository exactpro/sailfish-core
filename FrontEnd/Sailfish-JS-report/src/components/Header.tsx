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

import { h, Component } from 'preact';
import TestCase from '../models/TestCase';
import '../styles/header.scss';
import { StatusType } from '../models/Status';
import { FilterPanel } from './FilterPanel';
import { connect } from 'preact-redux';
import AppState from '../state/AppState';
import {
    nextTestCase,
    prevTestCase,
    resetTestCase,
    switchSplitMode,
    switchActionsFilter,
    switchFieldsFilter
} from '../actions/actionCreators';
import { getSecondsPeriod, formatTime } from '../helpers/dateFormatter';
import { createSelector } from '../helpers/styleCreators';

interface HeaderProps {
    testCase: TestCase;
    actionsFilter: StatusType[];
    fieldsFilter: StatusType[];
    nextTestCaseHandler: () => any;
    prevTestCaseHandler: () => any;
    backToListHandler: () => any;
    switchActionsFilter: (status: StatusType) => any;
    switchFieldsFilter: (status: StatusType) => any;
}

interface HeaderState {
    showFilter: boolean;
}

class HeaderBase extends Component<HeaderProps, HeaderState> {

    constructor(props) {
        super(props);

        this.state = {
            showFilter: false
        }
    }

    render({
        testCase,
        actionsFilter,
        fieldsFilter,
        nextTestCaseHandler,
        prevTestCaseHandler,
        backToListHandler,
        switchActionsFilter,
        switchFieldsFilter
    }: HeaderProps, { showFilter }: HeaderState) {

        const {
            name,
            status,
            startTime,
            finishTime,
            id,
            hash,
            description,
        } = testCase;

        const mainClass = createSelector(
                "header-main", 
                status.status
            ),
            infoClass = createSelector(
                "header-info", 
                status.status, 
                showFilter ? "filter" : null
            ),
            prevButtonClass = createSelector(
                "header-main-name-icon",
                "left", 
                prevTestCaseHandler ? "enabled" : "disabled"
            ),
            nextButtonClass = createSelector(
                "header-main-name-icon",
                "right", 
                nextTestCaseHandler ? "enabled" : "disabled"
            );

        const period = getSecondsPeriod(startTime, finishTime);

        return (
            <div class="header">
                <div class={mainClass}>
                    <div class="header-main-button"
                        onClick={backToListHandler}>
                        <div class="header-main-button-icon list" />
                        <h3>Back to list</h3>
                    </div>
                    <div class="header-main-name">
                        <div class={prevButtonClass}
                            onClick={prevTestCaseHandler} />
                        <h1>{(name || 'Test Case')} — {status.status} — {period}</h1>
                        <div class={nextButtonClass}
                            onClick={nextTestCaseHandler} />
                    </div>
                    <div class="header-main-button" onClick={() => this.switchFilter()}>
                        <div class="header-main-button-icon filter" />
                        <h3>{showFilter ? "Hide filter" : "Show filter"}</h3>
                    </div>
                </div>
                <div class={infoClass}>
                    <div class="header-info-start">
                        <span>Start:</span>
                        <p>{formatTime(startTime)}</p>
                    </div>
                    <div class="header-info-finish">
                        <span>Finish:</span>
                        <p>{formatTime(finishTime)}</p>
                    </div>
                    <div class="header-info-description">
                        <h2>{description}</h2>
                    </div>
                    <div class="header-info-id">
                        <span>ID:</span>
                        <p>{id}</p>
                    </div>
                    <div class="header-info-hash">
                        <span>Hash:</span>
                        <p>{hash}</p>
                    </div>
                </div>
                {
                    showFilter ?
                        <FilterPanel
                            actionsFilters={actionsFilter}
                            fieldsFilters={fieldsFilter}
                            actionFilterHandler={switchActionsFilter}
                            fieldsFilterHandler={switchFieldsFilter} />
                        : null
                }
            </div>
        );
    }

    private switchFilter() {
        this.setState({
            showFilter: !this.state.showFilter
        })
    }
}

export const Header = connect(
    (state: AppState) => ({
        testCase: state.testCase,
        splitMode: state.splitMode,
        actionsFilter: state.actionsFilter,
        fieldsFilter: state.fieldsFilter
    }),
    dispatch => ({
        nextTestCaseHandler: () => dispatch(nextTestCase()),
        prevTestCaseHandler: () => dispatch(prevTestCase()),
        backToListHandler: () => dispatch(resetTestCase()),
        switchSplitMode: () => dispatch(switchSplitMode()),
        switchFieldsFilter: (status: StatusType) => dispatch(switchFieldsFilter(status)),
        switchActionsFilter: (status: StatusType) => dispatch(switchActionsFilter(status))
    })
)(HeaderBase)
