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
import "../styles/root.scss";
import TestCaseLayout from "./TestCaseLayout";
import Report, { isReport } from '../models/Report';
import TestCase from "../models/TestCase";
import ReportLayout from '../components/ReportLayout';
import { connect } from 'preact-redux';
import AppState from "../state/AppState";
import { setTestCase, setReport, setTestCasePath, selectActionById, selectVerification } from "../actions/actionCreators";
import { 
    getUrlSearchString,
    ACTION_PARAM_KEY,
    MESSAGE_PARAM_KEY,
    TEST_CASE_PARAM_KEY
} from "../middleware/urlHandler";

const REPORT_FILE_PATH = 'index.html';

interface AppProps {
    report: Report;
    testCase: TestCase;
    testCaseFilePath: string;
    updateTestCase: (testCase: TestCase) => any;
    updateTestCasePath: (testCasePath: string) => any;
    selectAction: (actionId: number) => any;
    selectMessage: (messageId: number) => any;
    updateReport: (report: Report) => any;
}

class AppBase extends Component<AppProps, {}> {

    private searchParams : URLSearchParams;

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

    componentWillMount() {
        this.validateUrl();
    }

    componentDidUpdate(prevProps: AppProps) {

        // we use top.window instared of window to work with real window url, not iframe url

        // We can't use componentDidMount for this, because report file not yet loaded.
        // Only first funciton call will use it.
        if (!this.searchParams) {
            this.searchParams = new URLSearchParams(getUrlSearchString(top.window.location.href));
            this.handleSharedUrl();
            return;
        }
    }

    handleSharedUrl() {
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

    /**
     * This function replaces url file path to index.html when we go to the new report from the old
     */
    validateUrl() {
        const href = top.window.location.href,
            filePath = href.slice(href.lastIndexOf('/'));

        if (!filePath.includes(REPORT_FILE_PATH)) {
            top.window.history.pushState({}, "", href.replace(filePath, '/' + REPORT_FILE_PATH));
        }
    }

    selectTestCaseById(testCaseId: string) {
        const testCaseMetadata = this.props.report.metadata.find(metadata => metadata.id === testCaseId);
        
        if (testCaseMetadata) {
            this.props.updateTestCasePath(testCaseMetadata.jsonpFileName);
        }
    }

    render({report, testCase, testCaseFilePath}: AppProps, {}: {}) {
        if (!report) return (
            <div class="root">
                <p>Loading json...</p>
                <script src="reportData/report.js"></script>
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
                <script src={"reportData/" + testCaseFilePath}></script>
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
        updateTestCasePath: (testCasePath: string) => dispatch(setTestCasePath(testCasePath)),
        selectAction: (actionId: number) => dispatch(selectActionById(actionId)),
        selectMessage: (messageId: number) => dispatch(selectVerification(messageId)),
        updateReport: (report: Report) => dispatch(setReport(report))
    })
)(AppBase)