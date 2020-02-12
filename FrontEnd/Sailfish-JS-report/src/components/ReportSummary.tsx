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
import AlertCard from './AlertCard';
import Tag from './Tag';
import { statusValues, StatusType } from '../models/Status';
import { TestCaseMetadata, isTestCaseMetadata } from '../models/TestcaseMetadata';
import { createStyleSelector } from '../helpers/styleCreators';

export interface ReportSummaryProps {
    report: ReportState;
}

function renderStatusInfo(status: StatusType, metadata: TestCaseMetadata[]): React.ReactNode {
    const testCasesCount = metadata.filter(metadata => metadata.status.status == status).length,
        valueClassName = createStyleSelector(
            "report-summary__element-value",
            "bold",
            status
        );

    if (!testCasesCount) {
        return null;
    }

    return (
        <div className="report-summary__element" key={status}>
            <div className={valueClassName}>{status.toUpperCase()}</div>
            <div className={valueClassName}>{testCasesCount}</div>
        </div>
    )
}

const ReportSummary = ({ report }: ReportSummaryProps) => {
    const filteredMetadata = report.metadata.filter(isTestCaseMetadata);
    const tags = report.tags || [];
    const alerts = report.alerts || [];
    return (
    <React.Fragment>
        <div className="report-summary__block">
            <div className="report-summary__element">
                <div className="report-summary__element-title">Test Cases</div>
                <div className="report-summary__element-value">{report.metadata.length}</div>
            </div>
            {
                statusValues.map(statusValue => renderStatusInfo(statusValue, filteredMetadata))
            }
        </div>
        <div className="report-summary__divider"/>
        {
            tags.length ?
                (
                    <div className="report-summary__tags">
                        <div className="report-summary__element report-summary__tags-header">
                            <div className="report-summary__element-title">Tags</div>
                            <div className="report-summary__element-value">{tags.length}</div>
                        </div>
                        <div className="report-summary__tags-list">
                            {tags.map(tag => <Tag tag={tag}/>)}
                        </div>
                    </div>
                ) : (
                    <div className="report-summary__element">
                        <div className="report-summary__element-title">No tags</div>
                    </div>
                )
        }
        <div className="report-summary__divider"/>
        <div className="report-summary__element">
            <div className="report-summary__element-title">Errors and Warnings</div>
            <div className="report-summary__element-value">{alerts.length}</div>
        </div>
        <div className="report-summary__alerts">
            {alerts.map((alert, i) => <AlertCard {...alert} key={i}/>)}
        </div>
    </React.Fragment>)
}

export default ReportSummary;