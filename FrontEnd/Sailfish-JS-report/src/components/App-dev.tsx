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
import { setTestCase, setTestCasePath, selectAction } from "../actions/actionCreators";
import { selectActionById } from '../actions/actionCreators';
import { selectMessages } from '../actions/actionCreators';
import { 
    ACTION_PARAM_KEY,
    MESSAGE_PARAM_KEY,
    TEST_CASE_PARAM_KEY
} from "../middleware/urlHandler";

interface AppProps {
    report: Report;
    testCase: TestCase;
    testCaseFilePath: string;
    updateTestCase: (testCase: TestCase) => any;
    updateTestCasePath: (testCasePath: string) => any;
    selectAction: (actionId: number) => any;
    selectMessage: (messageId: number) => any;
}

class AppBase extends Component<AppProps> {

    private searchParams: URLSearchParams;

    constructor(props: AppProps) {
        super(props);
    
        this.searchParams = new URLSearchParams(window.location.search);
    }

    componentDidMount() {
        // shared URL
        const testCaseId = this.searchParams.get(TEST_CASE_PARAM_KEY),
            actionId = this.searchParams.get(ACTION_PARAM_KEY),
            msgId = this.searchParams.get(MESSAGE_PARAM_KEY);

        if (testCaseId != null) {
            this.selectTestCaseById(testCaseId)
        }

        if (msgId !== null && !isNaN(Number(msgId))) {
            this.props.selectMessage(Number(msgId))
        }

        if (actionId !== null && !isNaN(Number(actionId))) {
            this.props.selectAction(Number(actionId));
        }
    }

    selectTestCaseById(testCaseId: string) {
        const testCaseMetadata = this.props.report.metadata.find(metadata => metadata.id === testCaseId);
        
        if (testCaseMetadata) {
            this.props.updateTestCasePath(testCaseMetadata.jsonpFileName);
        }
    }

    render({ report, testCase, testCaseFilePath, updateTestCase }: AppProps) {

        if (testCase) {
            return (
                <div class="root">
                    <TestCaseLayout />
                </div>
            );
        }

        if (testCaseFilePath) {
            const currentMetadata = report.metadata.find(metadata => metadata.jsonpFileName === testCaseFilePath),
                currentTestCase = report.testCases.find(testCase => testCase.name === currentMetadata.name);

            updateTestCase(currentTestCase)
            return null;
        }

        return (
            <div class="root">
                <ReportLayout />
            </div>
        );
    }
}

export const App = connect(
    (state: AppState) => ({
        report: state.report,
        testCase: state.testCase,
        testCaseFilePath: state.currentTestCasePath
    }),
    dispatch => ({
        updateTestCase: (testCase: TestCase) => dispatch(setTestCase(testCase)),
        updateTestCasePath: (testCasePath: string) => dispatch(setTestCasePath(testCasePath)),
        selectAction: (actionId: number) => dispatch(selectActionById(actionId)),
        selectMessage: (messageId: number) => dispatch(selectMessages([messageId]))
    })
)(AppBase)