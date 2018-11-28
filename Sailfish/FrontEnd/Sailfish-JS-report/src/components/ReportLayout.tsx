import {h} from 'preact';
import Report from '../models/Report';
import TestCase from '../models/TestCase';

interface ReportLayoutProps {
    report: Report;
    onTestCaseSelect: (testCaseName: string) => void;
}

const ReportLayout = ({report, onTestCaseSelect}: ReportLayoutProps) => {
    return (
        <div class="report-root">
            {report.testCaseLinks.map(testCase => (<div class="report-testcase"
                    style={{cursor: "pointer"}}
                    onClick={() => onTestCaseSelect(testCase)}>
                    {testCase}
                </div>))}
        </div>
    )
}

export default ReportLayout;