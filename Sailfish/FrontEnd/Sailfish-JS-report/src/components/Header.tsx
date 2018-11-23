import {h} from 'preact';
import TestCase  from '../models/TestCase';
import '../styles/header.scss';


export const Header = ({
    name,
    testCase,
    splitMode,
    splitModeHandler,
    nextHandler,
    prevHandler,
    backToListHandler
} : {
    name: string,
    testCase: TestCase,
    splitMode: boolean,
    nextHandler?: Function,
    prevHandler?: Function,
    goTopHandler?: Function,
    backToListHandler: Function;
    splitModeHandler?: Function;
    filterHandler?: Function;
}) => {
    const {
        status,
        startTime,
        finishTime,
        id,
        hash,
        description,
    } = testCase;
    const statusClass = "header-status " + status.toLowerCase(),
        prevButtonClas = ["header-status-name-icon", "left", (prevHandler ? "enabled" : "disabled")].join(' '),
        nextButtonClass = ["header-status-name-icon", "right", (nextHandler ? "enabled" : "disabled")].join(' ');
    
    return(
        <div class="header-root">
            <div class={statusClass}>
                <div class="header-status-button"
                    onClick={e => backToListHandler()}>
                    <h3>Back to list</h3>
                </div>
                <div class="header-status-button">
                    <h3>Go top</h3>
                </div>
                <div class="header-status-name">
                    <div class={prevButtonClas}
                        onClick={prevHandler ? () => prevHandler() : null}/>
                    <h1>{name} — {status}</h1>
                    <div class={nextButtonClass}
                        onClick={nextHandler ? () => nextHandler() : null}/>
                </div>
                <div class="header-status-button" onClick={e => splitModeHandler()}>
                    <h3>{splitMode ? "List Mode" : "Split Mode"}</h3>
                </div>
                <div class="header-status-button">
                    <h3>View filter</h3>
                </div>
            </div>
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