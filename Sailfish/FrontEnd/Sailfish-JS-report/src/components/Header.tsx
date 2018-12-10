import {h} from 'preact';
import TestCase  from '../models/TestCase';
import '../styles/header.scss';
import { StatusType } from '../models/Status';
import { FilterPanel } from './FilterPanel';

interface HeaderProps {
    name: string;
    testCase: TestCase;
    splitMode: boolean;
    showFilter: boolean;
    actionsFilter: StatusType[];
    fieldsFilter: StatusType[];
    nextHandler?: Function;
    prevHandler?: Function;
    goTopHandler: Function;
    backToListHandler: Function;
    splitModeHandler?: Function;
    showFilterHandler: Function;
    actionsFilterHandler: (status: StatusType) => void;
    fieldsFilterHandler: (status: StatusType) => void;
}

export const Header = ({
    name,
    testCase,
    splitMode,
    showFilter,
    actionsFilter,
    fieldsFilter,
    splitModeHandler,
    nextHandler,
    prevHandler,
    backToListHandler,
    showFilterHandler,
    actionsFilterHandler,
    fieldsFilterHandler,
    goTopHandler
} : HeaderProps) => {
    const {
        status,
        startTime,
        finishTime,
        id,
        hash,
        description,
    } = testCase;
    const statusClass = ["header-status", status.status.toLowerCase(), (showFilter ? "filter" : "")].join(' '),
        prevButtonClas = ["header-status-name-icon", "left", (prevHandler ? "enabled" : "disabled")].join(' '),
        nextButtonClass = ["header-status-name-icon", "right", (nextHandler ? "enabled" : "disabled")].join(' ');
    
    return(
        <div class="header">
            <div class={statusClass}>
                <div class="header-status-button"
                    onClick={e => backToListHandler()}>
                    <div class="header-status-button-icon list"/>
                    <h3>Back to list</h3>
                </div>
                <div class="header-status-button"
                    onClick={() => goTopHandler()}>
                    <div class="header-status-button-icon gotop"/>
                    <h3>Go top</h3>
                </div>
                <div class="header-status-name">
                    <div class={prevButtonClas}
                        onClick={prevHandler ? () => prevHandler() : null}/>
                    <h1>{name} — {status.status}</h1>
                    <div class={nextButtonClass}
                        onClick={nextHandler ? () => nextHandler() : null}/>
                </div>
                <div class="header-status-button" onClick={() => splitModeHandler()}>
                    <div class="header-status-button-icon mode"/>
                    <h3>{splitMode ? "List Mode" : "Split Mode"}</h3>
                </div>
                <div class="header-status-button" onClick={() => showFilterHandler()}>
                    <div class="header-status-button-icon filter"/>
                    <h3>{showFilter ? "Hide filter" : "Show filter"}</h3>
                </div>
            </div>
            {
                    showFilter ? 
                    <FilterPanel 
                        actionsFilters={actionsFilter}
                        fieldsFilters={fieldsFilter}
                        actionFilterHandler={actionsFilterHandler}
                        fieldsFilterHandler={fieldsFilterHandler}/>
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