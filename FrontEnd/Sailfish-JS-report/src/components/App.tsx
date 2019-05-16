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

import * as React from 'react';
import "../styles/root.scss";
import TestCaseLayout from "./TestCaseLayout";
import Report, { isReport } from '../models/Report';
import TestCase from "../models/TestCase";
import ReportLayout from '../components/ReportLayout';
import { connect } from 'react-redux';
import AppState from "../state/models/AppState";
import { setTestCase, setReport, selectActionById, selectVerification, setMlToken, setSubmittedMlData } from "../actions/actionCreators";
import { 
    getUrlSearchString,
    ACTION_PARAM_KEY,
    MESSAGE_PARAM_KEY,
    TEST_CASE_PARAM_KEY
} from "../middleware/urlHandler";
import { fetchToken } from "../helpers/machineLearning";
import { SubmittedData } from "../models/MlServiceResponse" 
import { loadReport } from '../thunks/loadReport';
import { ThunkDispatch } from 'redux-thunk';
import { StateActionType } from '../actions/stateActions';
import { loadTestCase } from '../thunks/loadTestCase';
import SplashScreen from './SplashScreen';

const REPORT_FILE_PATH = 'index.html';

interface AppStateProps {
    report: Report;
    testCase: TestCase;
    mlToken: string;
    submittedMlData: SubmittedData[];
    isLoading: boolean;
}

interface AppDispatchProps {
    setMlToken: (token: string) => any;
    setSubmittedMlData: (data: SubmittedData[]) => any;
    selectAction: (actionId: number) => any;
    selectMessage: (messageId: number) => any;
    loadReport: () => any;
    loadTestCase: (testCasePath: string) => any;
}

interface AppProps extends AppStateProps, AppDispatchProps {}

class AppBase extends React.Component<AppProps, {}> {

    private searchParams : URLSearchParams;

    componentDidUpdate(prevProps: AppProps) {

        // we use top.window instared of window to work with real window url, not iframe url

        // We can't use componentDidMount for this, because report file not yet loaded.
        // Only first funciton call will use it.
        if (!this.searchParams) {
            this.searchParams = new URLSearchParams(getUrlSearchString(top.window.location.href));
            this.handleSharedUrl();
        }

        if (!this.props.mlToken && this.props.report.reportProperties) {
            fetchToken(this.props.report.reportProperties.workFolder, this.props.setMlToken, this.props.setSubmittedMlData)
        }
    }

    componentDidMount() {
        this.props.loadReport();
        this.validateUrl();
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
        if (!this.props.report) {
            console.error("Trying to handle shared url before report load.");
            return;
        }

        const testCaseMetadata = this.props.report.metadata.find(metadata => metadata.id === testCaseId);
        
        if (testCaseMetadata) {
            this.props.loadTestCase(testCaseMetadata.jsonpFileName);
        } else {
            console.warn("Can't handle chared url: Test Case with this id not found");
        }
    }

    render() {
        const { testCase, isLoading } = this.props;

        if (isLoading) {
            return (
                <div className="root">  
                    <SplashScreen/>
                </div>
            )
        }

        if (testCase) {
            return (
                <div className="root">
                    <TestCaseLayout />
                </div>
            )
        }

        return (
            <div className="root">
                <ReportLayout/>
            </div>
        );
    };
}

const App = connect(
    (state: AppState): AppStateProps => ({
        report: state.report,
        testCase: state.selected.testCase,
        mlToken: state.machineLearning.token,
        submittedMlData: state.machineLearning.submittedData,
        isLoading: state.view.isLoading
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateActionType>): AppDispatchProps => ({
        selectAction: (actionId: number) => dispatch(selectActionById(actionId)),
        selectMessage: (messageId: number) => dispatch(selectVerification(messageId)),
        setMlToken: (token: string) => dispatch(setMlToken(token)),
        setSubmittedMlData: (data: SubmittedData[]) => dispatch(setSubmittedMlData(data)),
        loadReport: () => dispatch(loadReport()),
        loadTestCase: (testCasePath: string) => dispatch(loadTestCase(testCasePath))
    })
)(AppBase)

export default App;
