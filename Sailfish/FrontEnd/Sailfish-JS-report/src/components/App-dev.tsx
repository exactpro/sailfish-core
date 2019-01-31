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