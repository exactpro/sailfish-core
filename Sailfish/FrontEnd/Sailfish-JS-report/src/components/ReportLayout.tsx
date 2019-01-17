import {h} from 'preact';
import Report from '../models/Report';
import TestCase from '../models/TestCase';
import { connect } from 'preact-redux';
import AppState from '../state/AppState';
import { setTestCasePath } from '../actions/actionCreators';

interface ReportLayoutProps {
    report: Report;
    onTestCaseSelect: (testCaseName: string) => void;
}

const ReportLayoutBase = ({report, onTestCaseSelect}: ReportLayoutProps) => {
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

const ReportLayout = connect(
    (state: AppState) => ({
        report: state.report
    }),
    dispatch => ({
        onTestCaseSelect: (testCaseName: string) => dispatch(setTestCasePath(testCaseName))
    })
)(ReportLayoutBase);

export default ReportLayout;