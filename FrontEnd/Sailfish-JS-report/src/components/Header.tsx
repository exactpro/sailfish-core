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
import TestCase from '../models/TestCase';
import '../styles/header.scss';
import { StatusType } from '../models/Status';
import { FilterPanel } from './FilterPanel';
import { connect } from 'react-redux';
import AppState from '../state/models/AppState';
import {
    resetTestCase,
    switchActionsFilter,
    switchFieldsFilter
} from '../actions/actionCreators';
import { getSecondsPeriod, formatTime } from '../helpers/dateFormatter';
import { createSelector } from '../helpers/styleCreators';
import { ThunkDispatch } from 'redux-thunk';
import StateActionType from '../actions/stateActions';
import { loadNextTestCase, loadPrevTestCase } from '../thunks/loadTestCase';

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

const HeaderBase = ({ testCase, actionsFilter, fieldsFilter, nextTestCaseHandler, prevTestCaseHandler, backToListHandler, switchActionsFilter, switchFieldsFilter }: HeaderProps) => {
        
    const [ showFilter, setShowFilter ] = React.useState(false);

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
        infoClass = createSelector(
            "header__info", 
            showFilter ? "filter-enabled" : null
        );

    const period = getSecondsPeriod(startTime, finishTime);

    return (
        <div className={rootClass}>
            <div className="header__main   header-main">
                <div className="header-button   header-main__contol-button"
                    onClick={backToListHandler}>
                    <div className="header-button__icon go-back" />
                    <div className="header-button__title">Back to list</div>
                </div>
                <div className="header-main__name ">
                    <div className="header-button"
                        onClick={prevTestCaseHandler}>
                        <div className="header-button__icon left"/>
                    </div>
                    <div className="header-main__title">
                        {(name || 'Test Case')} — {status.status} — {period}
                    </div>
                    <div className="header-button"
                        onClick={nextTestCaseHandler}>
                        <div className="header-button__icon right"/>
                    </div>
                </div>
                <div className="header-button   header-main__contol-button" onClick={() => setShowFilter(!showFilter)}>
                    <div className="header-button__icon filter" />
                    <div className="header-button__title">{showFilter ? "Hide filter" : "Show filter"}</div>
                </div>
            </div>
            <div className={infoClass}>
                <div className="header__info-element">
                    <span>Start:</span>
                    <p>{formatTime(startTime)}</p>
                </div>
                <div className="header__info-element">
                    <span>Finish:</span>
                    <p>{formatTime(finishTime)}</p>
                </div>
                <div className="header__description">
                    {description}
                </div>
                <div className="header__info-element">
                    <span>ID:</span>
                    <p>{id}</p>
                </div>
                <div className="header__info-element">
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

export const Header = connect(
    (state: AppState) => ({
        testCase: state.selected.testCase,
        splitMode: state.view.splitMode,
        actionsFilter: state.filter.actionsFilter,
        fieldsFilter: state.filter.fieldsFilter
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateActionType>) => ({
        nextTestCaseHandler: () => dispatch(loadNextTestCase()),
        prevTestCaseHandler: () => dispatch(loadPrevTestCase()),
        backToListHandler: () => dispatch(resetTestCase()),
        switchFieldsFilter: (status: StatusType) => dispatch(switchFieldsFilter(status)),
        switchActionsFilter: (status: StatusType) => dispatch(switchActionsFilter(status))
    })
)(HeaderBase)
