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
    switchFieldsFilter,
    showFilter
} from '../actions/actionCreators';
import { getSecondsPeriod, formatTime } from '../helpers/dateFormatter';

interface HeaderProps {
    testCase: TestCase;
    showFilter: boolean;
    actionsFilter: StatusType[];
    fieldsFilter: StatusType[];
    nextTestCaseHandler: () => any;
    prevTestCaseHandler: () => any;
    backToListHandler: () => any;
    switchActionsFilter: (status: StatusType) => any;
    switchFieldsFilter: (status: StatusType) => any;
    showFilterHandler: () => any;
}

const HeaderBase = ({ testCase, actionsFilter, fieldsFilter, nextTestCaseHandler, prevTestCaseHandler, backToListHandler,
    switchActionsFilter, switchFieldsFilter, showFilter, showFilterHandler }: HeaderProps) => {

    const {
        name,
        status,
        startTime,
        finishTime,
        id,
        hash,
        description,
    } = testCase;
    
    const statusClass = ["header-status", status.status.toLowerCase(), (showFilter ? "filter" : "")].join(' '),
        prevButtonClass = ["header-status-name-icon", "left", (prevTestCaseHandler ? "enabled" : "disabled")].join(' '),
        nextButtonClass = ["header-status-name-icon", "right", (nextTestCaseHandler ? "enabled" : "disabled")].join(' ');

    const period = getSecondsPeriod(startTime, finishTime);

    return (
        <div class="header">
            <div class={statusClass}>
                <div class="header-status-button"
                    onClick={backToListHandler}>
                    <div class="header-status-button-icon list" />
                    <h3>Back to list</h3>
                </div>
                <div class="header-status-name">
                    <div class={prevButtonClass}
                        onClick={prevTestCaseHandler} />
                    <h1>{(name || 'Test Case')} — {status.status} — {period}</h1>
                    <div class={nextButtonClass}
                        onClick={nextTestCaseHandler} />
                </div>
                <div class="header-status-button" onClick={() => showFilterHandler()}>
                    <div class="header-status-button-icon filter" />
                    <h3>{showFilter ? "Hide filter" : "Show filter"}</h3>
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
            <div class="header-description">
                <div class="header-description-element">
                    <span>Start:</span>
                    <p>{formatTime(startTime)}</p>
                    <span>Finish:</span>
                    <p>{formatTime(finishTime)}</p>
                    <span>ID:</span>
                    <p>{id}</p>
                    <span>Hash:</span>
                    <p>{hash}</p>
                    <span>Description:</span>
                    <p>{description}</p>
                </div>
            </div>
        </div>
    );
}


export const Header = connect(
    (state: AppState) => ({
        testCase: state.testCase,
        splitMode: state.splitMode,
        actionsFilter: state.actionsFilter,
        fieldsFilter: state.fieldsFilter,
        showFilter: state.showFilter
    }),
    dispatch => ({
        nextTestCaseHandler: () => dispatch(nextTestCase()),
        prevTestCaseHandler: () => dispatch(prevTestCase()),
        backToListHandler: () => dispatch(resetTestCase()),
        switchSplitMode: () => dispatch(switchSplitMode()),
        switchFieldsFilter: (status: StatusType) => dispatch(switchFieldsFilter(status)),
        switchActionsFilter: (status: StatusType) => dispatch(switchActionsFilter(status)),
        showFilterHandler: () => dispatch(showFilter())
    })
)(HeaderBase)
