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

import { h } from 'preact';
import { ReportMetadata } from '../models/ReportMetadata';
import { formatTime, getSecondsPeriod } from '../helpers/dateFormatter';
import '../styles/report.scss';
import { createSelector } from '../helpers/styleCreators';

interface TestCaseCardProps {
    metadata: ReportMetadata;
    index: number;
    handleClick: (metadata: ReportMetadata) => any;
}

const TestCaseCard = ({ metadata, handleClick, index }: TestCaseCardProps) => {

    const elapsedTime = getSecondsPeriod(metadata.startTime, metadata.finishTime);

    const rootClass = createSelector(
        "tc-card",
        metadata.status.status
    );

    return (
        <div class={rootClass}
            onClick={() => {

                // Don't trigger 'click' event when user selects text
                if (window.getSelection().type == 'Range') {
                    return;
                }
                
                handleClick(metadata)
            }}>
            <div class="tc-card__index">{index}</div>
            <div class="tc-card__title">
                <div class="tc-card__name">{metadata.name}</div>
                {
                    metadata.description ?
                        <div class="tc-card__description"> — {metadata.description}</div> :
                        null
                }
            </div>
            <div class="tc-card__status">
                {metadata.status.status.toUpperCase()}
            </div>
            <div class="tc-card__info">  
                <div class="tc-card__info-element">
                    <div class="tc-card__info-title">Start</div>
                    <div class="tc-card__info-value">{formatTime(metadata.startTime)}</div>
                </div>  
                <div class="tc-card__info-element">
                    <div class="tc-card__info-title">Finish</div>
                    <div class="tc-card__info-value">{formatTime(metadata.finishTime)}</div>
                </div>  
                <div class="tc-card__info-element">
                    <div class="tc-card__info-title">ID</div>
                    <div class="tc-card__info-value">{metadata.id}</div>
                </div>  
                <div class="tc-card__info-element">
                    <div class="tc-card__info-title">Hash</div>
                    <div class="tc-card__info-value">{metadata.hash}</div>
                </div>
            </div>
            <div class="tc-card__elapsed-time">{elapsedTime}</div>
        </div>
    )
}

export default TestCaseCard;
