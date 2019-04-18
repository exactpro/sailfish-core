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
import AppState from '../state/models/AppState';
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

        const rootClass = createSelector(
                "header",
                status.status
            ), 
            mainClass = createSelector(
                "header-main", 
                status.status
            ),
            infoClass = createSelector(
                "header__info", 
                showFilter ? "filter-enabled" : null
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
            <div class={rootClass}>
                <div class="header__main   header-main">
                    <div class="header-button   header-main__contol-button"
                        onClick={backToListHandler}>
                        <div class="header-button__icon go-back" />
                        <div class="header-button__title">Back to list</div>
                    </div>
                    <div class="header-main__name ">
                        <div class="header-button"
                            onClick={prevTestCaseHandler}>
                            <div class="header-button__icon left"/>
                        </div>
                        <div class="header-main__title">
                            {(name || 'Test Case')} — {status.status} — {period}
                        </div>
                        <div class="header-button"
                            onClick={nextTestCaseHandler}>
                            <div class="header-button__icon right"/>
                        </div>
                    </div>
                    <div class="header-button   header-main__contol-button" onClick={() => this.switchFilter()}>
                        <div class="header-button__icon filter" />
                        <div class="header-button__title">{showFilter ? "Hide filter" : "Show filter"}</div>
                    </div>
                </div>
                <div class={infoClass}>
                    <div class="header__info-element">
                        <span>Start:</span>
                        <p>{formatTime(startTime)}</p>
                    </div>
                    <div class="header__info-element">
                        <span>Finish:</span>
                        <p>{formatTime(finishTime)}</p>
                    </div>
                    <div class="header__description">
                        {description}
                    </div>
                    <div class="header__info-element">
                        <span>ID:</span>
                        <p>{id}</p>
                    </div>
                    <div class="header__info-element">
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
        testCase: state.selected.testCase,
        splitMode: state.view.splitMode,
        actionsFilter: state.filter.actionsFilter,
        fieldsFilter: state.filter.fieldsFilter
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
