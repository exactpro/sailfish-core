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

interface TestCaseCardProps {
    metadata: ReportMetadata;
    index: number;
    selectHandler: (metadata: ReportMetadata) => any;
}

const TestCaseCard = ({ metadata, selectHandler, index }: TestCaseCardProps) => {

    const elapsedTime = getSecondsPeriod(metadata.startTime, metadata.finishTime);

    const rootClass = [
        "tc-card",
        metadata.status.status
    ].join(' ').toLowerCase();

    return (
        <div class={rootClass}
            onClick={() => {

                // preventing select handling when user just selecting some text on card
                if (window.getSelection().type == 'Range') {
                    return;
                }
                
                selectHandler(metadata)
            }}>
            <div class="index">{index}</div>
            <div class="title">
                <div class="name">{metadata.name}</div>
                {
                    metadata.description ?
                        <div class="description"> — {metadata.description}</div> :
                        null
                }
            </div>
            <div class="status">
                {metadata.status.status.toUpperCase()}
            </div>
            <div class="info">  
                <div class="item">
                    <div class="title">Start</div>
                    <div class="value">{formatTime(metadata.startTime)}</div>
                </div>  
                <div class="item">
                    <div class="title">Finish</div>
                    <div class="value">{formatTime(metadata.finishTime)}</div>
                </div>  
                <div class="item">
                    <div class="title">ID</div>
                    <div class="value">{metadata.id}</div>
                </div>  
                <div class="item">
                    <div class="title">Hash</div>
                    <div class="value">{metadata.hash}</div>
                </div>
            </div>
            <div class="elapsed-time">{elapsedTime}</div>
        </div>
    )
}

export default TestCaseCard;
