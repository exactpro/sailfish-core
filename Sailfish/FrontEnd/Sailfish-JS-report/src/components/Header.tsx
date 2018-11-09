import {h} from 'preact';
import '../styles/styles.scss'

export const Header = ({
    Name,
    Status,
    Time,
    StartTime,
    FinishTime,
    Id,
    Hash,
    Description,
    nextHandler,
    prevHandler
} : {
    Name: string,
    Status: string,
    Time: string,
    StartTime: string,
    FinishTime: string,
    Id: string,
    Hash: string,
    Description: string,
    nextHandler?: Function,
    prevHandler?: Function
}) => {
    const statusClass = "header-status " + Status.toLowerCase();

    return(
        <div class="header-root">
            <div class={statusClass}>
                <div class="header-status-button">
                    <h3>Back to list</h3>
                </div>
                <div class="header-status-button">
                    <h3>Go top</h3>
                </div>
                <div class="header-status-name">
                    <h1>{'<'}</h1>
                    <h1>{Name} — {Status} — {Time}</h1>
                    <h1>{'>'}</h1>
                </div>
                <div class="header-status-button">
                    <h3>Split Mode</h3>
                </div>
                <div class="header-status-button">
                    <h3>View filter</h3>
                </div>
            </div>
            <div class="header-description">
                <div class="header-description-element">
                    <span>Start:</span>
                    <p>{StartTime}</p>
                    <span>Finish:</span>
                    <p>{FinishTime}</p>
                    <span>ID:</span>
                    <p>{Id}</p>
                    <span>Hash:</span>
                    <p>{Hash}</p>
                    <span>Description:</span>
                    <p>{Description}</p>
                </div>
            </div>
        </div>
    );
}