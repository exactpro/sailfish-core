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
import React from 'react';
import ReportState from '../state/models/ReportState';
import { formatTime, getSecondsPeriod } from '../helpers/date';

export interface RunInformationProps {
    report: ReportState;
}

const RunInformation = ({ report }: RunInformationProps) => {
    const executionTime = getSecondsPeriod(report.startTime, report.finishTime);  
    const plugins = report.plugins ? Object.entries(report.plugins) : [];
    return (<React.Fragment>
        <div className="report-summary__block">
            <div className="run-information__element">
                <div className="run-information__element-title">Host</div>
                <div className="run-information__element-value">{report.hostName}</div>
            </div>
            <div className="run-information__element">
                <div className="run-information__element-title">User</div>
                <div className="run-information__element-value">{report.userName}</div>
            </div>
        </div>
        <div className="report-summary__divider"/>
        <div className="report-summary__block">
            <div className="run-information__element">
                <div className="run-information__element-title">ScriptRun ID</div>
                <div className="run-information__element-value">{report.scriptRunId}</div>
            </div>
            <div className="run-information__element">
                <div className="run-information__element-title">Report Date</div>
                <div className="run-information__element-value">{formatTime(report.startTime)}</div>
            </div>
            <div className="run-information__element">
                <div className="run-information__element-title">Execution time</div>
                <div className="run-information__element-value">{executionTime}</div>
            </div>
        </div>
        <div className="report-summary__divider"/>
        {
            plugins.length ?
                (
                    <div className="report-summary__element">
                        <div className="report-summary__element-title">Plugins</div>
                        <div className="report-summary__element-value">
                            {plugins.map(([name, version], index) => <p key={index}>{name}: {version}</p>)}
                        </div>
                    </div>
                ) : (
                    <div className="report-summary__element">
                        <div className="report-summary__element-title">No plugins</div>
                    </div>
                )
        }
    </React.Fragment>)
}

export default RunInformation;