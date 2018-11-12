import {h} from 'preact';
import '../styles/styles.scss'

export const Header = ({
    name,
    status,
    startTime,
    finishTime,
    id,
    hash,
    description,
    nextHandler,
    prevHandler
} : {
    name: string,
    status: string,
    startTime: string,
    finishTime: string,
    id: number,
    hash: number,
    description: string,
    nextHandler?: Function,
    prevHandler?: Function
}) => {
    const statusClass = "header-status " + status.toLowerCase();

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
                    <h1>{name} — {status}</h1>
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