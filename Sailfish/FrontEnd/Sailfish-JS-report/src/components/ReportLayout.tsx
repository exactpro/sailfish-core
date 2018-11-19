import {h} from 'preact';
import Report from '../models/Report';
import TestCase from '../models/TestCase';

export interface ReportLayoutProps {
    report: Report;
    onTestCaseSelect: (testCase: TestCase, name: string) => void;
}

export const ReportLayout = ({report, onTestCaseSelect}: ReportLayoutProps) => {
    let testCaseCount = 1;
    return (
        <div class="report-root">
            {report.testCases.map(testCase => {
                const name = "TestCase " + testCaseCount++;
                return <div class="report-testcase"
                    style={{cursor: "pointer"}}
                    onClick={() => onTestCaseSelect(testCase, name)}>
                    {name}
                </div>
            })}
        </div>
    )
}