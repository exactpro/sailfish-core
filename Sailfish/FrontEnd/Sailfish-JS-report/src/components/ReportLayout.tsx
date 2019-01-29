import { h } from 'preact';
import Report from '../models/Report';
import TestCase from '../models/TestCase';
import { connect } from 'preact-redux';
import AppState from '../state/AppState';
import { setTestCasePath } from '../actions/actionCreators';
import "../styles/report.scss";
import { getSecondsPeriod } from '../helpers/dateFormatter';

interface ReportLayoutProps {
    report: Report;
    onTestCaseSelect: (testCaseName: string) => void;
}

const ReportLayoutBase = ({ report, onTestCaseSelect }: ReportLayoutProps) => {

    const executionTime = getSecondsPeriod(report.startTime, report.finishTime);
    let testCaseCount = 1;

    return (
        <div class="report">
            <div class="report-header">
                <p>Report script: {report.name}</p>
            </div>
            <div class="report-info">
                <div class="report-info-logo"></div>
                <div class="report-info-table">
                    <table>
                        <thead>
                            <th class="report-info-table-name" />
                            <th class="report-info-table-value" />
                        </thead>
                        <tbody>
                            <tr>
                                <td class="report-info-table-name">Host:</td>
                                <td>{report.hostName}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">User:</td>
                                <td>{report.userName}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Execution time:</td>
                                <td>{executionTime}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">ScriptRun Id:</td>
                                <td>{report.scriptRunId}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Date:</td>
                                <td>{report.startTime}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Version:</td>
                                <td>{report.version}</td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Plugins:</td>
                                <td>
                                    {
                                        Object.entries(report.plugins).map(([name, version]) => (
                                            <p>{name}: {version}</p>
                                        ))
                                    }
                                </td>
                            </tr>
                            <tr>
                                <td class="report-info-table-name">Test cases:</td>
                                <td>{report.testCaseLinks.length}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="report-testcases">
                <div class="report-testcases-list">
                    {report.testCaseLinks.map(testCase => (<div
                        style={{ cursor: "pointer" }}
                        onClick={() => onTestCaseSelect(testCase)}>
                        Test Case {testCaseCount++} : {testCase}
                    </div>))}
                </div>
            </div>
        </div>
    )
}

const ReportLayout = connect(
    (state: AppState) => ({
        report: state.report
    }),
    dispatch => ({
        onTestCaseSelect: (testCaseLink: string) => dispatch(setTestCasePath(testCaseLink))
    })
)(ReportLayoutBase);

export default ReportLayout;