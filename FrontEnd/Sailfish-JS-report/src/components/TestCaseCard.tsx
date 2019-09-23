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
import {TestcaseMetadata} from '../models/TestcaseMetadata';
import {formatTime, getSecondsPeriod} from '../helpers/dateFormatter';
import '../styles/report.scss';
import {createSelector} from '../helpers/styleCreators';
import {KnownBugIndicator} from "./knownbugs/KnownBugIndicator";
import {KnownBugSummary} from "./knownbugs/KnownBugSummary";
import '../styles/report.scss';
import {connect} from "react-redux";
import AppState from "../state/models/AppState";

interface Props {
    metadata: TestcaseMetadata;
    index: number;
    knownBugsEnabled: boolean;
    isSelected: boolean;
    handleClick: (metadata: TestcaseMetadata) => any;
}

function TestCaseCard({ metadata, handleClick, index, knownBugsEnabled, isSelected }: Props) {

    const elapsedTime = getSecondsPeriod(metadata.startTime, metadata.finishTime);
    const baseRef = React.useRef<HTMLDivElement>();

    React.useEffect(() => {
        if (isSelected) {
            baseRef.current.scrollIntoView();
        }
    }, [isSelected]);
    
    const rootClass = createSelector(
        "tc-card",
        metadata.status.status,
        isSelected ? "selected" : null
    );

    const baseClickHandler = () => {
        // Don't trigger 'click' event when user selects text
        if (window.getSelection().type == 'Range') {
            return;
        }

        handleClick(metadata);
    }

    return (
        <div className={rootClass}
            ref={baseRef}
            onClick={baseClickHandler}>
            <div className="tc-card__index">{index}</div>
            <div className="tc-card__title">
                <div className="tc-card__name">{metadata.name}</div>
                {
                    metadata.description ?
                        <div className="tc-card__description"> — {metadata.description}</div> :
                        null
                }
            </div>
            <div className="tc-card__status">
                {metadata.status.status.toUpperCase()}
            </div>
            <div className="tc-card__info">
                <div className="tc-card__info-element">
                    <div className="tc-card__info-title">Start</div>
                    <div className="tc-card__info-value">{formatTime(metadata.startTime)}</div>
                </div>
                <div className="tc-card__info-element">
                    <div className="tc-card__info-title">Finish</div>
                    <div className="tc-card__info-value">{formatTime(metadata.finishTime)}</div>
                </div>
                <div className="tc-card__info-element">
                    <div className="tc-card__info-title">ID</div>
                    <div className="tc-card__info-value">{metadata.id}</div>
                </div>
                <div className="tc-card__info-element">
                    <div className="tc-card__info-title">Hash</div>
                    <div className="tc-card__info-value">{metadata.hash}</div>
                </div>
            </div>
            <div className="tc-card__elapsed-time">{elapsedTime}</div>
            {
                knownBugsEnabled && metadata.bugs.length > 0 ? (
                    <React.Fragment>
                        <div className="tc-card__divider"/>
                        <KnownBugIndicator data={metadata.bugs}/>
                        <KnownBugSummary data={metadata.bugs}/>
                    </React.Fragment>
                ) : null
            }
        </div>
    )
}

export default TestCaseCard;
