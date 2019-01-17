import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import ReportLayout from './ReportLayout';
import TestCase from "../models/TestCase";
import Report from "../models/Report";
import { connect } from 'preact-redux';
import AppState from "../state/AppState";
import { setTestCase } from "../actions/actionCreators";

interface AppProps {
    report: Report;
    testCase: TestCase;
    testCaseFilePath: string;
    updateTestCase: (testCase: TestCase) => void;
}

const AppBase = ({report, testCase, testCaseFilePath, updateTestCase}: AppProps) => {
    console.log(report)

    if (testCase) {
        return(     
            <div class="root">
                <TestCaseLayout/>
            </div>
        );
    }

    if (testCaseFilePath) {
        updateTestCase(report.testCases.find(testCase => testCase.name === testCaseFilePath))
        return null;
    }

    return (
        <div class="root">
            <ReportLayout/>
        </div>
    );
}

export const App = connect(
    (state: AppState) => ({
        report: state.report,
        testCase: state.testCase,
        testCaseFilePath: state.currentTestCasePath
    }),
    dispatch => ({
        updateTestCase: (testCase: TestCase) => {
            dispatch(setTestCase(testCase))
        }
    })
)(AppBase as any)