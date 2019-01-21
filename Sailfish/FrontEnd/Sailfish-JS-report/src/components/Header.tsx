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
import { getSecondsPeriod } from '../helpers/dateFormatter';

interface HeaderProps {
    testCase: TestCase;
    splitMode: boolean;
    showFilter: boolean;
    actionsFilter: StatusType[];
    fieldsFilter: StatusType[];
    nextTestCaseHandler: () => any;
    prevTestCaseHandler: () => any;
    // TODO: implement
    //goTopHandler: () => any;
    backToListHandelr: () => any;
    switchSplitMode: () => any;
    switchActionsFilter: (status: StatusType) => any;
    switchFieldsFilter: (status: StatusType) => any;
    showFilterHandler: () => any;
}

const HeaderBase = ({ testCase, splitMode, actionsFilter, fieldsFilter, nextTestCaseHandler, prevTestCaseHandler, backToListHandelr,
    switchSplitMode: switchSplitMode, switchActionsFilter, switchFieldsFilter, showFilter, showFilterHandler }: HeaderProps) => {

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
        prevButtonClas = ["header-status-name-icon", "left", (prevTestCaseHandler ? "enabled" : "disabled")].join(' '),
        nextButtonClass = ["header-status-name-icon", "right", (nextTestCaseHandler ? "enabled" : "disabled")].join(' ');

    const period = getSecondsPeriod(startTime, finishTime);

    return (
        <div class="header">
            <div class={statusClass}>
                <div class="header-status-button"
                    onClick={backToListHandelr}>
                    <div class="header-status-button-icon list" />
                    <h3>Back to list</h3>
                </div>
                <div class="header-status-button"
                    onClick={() => { }}>
                    <div class="header-status-button-icon gotop" />
                    <h3>Go top</h3>
                </div>
                <div class="header-status-name">
                    <div class={prevButtonClas}
                        onClick={prevTestCaseHandler} />
                    <h1>{(name || 'Test Case')} — {status.status} — {period}</h1>
                    <div class={nextButtonClass}
                        onClick={nextTestCaseHandler} />
                </div>
                <div class="header-status-button" onClick={() => switchSplitMode()}>
                    <div class="header-status-button-icon mode" />
                    <h3>{splitMode ? "List Mode" : "Split Mode"}</h3>
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
                    <p>{startTime}</p>
                    <span>Finish:</span>
                    <p>{finishTime}</p>
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
        backToListHandelr: () => dispatch(resetTestCase()),
        switchSplitMode: () => dispatch(switchSplitMode()),
        switchFieldsFilter: (status: StatusType) => dispatch(switchFieldsFilter(status)),
        switchActionsFilter: (status: StatusType) => dispatch(switchActionsFilter(status)),
        showFilterHandler: () => dispatch(showFilter())
    })
)(HeaderBase)
