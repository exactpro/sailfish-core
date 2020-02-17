/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
import { connect } from 'react-redux';
import AppState from '../state/models/AppState';
import { resetTestCase } from '../actions/actionCreators';
import { getSecondsPeriod, formatTime } from '../helpers/date';
import { createBemElement, createSelector } from '../helpers/styleCreators';
import { ThunkDispatch } from 'redux-thunk';
import StateActionType from '../actions/stateActions';
import { loadNextTestCase, loadPrevTestCase } from '../thunks/loadTestCase';
import SearchInput from './search/SearchInput';
import { MlUploadIndicator } from "./machinelearning/MlUploadIndicator";
import LiveTimer from './LiveTimer';
import FilterPanel from "./filter/FilterPanel";
import { getIsFilterApplied, getIsMessageFilterApplied } from "../selectors/filter";
import useOutsideClickListener from "../hooks/useOutsideClickListener";
import { downloadTxtFile } from '../helpers/files/downloadTxt';
import { getFilteredMessages } from '../selectors/messages';
import Message from '../models/Message';
import Dropdown from './Dropdown';

interface StateProps {
    testCase: TestCase;
    isNavigationEnabled: boolean;
    isFilterApplied: boolean;
    messages: Message[];
    isMessageFilterApplied: boolean;
}

interface DispatchProps {
    nextTestCaseHandler: () => void;
    prevTestCaseHandler: () => void;
    backToListHandler: () => void;
}

interface Props extends StateProps, DispatchProps {}

