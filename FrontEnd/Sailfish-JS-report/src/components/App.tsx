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

import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import Report, { isReport } from '../models/Report';
import TestCase from "../models/TestCase";
import ReportLayout from '../components/ReportLayout';
import { connect } from 'preact-redux';
import AppState from "../state/AppState";
import { setTestCase, setReport, setTestCasePath } from "../actions/actionCreators";

interface AppProps {
    report: Report;
    testCase: TestCase;
    testCaseFilePath: string;
    updateTestCase: (testCase: TestCase) => any;
    updateTestCasePath: (testCasePath: string) => any;
    updateReport: (report: Report) => any;
}

class AppBase extends Component<AppProps, {}> {

    constructor(props) {
        super(props);
        window['loadJsonp'] = this.loadJsonpHandler.bind(this);
    }

    loadJsonpHandler(jsonp: Report | TestCase) {
        if (isReport(jsonp)) {
            this.props.updateReport(jsonp as Report);
        } else {
            this.props.updateTestCase(jsonp as TestCase);
        }
    }

    render({report, testCase, testCaseFilePath}: AppProps, {}: {}) {
        if (!report) return (
            <div class="root">
                <p>Loading json...</p>
                <script src="report/report.js"></script>
            </div>
        );

        if (testCase) {
            return (
                <div class="root">
                    <TestCaseLayout />
                </div>
            )
        }

        if (!testCaseFilePath) {
            return (
                <div class="root">
                    <ReportLayout/>
                </div>
            );
        }

        return (
            <div class="root">
                <p>Loading json...</p>
                <script src={"report/" + testCaseFilePath}></script>
            </div>
        )
    };
}

export const App = connect(
    (state: AppState) => ({
        report: state.report,
        testCase: state.testCase,
        testCaseFilePath: state.currentTestCasePath
    }),
    dispatch => ({
        updateTestCase: (testCase: TestCase) => dispatch(setTestCase(testCase)),
        updateReport: (report: Report) => dispatch(setReport(report)),
        updateTestCasePath: (testCasePath: string) => dispatch(setTestCasePath(testCasePath))
    })
)(AppBase as any)