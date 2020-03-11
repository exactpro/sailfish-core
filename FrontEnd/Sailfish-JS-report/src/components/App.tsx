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
import TestCase from "../models/TestCase";
import ReportLayout from '../components/ReportLayout';
import { connect } from 'react-redux';
import AppState from "../state/models/AppState";
import {
    selectActionById,
    selectVerification,
    setSubmittedMlData,
    selectActionsScrollHintIds,
    selectMessagesScrollHintIds 
} from "../actions/actionCreators";
import { 
    getUrlSearchString,
    ACTION_PARAM_KEY,
    MESSAGE_PARAM_KEY,
    TEST_CASE_PARAM_KEY
} from "../middleware/urlHandler";
import { fetchToken } from "../thunks/machineLearning";
import { SubmittedData } from "../models/MlServiceResponse" 
import { initReport } from '../thunks/initReport';
import { ThunkDispatch } from 'redux-thunk';
import StateActionType from '../actions/stateActions';
import { loadTestCase } from '../thunks/loadTestCase';
import SplashScreen from './SplashScreen';
import Report from '../models/Report';
import { TestCaseMetadata } from '../models/TestcaseMetadata';

const REPORT_FILE_PATH = 'index.html';

interface AppStateProps {
    report: Report;
    testCase: TestCase;
    mlToken: string;
    submittedMlData: SubmittedData[];
    isLoading: boolean;
}

interface AppDispatchProps {
    fetchToken: () => any;
    setSubmittedMlData: (data: SubmittedData[]) => any;
    selectAction: (actionId: number) => any;
    selectMessage: (messageId: number) => any;
    loadReport: () => any;
    loadTestCase: (metadata: TestCaseMetadata) => any;
    selectActionsScrollHintIds: (actionId: Number) => void;
    selectMessagesScrollHintIds: (messageId: Number) => void;
}

interface AppProps extends AppStateProps, AppDispatchProps {}

class AppBase extends React.Component<AppProps, {}> {

    private searchParams : URLSearchParams;

    componentDidUpdate(prevProps: AppProps) {
        // We can't use componentDidMount for this, because report file not yet loaded.
        // Only first funciton call will use it.
        if (!this.searchParams) {
            this.searchParams = new URLSearchParams(getUrlSearchString(window.location.href));
            this.handleSharedUrl();
        }
        
        if (prevProps.report !== this.props.report && this.props.report !== null) {
            document.title = this.props.report.name;

            // init ML features
            if (!this.props.mlToken && this.props.report.reportProperties) {
                this.props.fetchToken();
            }
        }
    }

    componentDidMount() {
        this.props.loadReport();
        this.validateUrl();
    }

    handleSharedUrl() {
        const testCaseOrder = this.searchParams.get(TEST_CASE_PARAM_KEY),
            actionId = this.searchParams.get(ACTION_PARAM_KEY),
            msgId = this.searchParams.get(MESSAGE_PARAM_KEY);
        
        if (testCaseOrder != null) {
            this.selectTestCaseByOrder(Number.parseInt(testCaseOrder))
        }

        if (msgId !== null && !isNaN(Number(msgId))) {
            this.props.selectMessage(Number(msgId))
            this.props.selectMessagesScrollHintIds(Number(msgId));
        }

        if (actionId !== null && !isNaN(Number(actionId))) {
            this.props.selectAction(Number(actionId));
            this.props.selectActionsScrollHintIds(Number(actionId));
        }
    }

    /**
     * This function replaces url file path to index.html when we go to the new report from the old
     */
    validateUrl() {
        const href = window.location.href,
            filePath = href.slice(href.lastIndexOf('/'));

        if (!filePath.includes(REPORT_FILE_PATH)) {
            window.history.pushState({}, "", href.replace(filePath, '/' + REPORT_FILE_PATH));
        }
    }

    selectTestCaseByOrder(testCaseOrder: number) {
        if (!this.props.report) {
            console.error("Trying to handle shared url before report load.");
            return;
        }

        const testCaseMetadata = this.props.report.metadata.find(metadata => metadata.order === testCaseOrder);
        
        if (testCaseMetadata) {
            this.props.loadTestCase(testCaseMetadata);
        } else {
            console.warn("Can't handle shared url: Test Case with this id not found");
        }
    }

    selectTestCaseById(testCaseId: string) {
        if (!this.props.report) {
            console.error("Trying to handle shared url before report load.");
            return;
        }

        const testCaseMetadata = this.props.report.metadata.find(metadata => metadata.id === testCaseId);
        
        if (testCaseMetadata) {
            this.props.loadTestCase(testCaseMetadata);
        } else {
            console.warn("Can't handle shared url: Test Case with this id not found");
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

        //FIXME: I guess we need a dedicated action for this
        selectMessage: (messageId: number) => dispatch(selectVerification(messageId)),
        selectActionsScrollHintIds: (actionId: Number) => dispatch(selectActionsScrollHintIds(actionId)),
        selectMessagesScrollHintIds: (actionId: Number) => dispatch(selectMessagesScrollHintIds(actionId)),
        fetchToken: () => dispatch(fetchToken()),
        setSubmittedMlData: (data: SubmittedData[]) => dispatch(setSubmittedMlData(data)),
        loadReport: () => dispatch(initReport()),
        loadTestCase: (metadata: TestCaseMetadata) => dispatch(loadTestCase(metadata.jsonpFileName))
    })
)(AppBase);

export default App;