export const HeaderBase = ({
   testCase,
   isNavigationEnabled,
   isFilterApplied,
   nextTestCaseHandler,
   prevTestCaseHandler,
   backToListHandler,
   messages,
   isMessageFilterApplied
}: Props) => {
    const {
        name = 'Test Case',
        startTime,
        finishTime,
        id,
        hash,
        description,
    } = testCase;

    const [showFilter, setShowFilter] = React.useState(false);
    const filterBaseRef = React.useRef<HTMLDivElement>();
    const filterButtonRef = React.useRef<HTMLDivElement>();

    useOutsideClickListener(filterBaseRef, e => {
        if (!filterButtonRef.current?.contains(e.target as Element)) {
            setShowFilter(false);
        }
    });

    const isLive = finishTime == null,
        status = testCase.status.status || 'RUNNING',
        period = getSecondsPeriod(startTime, finishTime);

    const rootClass = createSelector(
            "header",
            status
        ),
        navButtonClass = createSelector(
            "header-button",
            isNavigationEnabled ? '' : 'disabled'
        ),
        filterWrapperClass = createBemElement(
            "header-button",
            "filter-wrapper",
            showFilter ? "active" : null
        ),
        filterTitleClass = createBemElement(
            "header-button",
            "title",
            showFilter ? "active" : null,
            !showFilter && isFilterApplied ? "applied" : null
        ),
        filterIconClass = createBemElement(
            "header-button",
            "icon",
            "filter-icon",
            showFilter ? "active" : null,
            !showFilter && isFilterApplied ? "applied" : null
        );

    const downloadMessages = (fields: ('contentHumanReadable' | 'raw')[]) => {
        const content = messages
          .map(msg =>
            fields
              .filter(field => msg[field] !== 'null')
              .map(field => msg[field])
              .join('\n')
              .replace(/\n$/, ''),
          )
          .filter(Boolean)
          .join('\n\n');
        const fileName = `${testCase.name}_messages_${new Date().toISOString()}.txt`;
        downloadTxtFile([content], fileName);
    }

    return (
        <div className={rootClass}>
            <div className="header__main   header-main">
                <div className="header__group">
                    <div className="header-button"
                        onClick={backToListHandler}>
                        <div className="header-button__icon go-back"/>
                        <div className="header-button__title">Back to list</div>
                    </div>
                    <Dropdown 
                        disabled={messages.length === 0}
                        className="header__dropdown">
                        <Dropdown.Trigger>
                            <div className="header-button__icon export" />
                            <div>Export {isMessageFilterApplied && " Filtered "} Messages</div>
                            <div className="header-button__icon down" />
                        </Dropdown.Trigger>
                        <Dropdown.Menu>
                            <Dropdown.MenuItem
                                onClick={() => downloadMessages(['contentHumanReadable'])}>
                                Human-Readable
                            </Dropdown.MenuItem>
                            <Dropdown.MenuItem
                                onClick={() => downloadMessages(['raw'])}>
                                Raw Data
                            </Dropdown.MenuItem>
                            <Dropdown.MenuItem
                                onClick={() => downloadMessages(['contentHumanReadable', 'raw'])}>
                                Both
                            </Dropdown.MenuItem>
                        </Dropdown.Menu>
                    </Dropdown>
                </div>
                <div className="header-main__name header__group">
                    <div className={navButtonClass}
                         onClick={() => isNavigationEnabled && prevTestCaseHandler()}>
                        <div className="header-button__icon left"/>
                    </div>
                    <div className="header-main__title">
                        {
                            isLive ? (
                                <React.Fragment>
                                    <div className="header-main__spinner"/>
                                    {name} — {status} — <LiveTimer startTime={startTime}/>
                                </React.Fragment>
                            ) : (
                                `${name} — ${status} — ${period}`
                            )
                        }
                    </div>
                    <div className={navButtonClass}
                         onClick={() => isNavigationEnabled && nextTestCaseHandler()}>
                        <div className="header-button__icon right"/>
                    </div>
                </div>
                <div className="header__group">
                    <div className={filterWrapperClass}>
                        <div className="header-button"
                            ref={filterButtonRef}
                            onClick={() => setShowFilter(!showFilter)}>
                            <div className={filterIconClass}/>
                            <div className={filterTitleClass}>
                                {
                                    isFilterApplied ?
                                        "Filter Applied" :
                                        showFilter ?
                                            "Hide Filter" :
                                            "Show Filter"
                                }
                            </div>
                        </div>
                        {
                            showFilter ? (
                                <div ref={filterBaseRef} className="filter-wrapper">
                                    <FilterPanel/>
                                </div>
                            ) : null
                        }
                    </div>
                    <div className="header-main__search">
                        <SearchInput/>
                    </div>
                </div>
            </div>
            <div className="header__info">
                <div className="header__group">
                    <div className="header__info-element">
                        <span>Start:</span>
                        <p>{formatTime(startTime)}</p>
                    </div>
                    {
                        isLive ? null : (
                            <div className="header__info-element">
                                <span>Finish:</span>
                                <p>{formatTime(finishTime)}</p>
                            </div>
                        )
                    }
                </div>
                <div className="header__description header__group">
                    {description}
                </div>
                <div className="header__group">
                    <div className="header__info-element">
                        <span>ID:</span>
                        <p>{id}</p>
                    </div>
                    <div className="header__info-element">
                        <span>Hash:</span>
                        <p>{hash}</p>
                    </div>
                    <div className="header__info-element">
                        <MlUploadIndicator/>
                    </div>
                </div>
            </div>
        </div>
    );
};

export const Header = connect(
    (state: AppState): StateProps => ({
        testCase: state.selected.testCase,
        isNavigationEnabled: state.report.metadata.length > 1,
        isFilterApplied: getIsFilterApplied(state),
        messages: getIsMessageFilterApplied(state) ? getFilteredMessages(state): state.selected.testCase.messages,
        isMessageFilterApplied: getIsMessageFilterApplied(state)
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateActionType>): DispatchProps => ({
        nextTestCaseHandler: () => dispatch(loadNextTestCase()),
        prevTestCaseHandler: () => dispatch(loadPrevTestCase()),
        backToListHandler: () => dispatch(resetTestCase())
    })
)(HeaderBase);
